package gaia.crawl.gaialogs;

import gaia.Constants;
import gaia.crawl.CrawlProcessor;
import gaia.crawl.CrawlState;
import gaia.crawl.HistoryRecorder;
import gaia.crawl.datasource.DataSource;
import java.io.File;

public class LogCrawlState extends CrawlState {
	private LogCrawlerRunner crawler;
	private String nodeName;

	public LogCrawlState(String nodeName) {
		this.nodeName = nodeName;
	}

	public synchronized void init(DataSource ds, CrawlProcessor processor, HistoryRecorder historyRecorder)
			throws Exception {
		super.init(ds, processor, historyRecorder);

		crawler = new LogCrawlerRunner(nodeName, getStatus(), new File(Constants.GAIA_LOGS_HOME),
				processor.getUpdateController());
	}

	public LogCrawlerRunner getCrawler() {
		return crawler;
	}
}
