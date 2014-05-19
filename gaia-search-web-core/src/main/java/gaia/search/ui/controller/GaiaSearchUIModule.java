package gaia.search.ui.controller;

import gaia.api.JsonSerializer;
import gaia.api.ObjectSerializer;
import gaia.search.ui.api.services.parsers.JsonRequestBodyParser;
import gaia.search.ui.api.services.parsers.RequestBodyParser;
import gaia.search.ui.configuration.Configuration;
import gaia.search.ui.orm.PersistenceType;

import java.util.HashMap;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.crypto.password.StandardPasswordEncoder;
import org.springframework.web.filter.DelegatingFilterProxy;

import com.google.gson.Gson;
import com.google.inject.Injector;
import com.google.inject.Scopes;
import com.google.inject.persist.jpa.JpaPersistModule;
import com.sun.jersey.guice.JerseyServletModule;
import com.sun.jersey.guice.spi.container.servlet.GuiceContainer;

public class GaiaSearchUIModule extends JerseyServletModule {
	private static final Logger LOG = LoggerFactory.getLogger(GaiaSearchUIModule.class);
	private static final String JERSEY_CONFIG_PROPERTY_RESOURCECONFIGCLASS = "com.sun.jersey.config.property.resourceConfigClass";
	private static final String JERSEY_API_JSON_POJO_MAPPING_FEATURE = "com.sun.jersey.api.json.POJOMappingFeature";
	private static final String JERSEY_CONFIG_PROPERTY_PACKAGES = "com.sun.jersey.config.property.packages";
	
	private final Configuration configs;
	
	public GaiaSearchUIModule(Configuration configs) {
		this.configs = configs;
	}

	public void init(Injector injector) {
		configs.setInjector(injector);
//		GaiaSearchLicenseStartupManager manager = injector.getInstance(GaiaSearchLicenseStartupManager.class);
//		manager.startup();
	}

	@Override
	protected void configureServlets() {
		bind(Configuration.class).toInstance(configs);
		bind(GaiaProxyServlet.class);
		bind(Gson.class).in(Scopes.SINGLETON);
		bind(PasswordEncoder.class).toInstance(new StandardPasswordEncoder());
		
		bind(ObjectSerializer.class).toInstance(new JsonSerializer());
		bind(RequestBodyParser.class).toInstance(new JsonRequestBodyParser());
		
		install(buildJpaPersistModule());
		
		if (configs.getApiAuthentication()) {
			DelegatingFilterProxy springSecurityFilter = new DelegatingFilterProxy();
			springSecurityFilter.setTargetBeanName("springSecurityFilterChain");
			filter(configs.getApiCorePath(), new String[0]).through(springSecurityFilter);
			filter(configs.getApiAdminPath(), new String[0]).through(springSecurityFilter);
		}
		
		bind(GuiceContainer.class);
		
		HashMap<String, String> params = new HashMap<String, String>();
		params.put(JERSEY_CONFIG_PROPERTY_RESOURCECONFIGCLASS, "com.sun.jersey.api.core.PackagesResourceConfig");
		params.put(JERSEY_CONFIG_PROPERTY_PACKAGES, "gaia.search.ui.api");
		params.put(JERSEY_API_JSON_POJO_MAPPING_FEATURE, "true");
		// Route all requests through GuiceContainer
		serve(configs.getApiAdminPath()).with(GuiceContainer.class, params);
		
		params = new HashMap<String, String>();
		params.put(JERSEY_CONFIG_PROPERTY_RESOURCECONFIGCLASS, "com.sun.jersey.api.core.PackagesResourceConfig");
		params.put(JERSEY_CONFIG_PROPERTY_PACKAGES, "gaia.search.ui.api");
		params.put(JERSEY_API_JSON_POJO_MAPPING_FEATURE, "true");
		serve("/resources/*").with(GuiceContainer.class, params);
		
	}
	
	private JpaPersistModule buildJpaPersistModule() {
		PersistenceType persistenceType = configs.getPersistenceType();
		JpaPersistModule jpaPersistModule = new JpaPersistModule(Configuration.JDBC_UNIT_NAME);

		Properties properties = new Properties();

		switch (persistenceType) {
		case IN_MEMORY:
			properties.put("javax.persistence.jdbc.url", Configuration.JDBC_IN_MEMORY_URL);
			properties.put("javax.persistence.jdbc.driver", Configuration.JDBC_IN_MEMROY_DRIVER);
			properties.put("eclipselink.ddl-generation", "drop-and-create-tables");
			properties.put("eclipselink.orm.throw.exceptions", "true");
			jpaPersistModule.properties(properties);
			return jpaPersistModule;
		case REMOTE:
			properties.put("javax.persistence.jdbc.url", configs.getDatabaseUrl());
			properties.put("javax.persistence.jdbc.driver", configs.getDatabaseDriver());
			break;
		case LOCAL:
			LOG.debug("whlee21 buildJpaPersistModule LOCAL");
			properties.put("javax.persistence.jdbc.url", configs.getLocalDatabaseUrl());
			properties.put("javax.persistence.jdbc.driver", configs.getLocalDatabaseDriver());
			break;
		}

		properties.setProperty("javax.persistence.jdbc.user", configs.getDatabaseUser());
		properties.setProperty("javax.persistence.jdbc.password", configs.getDatabasePassword());

		switch (configs.getJPATableGenerationStrategy()) {
		case CREATE:
			properties.setProperty("eclipselink.ddl-generation", "create-tables");
			break;
		case DROP_AND_CREATE:
			properties.setProperty("eclipselink.ddl-generation", "drop-and-create-tables");
			break;
		case NONE:
		}
		properties.setProperty("eclipselink.ddl-generation.output-mode", "both");
		properties.setProperty("eclipselink.create-ddl-jdbc-file-name", "DDL-create.jdbc");
		properties.setProperty("eclipselink.drop-ddl-jdbc-file-name", "DDL-drop.jdbc");

		jpaPersistModule.properties(properties);

		return jpaPersistModule;
	}
	
	static {

//		if (Constants.GAIA_CONF_HOME == null) {
//			throw new IllegalStateException("conf.dir must be set in gaia-search.properties");
//		}
//
//		if (Constants.GAIA_LOGS_HOME == null) {
//			throw new IllegalStateException("logs.dir must be set in gaia-search.properties");
//		}

	}
}