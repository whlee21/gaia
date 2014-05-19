package gaia.hello.server.controller;

import gaia.hello.server.configuration.Configuration;
import gaia.hello.server.servlet.GaiaHelloGuiceServletConfig;

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

import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.servlet.GuiceFilter;
import com.sun.jersey.spi.container.servlet.ServletContainer;

public class GaiaHelloServer {
	private static Logger LOG = LoggerFactory.getLogger(GaiaHelloServer.class);
	private Server server = null;

	final String CONTEXT_PATH = "/";

	@Inject
	Configuration configs;

	@Inject
	Injector injector;

	private static GaiaHelloController gaiaController = null;

	public static GaiaHelloController getController() {
		return gaiaController;
	}

	public void run() throws Exception {
		server = new Server();

		try {
			ClassPathXmlApplicationContext parentSpringAppContext = new ClassPathXmlApplicationContext();
			parentSpringAppContext.refresh();
			ConfigurableListableBeanFactory factory = parentSpringAppContext
					.getBeanFactory();
			factory.registerSingleton("guiceInjector", injector);

			String[] contextLocations = {};
			ClassPathXmlApplicationContext springAppContext = new ClassPathXmlApplicationContext(
					contextLocations, parentSpringAppContext);

			ServletContextHandler root = new ServletContextHandler(server,
					CONTEXT_PATH, ServletContextHandler.SECURITY
							| ServletContextHandler.SESSIONS);

			root.addEventListener(new GaiaHelloGuiceServletConfig());

			root.getSessionHandler().getSessionManager()
					.setSessionCookie("GAIAHELLOSESSIONID");

			GenericWebApplicationContext springWebAppContext = new GenericWebApplicationContext();
			springWebAppContext.setServletContext(root.getServletContext());
			springWebAppContext.setParent(springAppContext);

			/* Configure web app context */
			root.setResourceBase(configs.getWebAppDir());

			root.getServletContext()
					.setAttribute(
							WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE,
							springWebAppContext);
			
			root.addFilter(GuiceFilter.class, "/*", null);

			ServletHolder rootServlet = root.addServlet(DefaultServlet.class,
					"/");
			rootServlet.setInitOrder(1);



			// Spring Security Filter initialization
//			if (configs.getApiAuthentication()) {
//			DelegatingFilterProxy springSecurityFilter = new DelegatingFilterProxy();
//			springSecurityFilter.setTargetBeanName("springSecurityFilterChain");
//				root.addFilter(new FilterHolder(springSecurityFilter),
//						"/api/*", 1);
//			}

//			ServletHolder sh = new ServletHolder(ServletContainer.class);
//			sh.setInitParameter(
//					"com.sun.jersey.config.property.resourceConfigClass",
//					"com.sun.jersey.api.core.PackagesResourceConfig");
//			sh.setInitParameter("com.sun.jersey.config.property.packages",
//					"gaia.hello.server.api.services;" + "gaia.hello.server.api");
//			sh.setInitParameter("com.sun.jersey.api.json.POJOMappingFeature",
//					"true");
//			root.addServlet(sh, "/api/v1/*");
//			sh.setInitOrder(2);

			server.setThreadPool(new QueuedThreadPool(25));

			SelectChannelConnector apiConnector = new SelectChannelConnector();
			apiConnector.setPort(configs.getClientApiPort());
			apiConnector.setMaxIdleTime(configs.getConnectionMaxIdleTime());

			server.addConnector(apiConnector);

			server.setStopAtShutdown(true);
			springAppContext.start();

			GaiaHelloController controller = injector
					.getInstance(GaiaHelloController.class);

			gaiaController = controller;

			server.start();
			server.join();
			LOG.info("Joined the Server");
		} catch (BindException bindException) {
			LOG.error(
					"Could not bind to server port - instance may already be running. "
							+ "Terminating this instance.", bindException);
			throw bindException;
		}
	}

	public void stop() throws Exception {
		try {
			server.stop();
		} catch (Exception e) {
			LOG.error("Error stopping the server", e);
		}
	}

	public static void main(String[] args) throws Exception {
		GaiaHelloModule gaiaModule = new GaiaHelloModule();
		Injector injector = Guice.createInjector(gaiaModule);
		GaiaHelloServer server = null;
		try {
			LOG.info("Getting the controller");
			server = injector.getInstance(GaiaHelloServer.class);
			if (server != null) {
				server.run();
			}
		} catch (Throwable t) {
			LOG.error("Failed to run the Gaia Search Server", t);
			if (server != null) {
				server.stop();
			}
			System.exit(-1);
		}
	}
}