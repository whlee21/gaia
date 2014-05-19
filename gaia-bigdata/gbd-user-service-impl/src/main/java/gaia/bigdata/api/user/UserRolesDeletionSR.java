package gaia.bigdata.api.user;

import org.restlet.resource.Delete;
import org.restlet.resource.ResourceException;

import com.google.inject.Inject;
import gaia.commons.api.Configuration;
import gaia.bigdata.api.SDARole;

public class UserRolesDeletionSR extends BaseUserSR implements UserRolesDeletionResource {
	protected String username;
	protected String rolename;

	@Inject
	public UserRolesDeletionSR(Configuration configuration, UserService userService) {
		super(configuration, userService);
	}

	protected void doInit() throws ResourceException {
		username = getRequest().getAttributes().get("username").toString();
		rolename = getRequest().getAttributes().get("rolename").toString();
	}

	@Delete
	public boolean remove() {
		boolean result = false;
		User user = userService.lookupByUsername(username);
		if (user != null) {
			SDARole role = SDARole.valueOf(rolename.toUpperCase());
			if (role != null) {
				userService.removeUserFromRole(user, role);
				result = true;
			}
		}
		return result;
	}
}
