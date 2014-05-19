package gaia.bigdata.api.user;

import org.restlet.resource.Delete;
import org.restlet.resource.Get;
import org.restlet.resource.Put;
import org.restlet.resource.ResourceException;

import com.google.inject.Inject;
import gaia.commons.api.Configuration;

public class UserSR extends BaseUserSR implements UserResource {
	private String username;

	@Inject
	public UserSR(Configuration configuration, UserService userService) {
		super(configuration, userService);
	}

	protected void doInit() throws ResourceException {
		username = getRequest().getAttributes().get("username").toString();
	}

	@Delete
	public User remove() {
		return userService.deleteUserByUserName(username);
	}

	@Get
	public User retrieve() {
		return userService.lookupByUsername(username);
	}

	@Put
	public User update(User user) {
		hashPassword(user);
		user = userService.updateUser(user);
		return user;
	}
}
