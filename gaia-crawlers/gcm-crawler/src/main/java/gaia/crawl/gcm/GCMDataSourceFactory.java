package gaia.crawl.gcm;

import gaia.crawl.CrawlerController;
import gaia.crawl.DataSourceFactory;

import java.util.ServiceLoader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GCMDataSourceFactory extends DataSourceFactory {
	private static final Logger LOG = LoggerFactory.getLogger(GCMDataSourceFactory.class);

	public GCMDataSourceFactory(CrawlerController cc) {
		super(cc);
		try {
			ServiceLoader<GCMExtension> gcmExtensionLoader = ServiceLoader.load(GCMExtension.class, GCMExtension.class.getClassLoader());

			LOG.info("Loading GCM extensions...");

			for (GCMExtension extension : gcmExtensionLoader) {
				try {
					LOG.info("Registering extension class:" + extension.getClass().getName());

					extension.register(this.types);
				} catch (Throwable t) {
					LOG.error("Register failed:" + t.getClass().getName(), t);
				}
			}

			LOG.info("Done loading GCM extensions.");
		} catch (Throwable t) {
			LOG.error("Could not load extensions.", t);
			return;
		}
	}
}
