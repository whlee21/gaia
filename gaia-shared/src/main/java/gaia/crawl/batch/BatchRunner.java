package gaia.crawl.batch;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gaia.crawl.CrawlProcessor;
import gaia.crawl.CrawlState;
import gaia.crawl.CrawlStatus;
import gaia.crawl.CrawlerController;
import gaia.crawl.UpdateController;
import gaia.crawl.io.Content;

public class BatchRunner implements Runnable {
	private static final Logger LOG = LoggerFactory.getLogger(BatchRunner.class);
	CrawlerController cc;
	BatchStatus batch;
	CrawlProcessor processor;
	UpdateController output;
	CrawlState state;
	CrawlStatus status;
	boolean parse;
	boolean index;
	BatchManager bm;
	boolean stopRequested = false;
	boolean commitOnFinish = false;

	public BatchRunner(CrawlerController cc, BatchStatus batch, CrawlProcessor processor, boolean parse, boolean index,
			CrawlState state) {
		this.cc = cc;
		this.batch = batch;
		this.processor = processor;
		this.output = processor.getUpdateController();
		this.state = state;
		this.status = state.getStatus();
		this.parse = parse;
		this.index = index;
		this.bm = cc.getBatchManager();
		this.commitOnFinish = state.getDataSource().getBoolean("commit_on_finish");
	}

	public synchronized void stop() {
		stopRequested = true;
		status.setState(CrawlStatus.JobState.STOPPING);
	}

	public void run() {
		BatchContentReader cfr = null;
		BatchSolrReader sfr = null;
		boolean failed = false;
		status.running();
		long started = System.currentTimeMillis();
		try {
			if ((parse) || ((index) && (!batch.parsed))) {
				status.setMessage("Preparing for parsing...");
				cfr = bm.openContentReader(batch);
				if (cfr == null) {
					if (batch.parsed)
						LOG.warn("Can't open raw content file, but batch is already parsed - re-parsing skipped.");
					else
						throw new Exception("Unable to parse - no content file!");
				} else {
					BatchTeeUpdateController bc = new BatchTeeUpdateController(bm, batch, null, true);
					processor.setUpdateController(bc);
					processor.start();
					Content c = new Content();
					batch.parsed = false;
					batch.parsedDocs = 0L;
					while (cfr.read(c)) {
						if (stopRequested) {
							return;
						}
						processor.process(c);
						batch.parsedDocs += 1L;
						status.setMessage("Parsed " + batch.parsedDocs);
					}
					processor.finish();

					if (bc.isStarted()) {
						bc.finish(true);
					}
					batch.parsed = true;
					status.setMessage("Finished parsing.");
				}
			}
			if (index) {
				status.setMessage("Preparing for indexing...");
				sfr = bm.openSolrReader(batch);
				output.start();
				sfr.process(output, state, true);
				status.setMessage("Finished indexing.");
			}
		} catch (Throwable t) {
			t.printStackTrace();
			status.failed(t);
			failed = true;
		} finally {
			if ((!failed) && (!stopRequested)) {
				status.setState(CrawlStatus.JobState.FINISHING);
				if (output.isStarted())
					try {
						output.finish(commitOnFinish);
					} catch (IOException e) {
						LOG.warn("Problem finishing the output", e);
					}
			}
			try {
				if (cfr != null)
					cfr.close();
			} catch (IOException e1) {
			}
			try {
				if (sfr != null)
					sfr.close();
			} catch (IOException e1) {
			}
			try {
				bm.saveBatchStatus(batch);
			} catch (Exception e) {
			}
			if (processor.getUpdateController() != output) {
				try {
					processor.setUpdateController(output);
				} catch (Exception e) {
					LOG.warn("Problem restoring UpdateController: " + e.toString());
				}
			}
			if (!failed) {
				batch.startTime = started;
				batch.finishTime = System.currentTimeMillis();
				try {
					bm.saveBatchStatus(batch);
				} catch (Exception e) {
				}
				if (stopRequested)
					status.end(CrawlStatus.JobState.STOPPED);
				else {
					status.end(CrawlStatus.JobState.FINISHED);
				}
			}
			LOG.info("Batch " + batch.batchId + " " + status);
		}
	}
}
