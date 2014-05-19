package gaia.search.server.controller;

import gaia.Constants;
import gaia.admin.collection.AdminScheduler;
import gaia.search.server.configuration.Configuration;
import gaia.search.server.orm.GuiceJpaInitializer;
import gaia.search.server.servlet.GaiaCoreContainer;
import gaia.search.server.servlet.GaiaSearchGuiceServletConfig;
import gaia.search.server.servlet.GaiaSearchServletModule;

import java.io.File;
import java.net.BindException;

import net.nicholaswilliams.java.licensing.License;
import net.nicholaswilliams.java.licensing.LicenseManager;
import net.nicholaswilliams.java.licensing.exception.ExpiredLicenseException;
import net.nicholaswilliams.java.licensing.exception.InvalidLicenseException;

import org.apache.solr.core.ConfigSolr;
import org.apache.solr.core.SolrResourceLoader;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.nio.SelectChannelConnector;
import org.eclipse.jetty.server.session.HashSessionManager;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.GenericWebApplicationContext;
import org.xnap.commons.i18n.I18n;
import org.xnap.commons.i18n.I18nFactory;

import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.persist.Transactional;
import com.google.inject.servlet.GuiceFilter;

public class GaiaSearchServer {
	private static I18n i18n = I18nFactory.getI18n(GaiaSearchServer.class);
	private static Logger LOG = LoggerFactory.getLogger(GaiaSearchServer.class);
	private Server server = null;

	final String CONTEXT_PATH = "/";
	final String SPRING_CONTEXT_LOCATION = "classpath:/webapp/WEB-INF/spring-security.xml";

	// @Inject
	Configuration configs;

	// @Inject
	Injector injector;

	// private static GaiaManagementController managementController = null;

	// public static GaiaManagementController getController() {
	// return managementController;
	// }

	@Inject
	public GaiaSearchServer(Configuration configs, Injector injector) {
		this.configs = configs;
		this.injector = injector;
	}

	public void run() throws Exception {
		performStaticInjection();
		// addLocalUsers();
		server = new Server();

		try {
			ServletContextHandler root = new ServletContextHandler(server, CONTEXT_PATH, ServletContextHandler.SECURITY
					| ServletContextHandler.SESSIONS);

			root.addEventListener(new GaiaSearchGuiceServletConfig(injector));

			((HashSessionManager) root.getSessionHandler().getSessionManager()).setSessionCookie("GAIASEARCHSESSIONID");

			/* Configure web app context */
			root.setResourceBase(configs.getConfig(Configuration.WEBAPP_DIR_KEY));
			
			// certMan.initRootCert();

			root.addFilter(GuiceFilter.class, "/*", null);

			ServletHolder rootServlet = root.addServlet(DefaultServlet.class, "/");
			rootServlet.setInitOrder(1);

			server.setThreadPool(new QueuedThreadPool(25));

			SelectChannelConnector apiConnector = new SelectChannelConnector();
			apiConnector.setPort(configs.getClientApiPort());
			String connMaxIdleTime = configs.getConfig(Configuration.SERVER_CONNECTION_MAX_IDLE_TIME);
			apiConnector.setMaxIdleTime(Integer.parseInt(connMaxIdleTime));

			server.addConnector(apiConnector);

			server.setStopAtShutdown(true);

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

	public void performStaticInjection() {

	}

	public static void main(String[] args) throws Exception {
		Configuration configuration = new Configuration();

		SolrResourceLoader loader = new SolrResourceLoader(Constants.GAIA_CONF_HOME + "/solr");
		GaiaCoreContainer cores = new GaiaCoreContainer(loader, ConfigSolr.fromFile(loader, new File(
				Constants.GAIA_CONF_HOME + "/solr/solr.xml")));
		GaiaSearchModule module = new GaiaSearchModule(cores, configuration);
		Injector injector = Guice.createInjector(new Module[] { module, new GaiaSearchServletModule(configuration) });
		module.init(injector);
//		LicenseManager lm = LicenseManager.getInstance();
//		License license = lm.getLicense(configuration.getClientName());
//		if (license == null) {
//			LOG.error(i18n.tr("License not found."));
//			throw new InvalidLicenseException();
//		}
//		try {
//			lm.validateLicense(license);
//		} catch (ExpiredLicenseException e) {
//			LOG.error(i18n.tr("Expired license."));
//			return;
//		} catch (InvalidLicenseException e) {
//			LOG.error(i18n.tr("Invalid license."));
//			return;
//		}
		cores.load();
		((AdminScheduler) injector.getInstance(AdminScheduler.class)).startAllSchedules();
		GaiaSearchServer server = null;
		try {
			LOG.info(i18n.tr("Getting the controller"));
			injector.getInstance(GuiceJpaInitializer.class);
			server = injector.getInstance(GaiaSearchServer.class);
			if (server != null) {
				server.run();
			}
		} catch (Throwable t) {
			LOG.error(i18n.tr("Failed to run the Gaia Search Server"), t);
			if (server != null) {
				server.stop();
			}
			System.exit(-1);
		}
	}
}