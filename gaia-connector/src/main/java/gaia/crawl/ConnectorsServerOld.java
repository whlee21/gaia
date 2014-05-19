package gaia.crawl;

import gaia.Constants;
import gaia.api.LWEStatusService;
import gaia.api.ResourceFinder;
import gaia.crawl.api.ConnectorManagerServerResource;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.LogManager;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.xml.XmlConfiguration;
import org.restlet.Application;
import org.restlet.Context;
import org.restlet.Restlet;
import org.restlet.ext.servlet.ServerServlet;
import org.restlet.routing.Router;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Module;

public class ConnectorsServerOld {
	private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(ConnectorsServerOld.class);
	public static final int DEFAULT_PORT = 8765;
	Injector injector;
	Server jetty;

	public ConnectorsServerOld() throws Exception {
		ConnectorsModule module = new ConnectorsModule();
		injector = Guice.createInjector(new Module[] { module });
		module.init(injector);

		File jettyXml = new File(Constants.GAIA_CONF_HOME + File.separator + "jetty/connectors/etc/jetty.xml");
		if ((!jettyXml.exists()) || (!jettyXml.isFile()) || (!jettyXml.canRead())) {
			LOG.error("Could not read jetty xml configuration file: " + jettyXml.getAbsolutePath());
			System.exit(1);
		}
		XmlConfiguration configuration = new XmlConfiguration(jettyXml.toURI().toURL());
		jetty = ((Server) configuration.configure());

		ServletContextHandler restletContext = new ServletContextHandler(1);
		restletContext.setContextPath("/connectors/v1");
		ServerServlet serverServlet = new ConnectorServerServlet(injector);
		ServletHolder servletHolder = new ServletHolder(serverServlet);

		servletHolder.setInitParameter("org.restlet.clients", "HTTP HTTPS");
		restletContext.addServlet(servletHolder, "/*");

		ContextHandlerCollection contexts = new ContextHandlerCollection();
		contexts.setHandlers(new org.eclipse.jetty.server.Handler[] { restletContext });

		List handlers = new ArrayList();

		handlers.addAll(Arrays.asList(jetty.getHandlers()));

		handlers.add(contexts);

		HandlerCollection handlerCollection = new HandlerCollection();
		handlerCollection.setHandlers((org.eclipse.jetty.server.Handler[]) handlers
				.toArray(new org.eclipse.jetty.server.Handler[0]));

		jetty.setHandler(handlerCollection);
	}

	public void start() throws Exception {
		int stopPort = Integer.parseInt(System.getProperty("STOP.PORT", "-1"));
		String stopKey = System.getProperty("STOP.KEY");
		if (stopPort != -1) {
			new Monitor(stopPort, stopKey);
		}
		jetty.start();
		jetty.join();
	}

	public void stop() throws Exception {
		jetty.stop();
	}

	public static void main(String[] args) throws Exception {
		new ConnectorsServerOld().start();
	}

	static {
		java.util.logging.Logger rootLogger = LogManager.getLogManager().getLogger("");
		java.util.logging.Handler[] handlers = rootLogger.getHandlers();
		for (int i = 0; i < handlers.length; i++) {
			rootLogger.removeHandler(handlers[i]);
		}
		SLF4JBridgeHandler.install();
	}

	public static class ConnectorMgrApplication extends Application {
		ResourceFinder factory;

		@Inject
		public ConnectorMgrApplication(Injector injector) {
			factory = new ResourceFinder(injector);
			setStatusService(new LWEStatusService());
		}

		public synchronized Restlet createInboundRoot() {
			Router router = new Router(getContext());
			router.attach("/mgr", factory.finderOf(ConnectorManagerServerResource.class));
			return router;
		}
	}

	public static class ConnectorServerServlet extends ServerServlet {
		private Injector injector;
		private Application application;

		public ConnectorServerServlet(Injector injector) {
			application = ((Application) injector.getInstance(ConnectorMgrApplication.class));
		}

		protected Application createApplication(Context ctx) {
			return application;
		}
	}
}