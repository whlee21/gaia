package gaia.crawl.gaialogs;

import gaia.crawl.CrawlStatus;

public final class LogCrawlerCounter {
	private final CrawlStatus status;

	public LogCrawlerCounter(CrawlStatus status) {
		this.status = status;
	}

	public void incrementCounter(CrawlStatus.Counter c) {
		status.incrementCounter(c);
	}

	public String toString() {
		return status.toString();
	}
}
