package gaia;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.solr.core.ConfigSolr;
import org.apache.solr.core.CoreContainer;
import org.apache.solr.core.SolrResourceLoader;
import org.restlet.resource.ServerResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Provides;
import com.google.inject.name.Names;
import gaia.admin.collection.CollectionManager;
import gaia.admin.collection.YamlCollectionManager;
import gaia.api.FieldResource;
import gaia.crawl.ConnectorManager;
import gaia.crawl.DataSourceManager;
import gaia.crawl.DefaultHistoryRecorder;
import gaia.crawl.HistoryRecorder;
import gaia.crawl.LocalConnectorManager;
import gaia.crawl.NoopConnectorManager;
import gaia.crawl.RestConnectorManager;
import gaia.crawl.YamlDataSourceManager;
import gaia.jmx.DefaultJmxManager;
import gaia.jmx.JmxManager;
import gaia.scheduler.DefaultSolrScheduler;
import gaia.scheduler.SolrScheduler;
import gaia.servlet.LweCoreContainer;
import gaia.utils.MasterConfUtil;

public class LWEModule extends AbstractModule {
	private static final Logger LOG = LoggerFactory.getLogger(LWEModule.class);
	private CoreContainer cores;

	protected void configure() {
		bind(String.class).annotatedWith(Names.named("role-mappings-filename")).toInstance("role-mappings.yml");
		bind(String.class).annotatedWith(Names.named("collections-filename")).toInstance("collections.yml");
		bind(String.class).annotatedWith(Names.named("datasources-filename")).toInstance("core-datasources.yml");
		bind(String.class).annotatedWith(Names.named("settings-filename")).toInstance("core-settings.yml");
		bind(String.class).annotatedWith(Names.named("users-filename")).toInstance("users.yml");

		bind(CollectionManager.class).to(YamlCollectionManager.class);

		if (gaia.utils.StringUtils.getBoolean(System.getProperty("local.connectors.tests")).booleanValue()) {
			LOG.info("TEST MODE: using LocalConnectorManager");
			bind(ConnectorManager.class).to(LocalConnectorManager.class);
		} else if (!org.apache.commons.lang.StringUtils.isBlank(System.getProperty("connectors.url"))) {
			LOG.info("Using RestConnectorManager with url '" + System.getProperty("connectors.url") + "'");

			bind(ConnectorManager.class).to(RestConnectorManager.class);
		} else {
			URL u = null;
			try {
				u = MasterConfUtil.getConnectorsAddress();
			} catch (IOException ioe) {
				LOG.warn("Error getting connectors.address from master.conf: " + ioe.toString());
			}
			if (u != null) {
				LOG.info("Using RestConnectorManager with url '" + u + "'");
				System.setProperty("connectors.url", u.toString());
				bind(ConnectorManager.class).to(RestConnectorManager.class);
			} else {
				LOG.warn("Neither connectors.url is defined nor connectors.address is specified in master.conf. Disabling connectors!");

				bind(ConnectorManager.class).to(NoopConnectorManager.class);
			}
		}
		bind(HistoryRecorder.class).to(DefaultHistoryRecorder.class);
		bind(DataSourceManager.class).to(YamlDataSourceManager.class);
		bind(SolrScheduler.class).to(DefaultSolrScheduler.class);
		bind(JmxManager.class).to(DefaultJmxManager.class);

		bind(String.class).annotatedWith(Names.named("dshistory-filename")).toInstance("datasource-history.yml");

		bind(String.class).annotatedWith(Names.named("cmdhistory-filename")).toInstance("solr-cmd-history.yml");

		bind(String.class).annotatedWith(Names.named("defaults-filename")).toInstance("defaults.yml");

		bind(ServerResource.class).to(FieldResource.class);
	}

	public void init(Injector injector) {
		Defaults.INSTANCE = (Defaults) injector.getInstance(Defaults.class);
		Defaults.injector = injector;
		Defaults.INSTANCE.setReadOnly(true);
		try {
			SolrResourceLoader loader = new SolrResourceLoader(Constants.GAIA_CONF_HOME + "/solr");
			cores = new LweCoreContainer(loader, ConfigSolr.fromFile(loader, new File(Constants.GAIA_CONF_HOME
					+ "/solr/solr.xml")));
			cores.load();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Provides
	public CoreContainer providesCoreContainer() throws ParserConfigurationException, IOException, SAXException {
		return cores;
	}

	static {
		if (Constants.GAIA_DATA_HOME == null) {
			throw new IllegalStateException("lucidworksDataHome must be set but was not found");
		}

		if (Constants.GAIA_APP_HOME == null) {
			throw new IllegalStateException("lucidworksAppHome must be set but was not found");
		}

		if (Constants.GAIA_CONF_HOME == null) {
			throw new IllegalStateException("lucidworksConfHome must be set but was not found");
		}

		if (Constants.GAIA_LOGS_HOME == null) {
			throw new IllegalStateException("lucidworksLogsHome must be set but was not found");
		}
		System.setProperty("org.restlet.engine.loggerFacadeClass", "org.restlet.ext.slf4j.Slf4jLoggerFacade");

		System.setProperty("zkServerDataDir", Constants.STORAGE_PATH);
	}
}
