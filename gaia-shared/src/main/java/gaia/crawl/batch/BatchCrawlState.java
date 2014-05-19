package gaia.crawl.batch;

import gaia.crawl.CrawlProcessor;
import gaia.crawl.CrawlState;
import gaia.crawl.CrawlStatus;
import gaia.crawl.HistoryRecorder;
import gaia.crawl.datasource.DataSource;

public class BatchCrawlState extends CrawlState {
	private BatchRunner runner;

	public synchronized void init(DataSource ds, CrawlProcessor processor, final HistoryRecorder historyRecorder)
			throws Exception {
		assert (ds != null);
		this.ds = ds;

		description = ("Batch Job " + id + " (template datasource " + ds.getDataSourceId() + ": " + ds.getDisplayName() + ")");

		status = new CrawlStatus(id, ds.getDataSourceId());
		status.setBatchJob(true);
		final CrawlStatus fStatus = status;
		status.addFinishListener(new CrawlStatus.FinishListener() {
			public void finished() {
				if (historyRecorder != null)
					historyRecorder.record(fStatus);
			}
		});
		this.processor = processor;
		if (processor != null)
			processor.init(this);
	}

	public void close() {
		stop();
	}

	public void setRunner(BatchRunner runner) {
		this.runner = runner;
	}

	public BatchRunner getRunner() {
		return runner;
	}

	public void stop() {
		if (runner != null)
			runner.stop();
	}
}
