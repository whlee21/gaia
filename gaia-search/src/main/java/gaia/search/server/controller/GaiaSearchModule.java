package gaia.search.server.controller;

import gaia.Constants;
import gaia.Defaults;
import gaia.admin.collection.CollectionManager;
import gaia.admin.collection.YamlCollectionManager;
import gaia.api.JsonSerializer;
import gaia.api.ObjectSerializer;
import gaia.crawl.ConnectorManager;
import gaia.crawl.DataSourceManager;
import gaia.crawl.DefaultHistoryRecorder;
import gaia.crawl.HistoryRecorder;
import gaia.crawl.LocalConnectorManager;
import gaia.crawl.NoopConnectorManager;
import gaia.crawl.RestConnectorManager;
import gaia.crawl.UpdateController;
import gaia.crawl.YamlDataSourceManager;
import gaia.jmx.DefaultJmxManager;
import gaia.jmx.JmxManager;
import gaia.scheduler.DefaultSolrScheduler;
import gaia.scheduler.SolrScheduler;
import gaia.search.server.api.services.parsers.JsonRequestBodyParser;
import gaia.search.server.api.services.parsers.RequestBodyParser;
import gaia.search.server.configuration.Configuration;
import gaia.search.server.orm.PersistenceType;
import gaia.search.server.servlet.GaiaCoreContainer;

import java.io.IOException;
import java.util.Properties;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.solr.core.CoreContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.crypto.password.StandardPasswordEncoder;
import org.xml.sax.SAXException;

import com.google.gson.Gson;
import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.name.Names;
import com.google.inject.persist.jpa.JpaPersistModule;

public class GaiaSearchModule extends AbstractModule {
	private static final Logger LOG = LoggerFactory.getLogger(GaiaSearchModule.class);

	private final Configuration configs;

	private final GaiaCoreContainer cores;

	// private String solrAddress = ;

	public GaiaSearchModule(GaiaCoreContainer cores, Configuration configs) {
		this.cores = cores;
		this.configs = configs;
	}

	public void init(Injector injector) {
		Defaults.INSTANCE = (Defaults) injector.getInstance(Defaults.class);
		Defaults.injector = injector;
		Defaults.INSTANCE.setReadOnly(true);
		configs.setInjector(injector);
		// GaiaSearchLicenseStartupManager manager =
		// injector.getInstance(GaiaSearchLicenseStartupManager.class);
		// manager.startup();
	}

	@Override
	protected void configure() {
		bind(Configuration.class).toInstance(configs);
		bind(Gson.class).in(Scopes.SINGLETON);
		bind(PasswordEncoder.class).toInstance(new StandardPasswordEncoder());
		bind(ObjectSerializer.class).toInstance(new JsonSerializer());
		bind(RequestBodyParser.class).toInstance(new JsonRequestBodyParser());

		install(buildJpaPersistModule());

		// bind(String.class).annotatedWith(Names.named("provider.module.class")).toInstance(
		// "gaia.search.server.controller.internal.DefaultProviderModule");

		bind(String.class).annotatedWith(Names.named("role-mappings-filename")).toInstance("role-mappings.yml");
		bind(String.class).annotatedWith(Names.named("collections-filename")).toInstance("collections.yml");
		bind(String.class).annotatedWith(Names.named("datasources-filename")).toInstance("core-datasources.yml");
		bind(String.class).annotatedWith(Names.named("settings-filename")).toInstance("core-settings.yml");
		bind(String.class).annotatedWith(Names.named("users-filename")).toInstance("users.yml");

		requestStaticInjection(UpdateController.class);
		String solrAddress;
		try {
			solrAddress = configs.getSolrAddress(true, null).toString();
		} catch (IOException e) {
			solrAddress = configs.getClientApiUrl().toString() + "/solr";
		}

		LOG.debug("whlee21 solrAddress = " + solrAddress);
		bind(String.class).annotatedWith(Names.named("solr-address")).toInstance(solrAddress);

		bind(CollectionManager.class).to(YamlCollectionManager.class);
		bind(HistoryRecorder.class).to(DefaultHistoryRecorder.class);
		bind(DataSourceManager.class).to(YamlDataSourceManager.class);
		bind(SolrScheduler.class).to(DefaultSolrScheduler.class);
		
		String connectorAddress = configs.getConnectorUrl();
		if (connectorAddress == null || org.apache.commons.lang.StringUtils.isBlank(connectorAddress)) {
			LOG.info("TEST MODE: using LocalConnectorManager");
			bind(ConnectorManager.class).to(LocalConnectorManager.class);
		} else if (!org.apache.commons.lang.StringUtils.isBlank(connectorAddress)) {
			LOG.info("Using RestConnectorManager with url '" + connectorAddress + "'");
			bind(ConnectorManager.class).to(RestConnectorManager.class);
		} else {
			bind(ConnectorManager.class).to(NoopConnectorManager.class);
		}

		bind(JmxManager.class).to(DefaultJmxManager.class);

		bind(String.class).annotatedWith(Names.named("dshistory-filename")).toInstance("datasource-history.yml");
		bind(String.class).annotatedWith(Names.named("cmdhistory-filename")).toInstance("solr-cmd-history.yml");
		bind(String.class).annotatedWith(Names.named("defaults-filename")).toInstance("defaults.yml");
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

	@Provides
	public GaiaCoreContainer providesGaiaCoreContainer() throws ParserConfigurationException, IOException, SAXException {
		return cores;
	}

	@Provides
	public CoreContainer providesCoreContainer() throws ParserConfigurationException, IOException, SAXException {
		return cores;
	}

	static {
		if (Constants.GAIA_DATA_HOME == null) {
			throw new IllegalStateException("data.dir must be set in gaia-search.properties");
		}

		if (Constants.GAIA_APP_HOME == null) {
			throw new IllegalStateException("app.dir must be set in gaia-search.properties");
		}

		if (Constants.GAIA_CONF_HOME == null) {
			throw new IllegalStateException("conf.dir must be set in gaia-search.properties");
		}

		if (Constants.GAIA_LOGS_HOME == null) {
			throw new IllegalStateException("logs.dir must be set in gaia-search.properties");
		}

		System.setProperty("zkServerDataDir", Constants.STORAGE_PATH);
	}
}