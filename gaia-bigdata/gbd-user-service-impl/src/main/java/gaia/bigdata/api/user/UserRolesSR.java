package gaia.bigdata.api.user;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.restlet.resource.Get;
import org.restlet.resource.Post;
import org.restlet.resource.ResourceException;
import org.restlet.security.Role;

import com.google.inject.Inject;
import gaia.commons.api.Configuration;
import gaia.bigdata.api.SDARole;

public class UserRolesSR extends BaseUserSR implements UserRolesResource {
	protected String username;

	@Inject
	public UserRolesSR(Configuration configuration, UserService userService) {
		super(configuration, userService);
	}

	protected void doInit() throws ResourceException {
		username = getRequest().getAttributes().get("username").toString();
	}

	@Get
	public List<Role> retrieve() {
		Collection<SDARole> sdaRoles = userService.lookupRoles(username);
		List<Role> result = new ArrayList<Role>(sdaRoles.size());
		for (SDARole sdaRole : sdaRoles) {
			result.add(sdaRole.getRole());
		}
		return result;
	}

	@Post
	public boolean add(List<String> roleNames) {
		boolean result = false;
		User user = userService.lookupByUsername(username);
		if (user != null) {
			for (String roleName : roleNames) {
				userService.addUserToRole(user, SDARole.valueOf(roleName.toUpperCase()));
			}
			result = true;
		}
		return result;
	}
}
