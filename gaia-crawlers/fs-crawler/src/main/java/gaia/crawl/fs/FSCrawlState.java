package gaia.crawl.fs;

import gaia.crawl.CrawlState;
import gaia.crawl.CrawlerController;

public class FSCrawlState extends CrawlState {
	private FSCrawler crawler;
	private CrawlerController cc;

	public FSCrawlState(CrawlerController cc) {
		this.cc = cc;
	}

	public void crawl() {
		try {
			crawler = new FSCrawler(ds, processor, this);
			crawler.crawl();
		} catch (Throwable e) {
			e.printStackTrace();
			status.failed(e);
		}
	}

	public CrawlerController getCrawlerController() {
		return cc;
	}

	public void stop() throws Exception {
		if ((crawler != null) && (crawler.isRunning()))
			crawler.stop();
	}

	public void abort() throws Exception {
		if ((crawler != null) && (crawler.isRunning()))
			crawler.abort();
	}
}
