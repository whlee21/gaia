package gaia.crawl.batch;

import gaia.crawl.UpdateController;
import gaia.crawl.datasource.DataSource;
import java.io.IOException;
import org.apache.solr.common.SolrInputDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BatchTeeUpdateController extends UpdateController {
	private static final Logger LOG = LoggerFactory.getLogger(BatchTeeUpdateController.class);
	BatchManager mgr;
	BatchStatus batch;
	BatchStatus oldBatch;
	BatchSolrWriter writer;
	UpdateController output;
	boolean overwrite = false;
	boolean tee = false;
	int parsedCount = 0;

	public BatchTeeUpdateController(BatchManager mgr, BatchStatus batch, UpdateController output, boolean overwrite)
			throws Exception {
		this.mgr = mgr;
		this.output = output;
		this.overwrite = overwrite;
		if (output != null) {
			tee = true;
		}
		setBatch(batch, overwrite);
	}

	public void init(DataSource ds) throws Exception {
		if (output != null)
			output.init(ds);
	}

	public void start() throws Exception {
		if (batch == null) {
			if (oldBatch == null) {
				throw new Exception("can't reuse - missing previous batch parameters");
			}
			BatchStatus b = BatchManager.newBatch(mgr.getCrawlerControllerType(), oldBatch.collection,
					oldBatch.dsId);
			b.descr = oldBatch.descr;
			batch = b;
		}
		writer = mgr.createSolrWriter(batch, overwrite);
		super.start();
	}

	private void ensureStarted() throws IOException {
		if (!isStarted())
			throw new IOException("batch update controller not started");
	}

	public void setBatch(BatchStatus batch, boolean overwrite) throws Exception {
		if (isStarted()) {
			finish(false);
		}
		this.batch = batch;
		this.overwrite = overwrite;
	}

	public void setOutput(UpdateController output) {
		this.output = output;
		if (output != null)
			tee = true;
	}

	public void add(SolrInputDocument doc) throws IOException {
		ensureStarted();
		writer.writeAdd(doc);
		parsedCount += 1;
		if (tee)
			try {
				output.add(doc);
			} catch (IOException ioe) {
				LOG.warn("Caught exception, disabling tee output", ioe);
				tee = false;
			}
	}

	public void delete(String id) throws IOException {
		ensureStarted();
		writer.writeDelete(id);
		if (tee)
			try {
				output.delete(id);
			} catch (IOException ioe) {
				LOG.warn("Caught exception, disabling tee output", ioe);
				tee = false;
			}
	}

	public void deleteByQuery(String query) throws IOException {
		ensureStarted();
		writer.writeDeleteByQuery(query);
		if (tee)
			try {
				output.deleteByQuery(query);
			} catch (IOException ioe) {
				LOG.warn("Caught exception, disabling tee output", ioe);
				tee = false;
			}
	}

	public void commit() throws IOException {
		ensureStarted();
		writer.writeCommit();
		if (tee)
			try {
				output.commit();
			} catch (IOException ioe) {
				LOG.warn("Caught exception, disabling tee output", ioe);
				tee = false;
			}
	}

	public void finish(boolean commit) throws IOException {
		ensureStarted();
		batch.parsed = true;
		batch.parsedDocs = parsedCount;
		try {
			mgr.saveBatchStatus(batch);
		} catch (Exception e) {
			LOG.warn("Failed saving batch status", e);
		}
		oldBatch = batch;
		batch = null;
		super.finish(commit);
		writer.close();
		if (tee)
			try {
				output.finish(commit);
			} catch (IOException ioe) {
				LOG.warn("Caught exception, disabling tee output", ioe);
				tee = false;
			}
	}
}
