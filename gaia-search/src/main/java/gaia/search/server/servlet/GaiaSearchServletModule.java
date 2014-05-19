package gaia.search.server.servlet;

import gaia.Constants;
import gaia.search.server.configuration.Configuration;

import java.util.HashMap;
import java.util.Map;

import org.apache.solr.servlet.RedirectServlet;
import org.apache.solr.servlet.ZookeeperInfoServlet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.filter.DelegatingFilterProxy;

import com.google.inject.Singleton;
import com.sun.jersey.guice.JerseyServletModule;
import com.sun.jersey.guice.spi.container.servlet.GuiceContainer;

public class GaiaSearchServletModule extends JerseyServletModule {

	private static final Logger LOG = LoggerFactory.getLogger(GaiaSearchServletModule.class);
	private static final String JERSEY_CONFIG_PROPERTY_RESOURCECONFIGCLASS = "com.sun.jersey.config.property.resourceConfigClass";
	private static final String JERSEY_API_JSON_POJO_MAPPING_FEATURE = "com.sun.jersey.api.json.POJOMappingFeature";
	private static final String JERSEY_CONFIG_PROPERTY_PACKAGES = "com.sun.jersey.config.property.packages";
	
	private Configuration configs;
	
	public GaiaSearchServletModule(Configuration configs) {
		this.configs = configs;
	}

	@Override
	protected void configureServlets() {
		
		// Must configure at least one JAX-RS resource or the
		// server will fail to start.

		bind(RedirectServlet.class).in(Singleton.class);
		bind(ZookeeperInfoServlet.class).in(Singleton.class);
		bind(JettyRelativeDirListingServlet.class).in(Singleton.class);
		bind(GaiaSolrAdminUiServlet.class).in(Singleton.class);
//		bind(ResourceInstanceFactory.class).to(ResourceInstanceFactoryImpl.class);
		bind(GuiceContainer.class);

		serve("/solr/zookeeper", new String[0]).with(ZookeeperInfoServlet.class);
		serve("/solr/", new String[0]).with(GaiaSolrAdminUiServlet.class);

		Map<String, String> params = new HashMap<String, String>();
		params.put("destination", "/solr/#/");
		serve("/solr/admin", new String[0]).with(RedirectServlet.class, params);
		serve("/solr/admin/", new String[0]).with(RedirectServlet.class, params);

		params = new HashMap<String, String>();
		params.put("destination", "/solr/zookeeper");
		serve("/zookeeper.jsp", new String[0]).with(RedirectServlet.class, params);

		params = new HashMap<String, String>();
		params.put("destination", "/solr/#/~logging");
		serve("/logging", new String[0]).with(RedirectServlet.class, params);

		params = new HashMap<String, String>();

		String logBase = "/logs";

		params.put("stripPath", logBase);
		params.put("aliases", "false");
		params.put("acceptRanges", "true");
		params.put("dirAllowed", "true");
		params.put("maxCacheSize", "0");
		params.put("resourceBase", Constants.GAIA_LOGS_HOME);

		serve("/logs*", new String[0]).with(JettyRelativeDirListingServlet.class, params);

		// serve("/api/*", new String[0]).with(LWEServerServlet.class);

		params = new HashMap<String, String>();
		params.put("path-prefix", "/solr");

		filter("/solr/*", new String[0]).through(GaiaSolrDispatchFilter.class, params);
		
		// filter("/api/*", new String[0]).through(SSLAuthorizationFilter.class);
		// filter("/solr/*", new String[0]).through(SSLAuthorizationFilter.class);

		// if (Constants.IS_CLOUDY)
		// serve("/control/*", new String[0]).with(LWEControlServlet.class);

		// bind(JacksonJsonProvider.class).in(Scopes.SINGLETON);

		params = new HashMap<String, String>();
		params.put(JERSEY_CONFIG_PROPERTY_RESOURCECONFIGCLASS, "com.sun.jersey.api.core.PackagesResourceConfig");
		params.put(JERSEY_CONFIG_PROPERTY_PACKAGES, "gaia.search.server.api.services;" + "gaia.search.server.api");
		params.put(JERSEY_API_JSON_POJO_MAPPING_FEATURE, "true");
		// Route all requests through GuiceContainer
		serve("/api/v1/*").with(GuiceContainer.class, params);
		
		params = new HashMap<String, String>();
		params.put(JERSEY_CONFIG_PROPERTY_RESOURCECONFIGCLASS, "com.sun.jersey.api.core.PackagesResourceConfig");
		params.put(JERSEY_CONFIG_PROPERTY_PACKAGES, "gaia.search.server.api.services;" + "gaia.search.server.api");
		params.put(JERSEY_API_JSON_POJO_MAPPING_FEATURE, "true");
		serve("/resources/*").with(GuiceContainer.class, params);
		
		// serve("/*").with(GuiceContainer.class);
	}

//	static {
//		java.util.logging.Logger rootLogger = LogManager.getLogManager().getLogger("");
//		Handler[] handlers = rootLogger.getHandlers();
//		for (int i = 0; i < handlers.length; i++) {
//			rootLogger.removeHandler(handlers[i]);
//		}
//		SLF4JBridgeHandler.install();
//	}
}
