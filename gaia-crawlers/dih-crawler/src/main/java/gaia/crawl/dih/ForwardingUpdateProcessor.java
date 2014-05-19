package gaia.crawl.dih;

import gaia.crawl.CrawlProcessor;
import gaia.crawl.CrawlStatus;
import gaia.crawl.CrawlerUtils;
import gaia.crawl.UpdateController;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.update.AddUpdateCommand;
import org.apache.solr.update.CommitUpdateCommand;
import org.apache.solr.update.DeleteUpdateCommand;
import org.apache.solr.update.RollbackUpdateCommand;
import org.apache.solr.update.processor.UpdateRequestProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class ForwardingUpdateProcessor extends UpdateRequestProcessor {
	private static final Logger LOG = LoggerFactory.getLogger(ForwardingUpdateProcessor.class);
	DIHCrawlState state;

	public ForwardingUpdateProcessor(DIHCrawlState state, UpdateRequestProcessor next) {
		super(next);
		this.state = state;
	}

	public void processAdd(AddUpdateCommand cmd) throws IOException {
		if (state == null) {
			return;
		}
		SolrInputDocument doc = cmd.getSolrInputDocument();
		try {
			Map<String, Collection<Object>> docMap = new HashMap<String, Collection<Object>>();
			Collection<String> fieldNames = doc.getFieldNames();
			for (String fieldName : fieldNames) {
				Collection<Object> values = doc.getFieldValues(fieldName);
				docMap.put(fieldName, values);
			}
			state.getProcessor().processParsedDocumentMap(docMap);
		} catch (Exception e) {
			LOG.warn(
					CrawlerUtils.msgDocFailed(state.getDataSource().getCollection(), state.getDataSource(), "", e.getMessage()),
					e);

			state.getStatus().incrementCounter(CrawlStatus.Counter.Failed);
		}
	}

	public void processDelete(DeleteUpdateCommand cmd) throws IOException {
		if (state == null) {
			return;
		}
		String id = cmd.getId();
		String query = cmd.getQuery();
		if (query != null) {
			CrawlProcessor processor = state.getProcessor();
			UpdateController updateController = processor.getUpdateController();
			updateController.deleteByQuery(query);
		} else if (id != null) {
			try {
				state.getProcessor().delete(id);
			} catch (Exception e) {
				LOG.warn(
						CrawlerUtils.msgDocFailed(state.getDataSource().getCollection(), state.getDataSource(), id, e.getMessage()),
						e);

				state.getStatus().incrementCounter(CrawlStatus.Counter.Failed);
			}
		}
	}

	public void processCommit(CommitUpdateCommand cmd) throws IOException {
		if (state == null) {
			return;
		}
		state.getProcessor().getUpdateController().commit();
	}

	public void processRollback(RollbackUpdateCommand cmd) throws IOException {
	}
}
