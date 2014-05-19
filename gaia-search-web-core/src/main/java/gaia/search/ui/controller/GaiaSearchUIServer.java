package gaia.search.ui.controller;

import gaia.search.ui.GaiaSearchUIServerException;
import gaia.search.ui.configuration.Configuration;
import gaia.search.ui.orm.GuiceJpaInitializer;
import gaia.search.ui.orm.PersistenceType;
import gaia.search.ui.security.authorization.GaiaLdapAuthenticationProvider;
import gaia.search.ui.security.authorization.GaiaLocalUserDetailsService;
import gaia.search.ui.security.authorization.Users;

import java.net.BindException;

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

public class GaiaSearchUIServer {
	private static I18n i18n = I18nFactory.getI18n(GaiaSearchUIServer.class);
	private static Logger LOG = LoggerFactory.getLogger(GaiaSearchUIServer.class);
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
	public GaiaSearchUIServer(Configuration configs, Injector injector) {
		this.configs = configs;
		this.injector = injector;
	}

	public void run() throws Exception {
		performStaticInjection();
		addLocalUsers();
		
		server = new Server();

		try {
			ClassPathXmlApplicationContext parentSpringAppContext = new ClassPathXmlApplicationContext();
			parentSpringAppContext.refresh();
			ConfigurableListableBeanFactory factory = parentSpringAppContext.getBeanFactory();
			factory.registerSingleton("guiceInjector", injector);
			factory.registerSingleton("passwordEncoder", injector.getInstance(PasswordEncoder.class));
			factory.registerSingleton("gaiaLocalUserService", injector.getInstance(GaiaLocalUserDetailsService.class));
			factory.registerSingleton("gaiaLdapAuthenticationProvider",
					injector.getInstance(GaiaLdapAuthenticationProvider.class));

			String[] contextLocations = { SPRING_CONTEXT_LOCATION };
			ClassPathXmlApplicationContext springAppContext = new ClassPathXmlApplicationContext(contextLocations,
					parentSpringAppContext);
			
			ServletContextHandler root = new ServletContextHandler(server, CONTEXT_PATH, ServletContextHandler.SECURITY
					| ServletContextHandler.SESSIONS);
			
			root.addEventListener(new GaiaSearchUIGuiceServletConfig(injector));
			
			((HashSessionManager) root.getSessionHandler().getSessionManager()).setSessionCookie("GAIASEARCHUISESSIONID");

			GenericWebApplicationContext springWebAppContext = new GenericWebApplicationContext();
			springWebAppContext.setServletContext(root.getServletContext());
			springWebAppContext.setParent(springAppContext);

			/* Configure web app context */
			root.setResourceBase(configs.getWebAppDir());

			root.getServletContext().setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE,
					springWebAppContext);
			
			//for dispatching requests to injectable filters and servlets, make JerseyServletModule works.
			root.addFilter(GuiceFilter.class, "/*", null);
	    
			ServletHolder rootServlet = root.addServlet(DefaultServlet.class, "/");
			rootServlet.setInitOrder(1);
			
			GaiaProxyServlet.init(this.injector);
			ServletHolder proxyServlet = root.addServlet(GaiaProxyServlet.class, configs.getApiCorePath());
			proxyServlet.setInitOrder(2);
			
			
			server.setThreadPool(new QueuedThreadPool(25));

			SelectChannelConnector apiConnector = new SelectChannelConnector();
			apiConnector.setPort(configs.getLocalServerPort());
			String connMaxIdleTime = configs.getConfig(Configuration.SERVER_CONNECTION_MAX_IDLE_TIME);
			apiConnector.setMaxIdleTime(Integer.parseInt(connMaxIdleTime));

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

	public void performStaticInjection() {

	}
	
	@Transactional
	protected void addLocalUsers() {
		if (configs.getPersistenceType() == PersistenceType.LOCAL) {
			LOG.info(i18n.tr("Local database is used - creating default users"));
			Users users = injector.getInstance(Users.class);

			users.createDefaultRoles();
			users.createUser("admin", "admin");
			users.createUser("user", "user");
			try {
				users.promoteToAdmin(users.getLocalUser("admin"));
			} catch (GaiaSearchUIServerException e) {
				throw new RuntimeException(e);
			}
		}
	}
	
	@Transactional
	protected void addInMemoryUsers() {
		if (configs.getPersistenceType() == PersistenceType.IN_MEMORY) {
			LOG.info(i18n.tr("In-memory database is used - creating default users"));
			Users users = injector.getInstance(Users.class);

			users.createDefaultRoles();
			users.createUser("admin", "admin");
			users.createUser("user", "user");
			try {
				users.promoteToAdmin(users.getLocalUser("admin"));
			} catch (GaiaSearchUIServerException e) {
				throw new RuntimeException(e);
			}
		}
	}

	public static void main(String[] args) throws Exception {
		Configuration configuration = new Configuration();

		GaiaSearchUIModule module = new GaiaSearchUIModule(configuration);
		Injector injector = Guice.createInjector(new Module[] { module });
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
		GaiaSearchUIServer server = null;
		try {
			LOG.info(i18n.tr("Getting the controller"));
			injector.getInstance(GuiceJpaInitializer.class);
			server = injector.getInstance(GaiaSearchUIServer.class);
			if (server != null) {
				server.run();
			}
		} catch (Throwable t) {
			LOG.error(i18n.tr("Failed to run the Gaia Proxy Server"), t);
			if (server != null) {
				server.stop();
			}
			System.exit(-1);
		}
	}
}