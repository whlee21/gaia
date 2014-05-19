package gaia.crawl.api;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import gaia.commons.api.API;
import gaia.commons.api.ResourceFinder;

@Singleton
public class ConnectorsAPI extends API {
	@Inject
	protected ConnectorsAPI(ResourceFinder finder) {
		super(finder);
	}

	public String getAPIName() {
		return "CONNECTORS";
	}

	public String getAPIRoot() {
		return "/connectors";
	}

	protected void initAttachments() {
		attach("/mgr", ConnectorManagerServerResource.class);
	}
}