package gaia.bigdata.api.analytics;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import gaia.commons.api.API;
import gaia.commons.api.ResourceFinder;
import gaia.bigdata.services.ServiceType;

@Singleton
public class AnalyticsAPI extends API {
	@Inject
	public AnalyticsAPI(ResourceFinder finder) {
		super(finder);
	}

	protected void initAttachments() {
		attach("/{collection}", CollectionMetricServerResource.class);
	}

	public String getAPIRoot() {
		return "/analysis";
	}

	public String getAPIName() {
		return ServiceType.ANALYTICS.name();
	}
}
