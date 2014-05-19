package gaia.crawl.http.robots;

import java.util.ArrayList;
import java.util.List;

public abstract class BaseRobotRules {
	private long _crawlDelay = 2000L;
	private boolean _deferVisits = false;
	private List<String> _sitemaps;

	public abstract boolean isAllowed(String paramString);

	public abstract boolean isAllowAll();

	public abstract boolean isAllowNone();

	public BaseRobotRules() {
		_sitemaps = new ArrayList<String>();
	}

	public long getCrawlDelay() {
		return _crawlDelay;
	}

	public void setCrawlDelay(long crawlDelay) {
		_crawlDelay = crawlDelay;
	}

	public boolean isDeferVisits() {
		return _deferVisits;
	}

	public void setDeferVisits(boolean deferVisits) {
		_deferVisits = deferVisits;
	}

	public void addSitemap(String sitemap) {
		_sitemaps.add(sitemap);
	}

	public List<String> getSitemaps() {
		return _sitemaps;
	}
}
