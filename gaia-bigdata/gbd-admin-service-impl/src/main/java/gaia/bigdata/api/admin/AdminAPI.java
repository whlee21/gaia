package gaia.bigdata.api.admin;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import gaia.commons.api.API;
import gaia.commons.api.ResourceFinder;
import gaia.bigdata.services.ServiceType;

@Singleton
public class AdminAPI extends API {
	@Inject
	public AdminAPI(ResourceFinder finder) {
		super(finder);
	}

	protected void initAttachments() {
		attach("/info", AdminInfoSR.class);
		attach("/info/statistics", AdminStatsSR.class);
		attach("/info/endpoints", AdminEndpointsSR.class);
		attach("/info/services", AdminServicesSR.class);
	}

	public String getAPIRoot() {
		return "/admin";
	}

	public String getAPIName() {
		return ServiceType.ADMIN.name();
	}
}
