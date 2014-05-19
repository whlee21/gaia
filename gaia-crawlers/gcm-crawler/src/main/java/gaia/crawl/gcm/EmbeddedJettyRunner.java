package gaia.crawl.gcm;

import gaia.Constants;
import gaia.crawl.gcm.api.LocalJettyGCMServer;
import gaia.crawl.gcm.api.RemoteGCMServer;
import gaia.crawl.gcm.feeder.GaiaFeedConsumer;
import java.io.File;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.LocalConnector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.bio.SocketConnector;
import org.eclipse.jetty.server.handler.ContextHandler.Context;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.webapp.WebAppContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EmbeddedJettyRunner {
	private Logger logger = LoggerFactory.getLogger(EmbeddedJettyRunner.class);
	private Server server;
	private LocalConnector connector;
	private SocketConnector remoteConnector;

	public EmbeddedJettyRunner(GCMController controller) {
		server = new Server();
		ServletContextHandler context = new ServletContextHandler();
		context.setContextPath("/");
		server.setHandler(context);

		ServletContextHandler servletContext = new ServletContextHandler();
		servletContext.setContextPath("/");

		String gcmWebappLocation = Constants.GAIA_APP_HOME + File.separator + "webapps" + File.separator
				+ "connector-manager";

		logger.info("Starting gcm webapp in dir:" + gcmWebappLocation);

		WebAppContext webApp = new WebAppContext();

		webApp.setContextPath("/connector-manager");
		webApp.setResourceBase(gcmWebappLocation);
		ContextHandlerCollection contexts = new ContextHandlerCollection();
		contexts.setHandlers(new Handler[] { webApp, servletContext });
		server.setHandler(contexts);
		webApp.getServletContext().setAttribute("endpoint", new GaiaFeedConsumer(controller));
		connector = new LocalConnector();
		connector.setMaxIdleTime(Integer.MAX_VALUE);
		server.addConnector(connector);
		if (Constants.GCM_PORT != -1) {
			remoteConnector = new SocketConnector();
			remoteConnector.setPort(Constants.GCM_PORT);
			server.addConnector(remoteConnector);
		}
	}

	public void start() throws Exception {
		logger.info("Starting embedded container");
		server.start();
	}

	public void stop() throws Exception {
		logger.info("Shutting down connectors");
		for (Connector connector : server.getConnectors()) {
			connector.close();
		}
		logger.info("Shutting down jetty");
		server.stop();
		logger.info("Stopping embedded container");
	}

	public RemoteGCMServer getGCMServer() {
		return new LocalJettyGCMServer("http://0.0.0.0:0/connector-manager/", connector);
	}
}
