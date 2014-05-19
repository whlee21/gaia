package gaia.crawl.api;

import gaia.Defaults;
import gaia.commons.api.APIModule;
import gaia.crawl.ConnectorManager;
import gaia.crawl.DataSourceHistory;
import gaia.crawl.DataSourceManager;
import gaia.crawl.DefaultHistoryRecorder;
import gaia.crawl.History;
import gaia.crawl.HistoryRecorder;
import gaia.crawl.LocalConnectorManager;
import gaia.crawl.YamlDataSourceManager;

import com.google.inject.name.Names;

public class ConnectorsAPIModule extends APIModule {
	protected void defineBindings() {
		bind(ConnectorManager.class).to(LocalConnectorManager.class);
		bind(History.class).to(DataSourceHistory.class);

		bind(String.class).annotatedWith(Names.named("role-mappings-filename")).toInstance("role-mappings.yml");
		bind(String.class).annotatedWith(Names.named("collections-filename")).toInstance("collections.yml");
		bind(String.class).annotatedWith(Names.named("datasources-filename")).toInstance("datasources.yml");
		bind(String.class).annotatedWith(Names.named("users-filename")).toInstance("users.yml");

		bind(DataSourceManager.class).to(YamlDataSourceManager.class);
		bind(HistoryRecorder.class).to(DefaultHistoryRecorder.class);

		bind(String.class).annotatedWith(Names.named("dshistory-filename")).toInstance("datasource-history.yml");

		bind(String.class).annotatedWith(Names.named("cmdhistory-filename")).toInstance("solr-cmd-history.yml");

		bind(String.class).annotatedWith(Names.named("defaults-filename")).toInstance("defaults.yml");
	}

	public void initInjectorDependent() {
		Defaults.INSTANCE = (Defaults) injector.getInstance(Defaults.class);
		Defaults.injector = injector;
	}
}