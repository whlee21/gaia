package gaia.servlet;

import gaia.Constants;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Handler;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import org.apache.solr.servlet.RedirectServlet;
import org.apache.solr.servlet.ZookeeperInfoServlet;
import org.restlet.ext.servlet.ServerServlet;
import org.slf4j.bridge.SLF4JBridgeHandler;

import com.google.inject.Singleton;
import com.google.inject.servlet.ServletModule;

//public class LWEServletModule extends ServletModule {
//	protected void configureServlets() {
//		bind(RedirectServlet.class).in(Singleton.class);
//		bind(ZookeeperInfoServlet.class).in(Singleton.class);
//		bind(JettyRelativeDirListingServlet.class).in(Singleton.class);
//		bind(LWELoadAdminUiServlet.class).in(Singleton.class);
//		bind(ServerServlet.class).in(Singleton.class);
//
//		serve("/solr/zookeeper", new String[0]).with(ZookeeperInfoServlet.class);
//		serve("/solr/", new String[0]).with(LWELoadAdminUiServlet.class);
//
//		HashMap params = new HashMap();
//		params.put("destination", "/solr/#/");
//		serve("/solr/admin", new String[0]).with(RedirectServlet.class, params);
//		serve("/solr/admin/", new String[0]).with(RedirectServlet.class, params);
//
//		params = new HashMap();
//		params.put("destination", "/solr/zookeeper");
//		serve("/zookeeper.jsp", new String[0]).with(RedirectServlet.class, params);
//
//		params = new HashMap();
//		params.put("destination", "/solr/#/~logging");
//		serve("/logging", new String[0]).with(RedirectServlet.class, params);
//
//		Map logParams = new HashMap();
//
//		String logBase = "/logs";
//
//		logParams.put("stripPath", "/logs");
//		logParams.put("aliases", "false");
//		logParams.put("acceptRanges", "true");
//		logParams.put("dirAllowed", "true");
//		logParams.put("maxCacheSize", "0");
//		logParams.put("resourceBase", Constants.GAIA_LOGS_HOME);
//
//		serve("/logs*", new String[0]).with(JettyRelativeDirListingServlet.class, logParams);
//
//		serve("/api/*", new String[0]).with(LWEServerServlet.class);
//
//		params = new HashMap();
//		params.put("path-prefix", "/solr");
//
//		filter("/solr/*", new String[0]).through(LweSolrDispatchFilter.class, params);
//		filter("/api/*", new String[0]).through(SSLAuthorizationFilter.class);
//		filter("/solr/*", new String[0]).through(SSLAuthorizationFilter.class);
//
//		params = new HashMap();
//		params.put("org.restlet.application", "org.apache.solr.rest.SolrRestApi");
//
//		serve("/schema", new String[0]).with(ServerServlet.class, params);
//		serve("/schema/*", new String[0]).with(ServerServlet.class, params);
//
//		if (Constants.IS_CLOUDY)
//			serve("/control/*", new String[0]).with(LWEControlServlet.class);
//	}
//
//	static {
//		Logger rootLogger = LogManager.getLogManager().getLogger("");
//		Handler[] handlers = rootLogger.getHandlers();
//		for (int i = 0; i < handlers.length; i++) {
//			rootLogger.removeHandler(handlers[i]);
//		}
//		SLF4JBridgeHandler.install();
//	}
//}

public class LWEServletModule {
	
}