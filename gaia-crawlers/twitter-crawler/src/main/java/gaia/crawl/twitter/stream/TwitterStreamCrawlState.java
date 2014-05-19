package gaia.crawl.twitter.stream;

import gaia.crawl.CrawlState;

public class TwitterStreamCrawlState extends CrawlState {
	TwitterStreamCrawler crawler = null;
	Thread t = null;

	public synchronized void start() throws Exception {
		if ((t != null) && (t.isAlive())) {
			throw new Exception("already running");
		}
		crawler = new TwitterStreamCrawler(this);
		t = new Thread(crawler);

		t.start();
	}

	public synchronized void stop() throws Exception {
		if ((t == null) || (!t.isAlive())) {
			throw new Exception("not running");
		}
		crawler.stop();
	}
}
