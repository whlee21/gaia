package gaia.crawl.connector;

import gaia.crawl.connector.configuration.Configuration;

import java.net.BindException;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.nio.SelectChannelConnector;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.GenericWebApplicationContext;
import org.xnap.commons.i18n.I18n;
import org.xnap.commons.i18n.I18nFactory;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.servlet.GuiceFilter;

public class GaiaConnectorServer {
	private static I18n i18n = I18nFactory.getI18n(GaiaConnectorServer.class);
	private static Logger LOG = LoggerFactory.getLogger(GaiaConnectorServer.class);
	private Server server = null;

	final String CONTEXT_PATH = "/";

	// @Inject
	// Configuration configs;

	Injector injector;

	public void run() throws Exception {
		server = new Server();
		try {

			ClassPathXmlApplicationContext springAppContext = new ClassPathXmlApplicationContext();
			springAppContext.refresh();
			ConfigurableListableBeanFactory factory = springAppContext.getBeanFactory();
			factory.registerSingleton("guiceInjector", injector);

			ServletContextHandler root = new ServletContextHandler(server, CONTEXT_PATH, ServletContextHandler.NO_SECURITY
					| ServletContextHandler.NO_SESSIONS);

			GenericWebApplicationContext springWebAppContext = new GenericWebApplicationContext();
			springWebAppContext.setServletContext(root.getServletContext());
			springWebAppContext.setParent(springAppContext);

			// root.setResourceBase(configs.getConfig(Configuration.WEBAPP_DIR_KEY));
			// root.setResourceBase("/home/whlee21/workspace/gaia/gaia-connector/target");

			root.getServletContext().setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE,
					springWebAppContext);

			root.addFilter(GuiceFilter.class, "/*", null);

			ServletHolder rootServlet = root.addServlet(DefaultServlet.class, "/");
			rootServlet.setInitOrder(1);

			server.setThreadPool(new QueuedThreadPool(25));

			SelectChannelConnector apiConnector = new SelectChannelConnector();
			apiConnector.setPort(8881);
			apiConnector.setMaxIdleTime(9000000);

			server.addConnector(apiConnector);

			server.setStopAtShutdown(true);
			springAppContext.start();

			server.start();
			server.join();
			LOG.info(i18n.tr("Joined the Server"));
		} catch (BindException bindException) {
			LOG.error(i18n.tr("Could not bind to server port - instance may already be running. "
					+ "Terminating this instance."), bindException);
			throw bindException;
		}
	}

	public void stop() throws Exception {
		try {
			server.stop();
		} catch (Exception e) {
			LOG.error(i18n.tr("Error stopping the server"), e);
		}
	}

	public static void main(String[] args) throws Exception {
		Configuration configuration = new Configuration();

		// SolrResourceLoader loader = new
		// SolrResourceLoader(Constants.GAIA_CONF_HOME + "/solr");
		// GaiaCoreContainer cores = new GaiaCoreContainer(loader,
		// ConfigSolr.fromFile(loader, new File(
		// Constants.GAIA_CONF_HOME + "/solr/solr.xml")));
		GaiaConnectorModule module = new GaiaConnectorModule(configuration);
		Injector injector = Guice.createInjector(new Module[] { module, new GaiaConnectorServletModule() });
		module.init(injector);
		GaiaConnectorServer server = null;
		try {
			LOG.info(i18n.tr("Getting the controller"));
			server = injector.getInstance(GaiaConnectorServer.class);
			if (server != null) {
				server.run();
			}
		} catch (Throwable t) {
			LOG.error(i18n.tr("Failed to run the Gaia Connector Server"), t);
			if (server != null) {
				server.stop();
			}
			System.exit(-1);
		}
	}
}