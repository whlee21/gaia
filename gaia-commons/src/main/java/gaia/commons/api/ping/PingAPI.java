package gaia.commons.api.ping;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import gaia.commons.api.API;
import gaia.commons.api.ResourceFinder;

@Singleton
public class PingAPI extends API {
	@Inject
	public PingAPI(ResourceFinder finder) {
		super(finder);
	}

	protected void initAttachments() {
		attach("", PingServerResource.class);
	}

	public String getAPIRoot() {
		return "/ping";
	}

	public String getAPIName() {
		return "PING";
	}
}
