package gaia.bigdata.api.analytics;

import gaia.bigdata.analytics.AnalyticsService;
import gaia.bigdata.analytics.hbase.HBaseAnalyticsService;
import gaia.commons.api.APIModule;

public class AnalyticsAPIModule extends APIModule {
	protected void defineBindings() {
		bind(AnalyticsService.class).to(HBaseAnalyticsService.class);
	}
}
