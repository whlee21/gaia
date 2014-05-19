package gaia.crawl;

import gaia.Defaults;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.name.Names;

public class ConnectorsModule extends AbstractModule {
	protected void configure() {
		bind(String.class).annotatedWith(Names.named("datasources-filename")).toInstance("datasources.yml");

		bind(ConnectorManager.class).to(LocalConnectorManager.class);
		bind(HistoryRecorder.class).to(DefaultHistoryRecorder.class);
		bind(DataSourceManager.class).to(YamlDataSourceManager.class);

		bind(String.class).annotatedWith(Names.named("dshistory-filename")).toInstance("datasource-history.yml");

		bind(String.class).annotatedWith(Names.named("defaults-filename")).toInstance("defaults.yml");
	}

	public void init(Injector injector) {
		Defaults.INSTANCE = (Defaults) injector.getInstance(Defaults.class);
		Defaults.INSTANCE.setReadOnly(true);
		Defaults.injector = injector;
	}

	static {
		System.setProperty("org.restlet.engine.loggerFacadeClass", "org.restlet.ext.slf4j.Slf4jLoggerFacade");
	}
}