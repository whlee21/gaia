package gaia.bigdata.api.user.property;

import gaia.bigdata.api.SDARole;
import gaia.bigdata.api.user.User;
import gaia.bigdata.api.user.UserService;
import gaia.bigdata.services.ServiceType;
import gaia.commons.api.Configuration;
import gaia.commons.services.BaseService;
import gaia.commons.services.ServiceLocator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class PropertiesUserService extends BaseService implements UserService {
	private Map<String, User> users;
	private Map<String, List<SDARole>> userRoles;

	@Inject
	public PropertiesUserService(Configuration config, ServiceLocator locator) {
		super(config, locator);
		Properties props = config.getProperties();
		users = new HashMap<String, User>();
		userRoles = new HashMap<String, List<SDARole>>();

		for (Iterator<Object> iter = props.keySet().iterator(); iter.hasNext();) {
			Object o = iter.next();
			String prop = o.toString();
			if ((prop.startsWith("user.")) && (prop.endsWith(".pass"))) {
				String username = prop.replace("user.", "").replace(".pass", "");
				String pass = props.getProperty("user." + username + ".pass");
				User user = new User(username, pass);
				users.put(username, user);
				String roleStr = props.getProperty("user." + username + ".roles");
				if ((roleStr != null) && (!roleStr.isEmpty())) {
					String[] roles = roleStr.split(",");
					List<SDARole> sdaRoles = userRoles.get(username);
					if (sdaRoles == null) {
						sdaRoles = new ArrayList<SDARole>();
						userRoles.put(username, sdaRoles);
					}
					for (int i = 0; i < roles.length; i++) {
						String role = roles[i];
						SDARole theRole = SDARole.valueOf(role.toUpperCase());
						sdaRoles.add(theRole);
					}
				}
			}
		}
	}

	public String getType() {
		return ServiceType.USER.name();
	}

	public Collection<User> listUsers() {
		return users.values();
	}

	public Collection<User> listUsers(Pattern usernameRegex) {
		Collection<User> result = new ArrayList<User>();
		for (User user : users.values()) {
			Matcher matcher = usernameRegex.matcher(user.username);
			if (matcher.matches()) {
				result.add(user);
			}
		}
		return result;
	}

	public Iterator<User> iterator() {
		return users.values().iterator();
	}

	public User lookupByUsername(String username) {
		return (User) users.get(username);
	}

	public User deleteUser(User user) {
		return deleteUserByUserName(user.username);
	}

	public User deleteUserByUserName(String username) {
		userRoles.remove(username);
		return (User) users.remove(username);
	}

	public Collection<SDARole> lookupRoles(String username) {
		return userRoles.get(username);
	}

	public User addUser(User user) {
		users.put(user.username, user);
		return user;
	}

	public User updateUser(User user) {
		users.put(user.username, user);
		return user;
	}

	public void addUserToRole(User user, SDARole role) {
		List<SDARole> sdaRoles = userRoles.get(user.username);
		if (sdaRoles == null) {
			sdaRoles = new ArrayList<SDARole>();
			userRoles.put(user.username, sdaRoles);
		}
		sdaRoles.add(role);
	}

	public void removeUserFromRole(User user, SDARole role) {
		List<SDARole> roles = userRoles.get(user.username);
		if (roles != null)
			roles.remove(role);
	}
}
