package gaia.crawl.aperture;

import gaia.crawl.CrawlState;
import gaia.crawl.datasource.DataSource;
import gaia.crawl.http.protocol.HttpProtocolConfig;
import gaia.crawl.http.robots.RobotsCache;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.semanticdesktop.aperture.accessor.DataAccessor;
import org.semanticdesktop.aperture.accessor.DataAccessorFactory;

public class GaiaHttpAccessorFactory implements DataAccessorFactory {
	private static Set<String> set = new HashSet<String>();
	private static final Set<String> SUPPORTED_SCHEMES = Collections.unmodifiableSet(set);
	private GaiaHttpAccessor accessor;
	private RobotsCache robots;
	private DataSource ds;
	private CrawlState crawlState;

	public GaiaHttpAccessorFactory(RobotsCache robots, DataSource ds, CrawlState crawlState) {
		this.robots = robots;
		this.ds = ds;
		this.crawlState = crawlState;
	}

	public Set<String> getSupportedSchemes() {
		return SUPPORTED_SCHEMES;
	}

	public DataAccessor get() {
		if (this.accessor == null) {
			this.accessor = new GaiaHttpAccessor(this.ds, this.crawlState, this.robots, new HttpProtocolConfig(this.ds));
		}
		return this.accessor;
	}

	static {
		set.add("http");
		set.add("https");
	}
}
