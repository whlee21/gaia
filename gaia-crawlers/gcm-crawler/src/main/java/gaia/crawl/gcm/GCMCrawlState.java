package gaia.crawl.gcm;

import gaia.crawl.CrawlProcessor;
import gaia.crawl.CrawlState;
import gaia.crawl.CrawlStatus;
import gaia.crawl.HistoryRecorder;
import gaia.crawl.datasource.DataSource;

public class GCMCrawlState extends CrawlState implements CrawlStatus.FinishListener {
	boolean commitOnFinish = false;

	public synchronized void init(DataSource ds, CrawlProcessor processor, HistoryRecorder historyRecorder)
			throws Exception {
		this.commitOnFinish = ds.getBoolean("commit_on_finish", true);
		super.init(ds, processor, historyRecorder);
		getStatus().addFinishListener(this);
	}

	public void finished() {
		try {
			getProcessor().getUpdateController().finish(this.commitOnFinish);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
