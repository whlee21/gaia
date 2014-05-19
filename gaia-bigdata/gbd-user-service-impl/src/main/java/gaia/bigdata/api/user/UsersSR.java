package gaia.bigdata.api.user;

import java.util.Collection;
import java.util.regex.Pattern;

import org.restlet.data.Form;
import org.restlet.resource.Get;
import org.restlet.resource.Post;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import gaia.commons.api.Configuration;
import gaia.commons.services.ServiceLocator;

public class UsersSR extends BaseUserSR implements UsersResource {
	private static transient Logger log = LoggerFactory.getLogger(UsersSR.class);
	protected ServiceLocator serviceLocator;

	@Inject
	public UsersSR(Configuration configuration, ServiceLocator serviceLocator, UserService userService) {
		super(configuration, userService);
		this.serviceLocator = serviceLocator;
	}

	@Get
	public Collection<User> listUsers() {
		Form form = getRequest().getResourceRef().getQueryAsForm();
		String regexStr = form.getValues("regex") != null ? form.getValues("regex") : null;
		if ((regexStr != null) && (!regexStr.isEmpty())) {
			Pattern pattern = Pattern.compile(regexStr);
			return userService.listUsers(pattern);
		}
		return userService.listUsers();
	}

	@Post
	public User addUser(User user) {
		User result = null;
		if (user != null) {
			hashPassword(user);
			result = userService.addUser(user);
		}
		return result;
	}
}
