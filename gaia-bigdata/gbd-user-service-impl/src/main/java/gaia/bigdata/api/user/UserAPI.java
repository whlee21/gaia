package gaia.bigdata.api.user;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import gaia.commons.api.API;
import gaia.commons.api.ResourceFinder;
import gaia.bigdata.services.ServiceType;

@Singleton
public class UserAPI extends API {
	@Inject
	public UserAPI(ResourceFinder finder) {
		super(finder);
	}

	protected void initAttachments() {
		attach("", UsersSR.class);
		attach("/{username}", UserSR.class);
		attach("/{username}/roles", UserRolesSR.class);
		attach("/{username}/roles/{rolename}", UserRolesDeletionSR.class);
	}

	public String getAPIRoot() {
		return "/users";
	}

	public String getAPIName() {
		return ServiceType.USER.name();
	}
}
