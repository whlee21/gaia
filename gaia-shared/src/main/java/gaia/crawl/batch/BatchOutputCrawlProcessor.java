package gaia.crawl.batch;

import gaia.crawl.CrawlProcessor;
import gaia.crawl.UpdateController;
import gaia.crawl.io.Content;
import org.apache.solr.common.SolrInputDocument;

public class BatchOutputCrawlProcessor extends CrawlProcessor {
	BatchManager mgr;
	BatchContentWriter writer;
	BatchStatus batch;

	public BatchOutputCrawlProcessor(BatchManager mgr, BatchStatus batch) throws Exception {
		super(UpdateController.NULL_UPDATE_CONTROLLER);
		this.mgr = mgr;
		writer = mgr.createContentWriter(batch, false);
		this.batch = batch;
		batch.numDocs = 0L;
	}

	public void process(Content content) throws Exception {
		writer.write(content);
		batch.numDocs += 1L;
	}

	public void processParsedDocument(SolrInputDocument doc) throws Exception {
		throw new UnsupportedOperationException("processParsedDocument");
	}

	public void delete(String key) throws Exception {
		throw new UnsupportedOperationException("delete");
	}

	public void finish() throws Exception {
		writer.close();
		mgr.saveBatchStatus(batch);
	}

	public void start() {
	}
}
