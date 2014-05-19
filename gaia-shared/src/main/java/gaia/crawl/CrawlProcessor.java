package gaia.crawl;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;

import org.apache.solr.common.SolrInputDocument;

import gaia.crawl.batch.BatchOutputCrawlProcessor;
import gaia.crawl.batch.BatchStatus;
import gaia.crawl.datasource.DataSource;
import gaia.crawl.io.Content;

public abstract class CrawlProcessor {
	protected String collectionName;
	protected UpdateController updateController;
	protected CrawlState state;

	public CrawlProcessor(UpdateController updateController) {
		this.updateController = updateController;
	}

	public void init(CrawlState state) throws Exception {
		collectionName = state.ds.getCollection();
		this.state = state;
		if (updateController != null)
			updateController.init(state.ds);
	}

	public abstract void process(Content paramContent) throws Exception;

	public abstract void processParsedDocument(SolrInputDocument paramSolrInputDocument) throws Exception;

	public void processParsedDocumentMap(Map<String, Collection<Object>> map) throws Exception {
		SolrInputDocument doc = new SolrInputDocument();
		for (Map.Entry<String, Collection<Object>> entry : map.entrySet()) {
			for (Object obj : entry.getValue()) {
				doc.addField((String) entry.getKey(), obj);
			}
		}
		processParsedDocument(doc);
	}

	public abstract void delete(String paramString) throws Exception;

	public abstract void finish() throws Exception;

	public abstract void start() throws Exception;

	public UpdateController getUpdateController() {
		return updateController;
	}

	public void setUpdateController(UpdateController updateController) throws Exception {
		this.updateController = updateController;
	}

	public static CrawlProcessor create(CrawlerController cc, DataSource ds) throws Exception {
		CrawlProcessor processor = null;

		boolean parsing = ds.getBoolean("parsing", true);
		parsing = (parsing) || (cc.getBatchManager() == null);
		if (!parsing) {
			String batchId = new UUID(0L, System.nanoTime()).toString();
			BatchStatus batch = new BatchStatus(ds.getCrawlerType(), ds.getCollection(), ds.getDataSourceId().toString(),
					batchId);

			processor = new BatchOutputCrawlProcessor(cc.getBatchManager(), batch);
		} else {
			UpdateController updateController = UpdateController.create(cc, ds);
			processor = new TikaCrawlProcessor(updateController);
		}
		return processor;
	}

	public String getBatchId() {
		return null;
	}
}
