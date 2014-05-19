package gaia.crawl.connector;

import java.io.IOException;

import gaia.Defaults;
import gaia.api.JsonSerializer;
import gaia.api.ObjectSerializer;
import gaia.crawl.ConnectorManager;
import gaia.crawl.DataSourceManager;
import gaia.crawl.DefaultHistoryRecorder;
import gaia.crawl.HistoryRecorder;
import gaia.crawl.LocalConnectorManager;
import gaia.crawl.UpdateController;
import gaia.crawl.YamlDataSourceManager;
import gaia.crawl.connector.api.services.parsers.JsonRequestBodyParser;
import gaia.crawl.connector.api.services.parsers.RequestBodyParser;
import gaia.crawl.connector.configuration.Configuration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.name.Names;

public class GaiaConnectorModule extends AbstractModule {

	private static final Logger LOG = LoggerFactory.getLogger(GaiaConnectorModule.class);

	private final Configuration configs;

	public GaiaConnectorModule(Configuration configs) {
		this.configs = configs;
	}

	@Override
	protected void configure() {

		LOG.debug("whlee21 configure");

		bind(String.class).annotatedWith(Names.named("datasources-filename")).toInstance("datasources.yml");
		
		requestStaticInjection(UpdateController.class);
		String solrAddress;
		try {
			solrAddress = configs.getSolrAddress(true, null).toString();
		} catch (IOException e) {
			solrAddress = configs.getClientApiUrl().toString() + "/solr";
		}

		LOG.debug("whlee21 solrAddress = " + solrAddress);
		bind(String.class).annotatedWith(Names.named("solr-address")).toInstance(solrAddress);

		bind(ConnectorManager.class).to(LocalConnectorManager.class);
		bind(HistoryRecorder.class).to(DefaultHistoryRecorder.class);
		bind(DataSourceManager.class).to(YamlDataSourceManager.class);

		bind(String.class).annotatedWith(Names.named("dshistory-filename")).toInstance("datasource-history.yml");

		bind(String.class).annotatedWith(Names.named("defaults-filename")).toInstance("defaults.yml");

		bind(ObjectSerializer.class).toInstance(new JsonSerializer());
		bind(RequestBodyParser.class).toInstance(new JsonRequestBodyParser());
	}

	public void init(Injector injector) {
		Defaults.INSTANCE = (Defaults) injector.getInstance(Defaults.class);
		Defaults.injector = injector;
		Defaults.INSTANCE.setReadOnly(true);
		configs.setInjector(injector);
	}

}