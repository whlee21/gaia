package gaia.crawl.aperture;

import gaia.crawl.CrawlState;
import gaia.crawl.datasource.DataSource;
import gaia.crawl.http.robots.RobotsCache;

import java.util.Set;

import org.semanticdesktop.aperture.accessor.DataAccessorFactory;
import org.semanticdesktop.aperture.accessor.impl.DefaultDataAccessorRegistry;

public class GaiaDataAccessorRegistry extends DefaultDataAccessorRegistry {
	private RobotsCache robots;
	private DataSource ds;

	public GaiaDataAccessorRegistry(RobotsCache robots, DataSource ds, CrawlState crawlState) {
		this.robots = robots;
		this.ds = ds;

		Set<DataAccessorFactory> impls = get("http");
		for (DataAccessorFactory daf : impls) {
			remove(daf);
		}
		impls = get("https");
		for (DataAccessorFactory daf : impls) {
			remove(daf);
		}

		add(new GaiaHttpAccessorFactory(robots, ds, crawlState));
	}
}
