package gaia.crawl.batch;

import java.io.Closeable;
import java.io.IOException;

import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.update.AddUpdateCommand;
import org.apache.solr.update.CommitUpdateCommand;
import org.apache.solr.update.DeleteUpdateCommand;
import org.apache.solr.update.UpdateCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gaia.crawl.CrawlState;
import gaia.crawl.CrawlStatus;
import gaia.crawl.UpdateController;

public abstract class BatchSolrReader implements Closeable {
	protected static final Logger LOG = LoggerFactory.getLogger(BatchSolrReader.class);

	public abstract UpdateCommand next() throws IOException;

	public void process(UpdateController update, CrawlState state, boolean commitAtEnd) throws IOException {
		UpdateCommand cmd = null;
		CrawlStatus status = state != null ? state.getStatus() : null;
		while (((cmd = next()) != null) && ((status == null) || (status.getState() == CrawlStatus.JobState.RUNNING))) {
			if ((cmd instanceof AddUpdateCommand)) {
				SolrInputDocument doc = ((AddUpdateCommand) cmd).solrDoc;
				LOG.debug("Add: " + doc);
				update.add(doc);
				if (status != null)
					status.incrementCounter(CrawlStatus.Counter.New);
			} else if ((cmd instanceof DeleteUpdateCommand)) {
				DeleteUpdateCommand del = (DeleteUpdateCommand) cmd;
				if (del.id != null) {
					LOG.debug("Del: id=" + del.id);
					update.delete(del.id);
				} else {
					LOG.debug("Del: query=" + del.query);
					update.deleteByQuery(del.query);
				}
				if (status != null)
					status.incrementCounter(CrawlStatus.Counter.Deleted);
			} else if ((cmd instanceof CommitUpdateCommand)) {
				LOG.debug("Commit.");
				update.commit();
			}
		}
		if ((status != null) && (status.getState() == CrawlStatus.JobState.ABORTING)) {
			commitAtEnd = false;
		}
		update.finish(commitAtEnd);
	}
}
