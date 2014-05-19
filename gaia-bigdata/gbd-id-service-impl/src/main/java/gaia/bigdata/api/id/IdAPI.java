package gaia.bigdata.api.id;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import gaia.commons.api.API;
import gaia.commons.api.ResourceFinder;
import gaia.bigdata.services.ServiceType;

@Singleton
public class IdAPI extends API {
	@Inject
	protected IdAPI(ResourceFinder finder) {
		super(finder);
	}

	protected void initAttachments() {
		router.attach("", finder.finderOf(IdServerResource.class));
	}

	public String getAPIRoot() {
		return "/id";
	}

	public String getAPIName() {
		return ServiceType.ID.name();
	}
}
