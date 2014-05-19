package gaia.bigdata.api.user;

import gaia.commons.api.Configuration;
import gaia.commons.services.BaseServerResource;
import gaia.commons.util.StringUtil;

public class BaseUserSR extends BaseServerResource {
	protected UserService userService;

	public BaseUserSR(Configuration configuration, UserService userService) {
		super(configuration);
		this.userService = userService;
	}

	protected void hashPassword(User user) {
		if (user.password != null)
			user.password = StringUtil.md5Hash(user.password);
	}
}