package gaia.server.controller;

import gaia.server.servlet.HelloGuiceServletConfig;

import java.net.BindException;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.nio.SelectChannelConnector;
import org.eclipse.jetty.server.session.HashSessionManager;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.ServletContextHandler;
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

public class HelloServer {
	private static final Logger LOG = LoggerFactory
			.getLogger(HelloServer.class);

	private Server server = null;

	final String CONTEXT_PATH = "/";

	@Inject
	Injector injector;

	public void run() throws Exception {
		// Create the server.
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

			// Create a servlet context and add the jersey servlet.
			// ServletContextHandler sch = new ServletContextHandler(server,
			// "/");

			// Add our Guice listener that includes our bindings
			root.addEventListener(new HelloGuiceServletConfig());
			
			((HashSessionManager)root.getSessionHandler().getSessionManager())
					.setSessionCookie("GAIAHELLOSESSIONID");

			GenericWebApplicationContext springWebAppContext = new GenericWebApplicationContext();
			springWebAppContext.setServletContext(root.getServletContext());
			springWebAppContext.setParent(springAppContext);

			root.getServletContext()
					.setAttribute(
							WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE,
							springWebAppContext);

			// Then add GuiceFilter and configure the server to
			// reroute all requests through this filter.
			root.addFilter(GuiceFilter.class, "/*", null);

			// Must add DefaultServlet for embedded Jetty.
			// Failing to do this will cause 404 errors.
			// This is not needed if web.xml is used instead.
			root.addServlet(DefaultServlet.class, "/");

			
			server.setThreadPool(new QueuedThreadPool(25));

			SelectChannelConnector apiConnector = new SelectChannelConnector();
			apiConnector.setPort(8088);
			apiConnector.setMaxIdleTime(900000);

			server.addConnector(apiConnector);
			
			server.setStopAtShutdown(true);
			springAppContext.start();

			// Start the server
			server.start();
			server.join();
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
		Injector injector = Guice.createInjector(new HelloModule());
		HelloServer server = null;
		try {
			LOG.info("Getting the controller");
			server = injector.getInstance(HelloServer.class);
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
