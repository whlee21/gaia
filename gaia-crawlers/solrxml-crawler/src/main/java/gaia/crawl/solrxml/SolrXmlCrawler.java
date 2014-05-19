package gaia.crawl.solrxml;

import gaia.crawl.CrawlProcessor;
import gaia.crawl.CrawlStatus;
import gaia.crawl.CrawlerController;
import gaia.crawl.CrawlerUtils;
import gaia.crawl.UpdateController;
import gaia.crawl.datasource.DataSource;
import gaia.crawl.datasource.DataSourceId;
import gaia.crawl.datasource.DataSourceUtils;
import gaia.crawl.datasource.FieldMappingUtil;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.common.util.ContentStreamBase;
import org.apache.solr.handler.loader.XMLLoader;
import org.apache.solr.request.LocalSolrQueryRequest;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.response.SolrQueryResponse;
import org.apache.solr.update.AddUpdateCommand;
import org.apache.solr.update.DeleteUpdateCommand;
import org.apache.solr.update.processor.UpdateRequestProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SolrXmlCrawler {
	private static transient Logger LOG = LoggerFactory.getLogger(SolrXmlCrawler.class);

	private static Map<DataSourceId, Boolean> interruptedJobs = new HashMap<DataSourceId, Boolean>();
	private DataSource ds;
	private CrawlStatus crawlStatus;
	private CrawlerController cc;
	private UpdateRequestProcessor processor;
	private CrawlProcessor cp;

	public static void interrupt(DataSource ds) {
		synchronized (interruptedJobs) {
			interruptedJobs.put(ds.getDataSourceId(), Boolean.valueOf(true));
		}
	}

	public static void clearInterruptedStatus(DataSource ds) {
		synchronized (interruptedJobs) {
			interruptedJobs.remove(ds.getDataSourceId());
		}
	}

	private static boolean isInterrupted(DataSourceId dsId) {
		synchronized (interruptedJobs) {
			Boolean interrupted = (Boolean) interruptedJobs.get(dsId);
			if (interrupted != null)
				return interrupted.booleanValue();
			return false;
		}
	}

	public SolrXmlCrawler(CrawlerController cc, DataSource ds, CrawlStatus crawlStatus, CrawlProcessor cp) {
		this.cc = cc;
		this.ds = ds;
		this.cp = cp;
		this.crawlStatus = crawlStatus;
	}

	public void crawl() {
		UpdateController gaiaUpdateController;
		if (cp != null)
			gaiaUpdateController = cp.getUpdateController();
		else {
			try {
				gaiaUpdateController = UpdateController.create(cc, ds);
			} catch (Exception e) {
				crawlStatus.failed(e);
				return;
			}
		}
		UUID uuid = UUID.randomUUID();
		processor = new SolrUpdateRequestProcessorWrapper(gaiaUpdateController, crawlStatus, ds, uuid.toString(), null);

		String filePath = DataSourceUtils.getPath(ds);
		try {
			gaiaUpdateController.start();
		} catch (Exception e) {
			crawlStatus.failed(e);
			return;
		}

		traverseDirectory(CrawlerUtils.resolveRelativePath(filePath));

		if (ds.getBoolean("include_datasource_metadata")) {
			String query = ds.getFieldMapping().getDatasourceField() + ":" + ds.getDataSourceId() + " AND NOT batch_id:"
					+ uuid.toString();
			try {
				gaiaUpdateController.deleteByQuery(query);
			} catch (IOException e) {
				LOG.warn(CrawlerUtils.msgDocFailed(ds.getCollection(), ds, null,
						"unable to delete obsolete documents from Solr index: " + e.getMessage()), e);
			}

		}

		boolean commit = ds.getBoolean("commit_on_finish", true);
		try {
			gaiaUpdateController.finish(commit);
		} catch (IOException e) {
			crawlStatus.failed(e);
		}

		if (crawlStatus.getState() == CrawlStatus.JobState.RUNNING)
			crawlStatus.end(CrawlStatus.JobState.FINISHED);
		else if (crawlStatus.getState() == CrawlStatus.JobState.STOPPING)
			crawlStatus.end(CrawlStatus.JobState.STOPPED);
	}

	private void traverseDirectory(File f) {
		if (f.isDirectory()) {
			File[] files = f.listFiles();
			for (File file : files) {
				if (isInterrupted(ds.getDataSourceId())) {
					break;
				}
				traverseDirectory(file);
			}
		} else {
			String filename = f.getAbsolutePath();
			for (String excludePattern : DataSourceUtils.getExcludePattern(ds)) {
				if (filename.matches(excludePattern)) {
					LOG.warn(CrawlerUtils.msgDocSkipped(ds.getCollection(), ds, f.toURI().toString(),
							"file matches exclude pattern '" + excludePattern + "'"));

					return;
				}
			}
			boolean toIndex = false;
			for (String includePattern : DataSourceUtils.getIncludePattern(ds)) {
				if (filename.matches(includePattern)) {
					toIndex = true;
					break;
				}
			}
			if (toIndex)
				indexFile(f);
			else
				LOG.warn(CrawlerUtils.msgDocSkipped(ds.getCollection(), ds, f.toURI().toString(),
						"skipping file as it doesn't match any include pattern " + DataSourceUtils.getIncludePattern(ds).toString()));
		}
	}

	private void indexFile(File f) {
		try {
			ContentStreamBase stream = new ContentStreamBase.FileStream(f);
			XMLLoader xmlLoader = new XMLLoader().init(null);

			ModifiableSolrParams params = new ModifiableSolrParams();
			SolrQueryRequest req = new LocalSolrQueryRequest(null, params);
			SolrQueryResponse rsp = new SolrQueryResponse();

			xmlLoader.load(req, rsp, stream, processor);

			if (LOG.isInfoEnabled())
				LOG.info("add document: " + f);
		} catch (Exception e) {
			LOG.warn(CrawlerUtils.msgDocFailed(ds.getCollection(), ds, f.toURI().toString(), e.getMessage()), e);

			crawlStatus.incrementCounter(CrawlStatus.Counter.Failed);
		}
	}

	private class SolrUpdateRequestProcessorWrapper extends UpdateRequestProcessor {
		private UpdateController updateController;
		private CrawlStatus crawlStatus;
		private DataSource ds;
		private String batch_id;
		private boolean isInterrupted = false;
		private int maxDocs;

		public SolrUpdateRequestProcessorWrapper(UpdateController updateController, CrawlStatus crawlStatus, DataSource ds,
				String batch_id, UpdateRequestProcessor next) {
			super(next);
			this.updateController = updateController;
			this.crawlStatus = crawlStatus;
			this.ds = ds;
			this.batch_id = batch_id;
			maxDocs = ds.getInt("max_docs");
			if (maxDocs < 0)
				maxDocs = Integer.MAX_VALUE;
		}

		public void processAdd(AddUpdateCommand cmd) throws IOException {
			if (reachedMaxDocs()) {
				return;
			}
			crawlStatus.incrementCounter(CrawlStatus.Counter.New);

			SolrInputDocument doc = cmd.getSolrInputDocument();

			if (ds.getBoolean("generate_unique_key")) {
				FieldMappingUtil.generateUniqueKey(doc, ds.getFieldMapping().getUniqueKey());
			}

			doc.addField("batch_id", batch_id);

			updateController.add(doc);
		}

		public void processDelete(DeleteUpdateCommand cmd) throws IOException {
			if (reachedMaxDocs()) {
				return;
			}
			crawlStatus.incrementCounter(CrawlStatus.Counter.Deleted);
			if (cmd.id != null)
				updateController.delete(cmd.id);
			else
				updateController.deleteByQuery(cmd.query);
		}

		private boolean reachedMaxDocs() {
			if (isInterrupted) {
				return true;
			}
			if (crawlStatus.getCounter(CrawlStatus.Counter.Total) >= maxDocs) {
				LOG.info("Stopping crawl because maxDocs=" + maxDocs + " limit was reached or exceeded.");
				isInterrupted = true;
				SolrXmlCrawler.interrupt(ds);
				return true;
			}
			return false;
		}
	}
}
