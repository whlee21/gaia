package gaia.bigdata.api.user.hbase;

import gaia.bigdata.api.user.User;
import gaia.bigdata.api.user.UserService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.regex.Pattern;

import org.restlet.data.Status;
import org.restlet.resource.ResourceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import gaia.commons.api.Configuration;
import gaia.commons.services.BaseService;
import gaia.commons.services.ServiceLocator;
import gaia.bigdata.api.SDARole;
import gaia.bigdata.hbase.users.UserTable;
import gaia.bigdata.services.ServiceType;

@Singleton
public class HBaseUserService extends BaseService implements UserService {
	private static transient Logger LOG = LoggerFactory.getLogger(HBaseUserService.class);
	final UserTable table;

	@Inject
	public HBaseUserService(Configuration config, ServiceLocator locator) {
		super(config, locator);
		String zkConnect = config.getProperties().getProperty("hbase.zk.connect");
		if (zkConnect == null) {
			throw new IllegalArgumentException("Missing required config value: hbase.zk.connect");
		}
		table = new UserTable(zkConnect);
	}

	public String getType() {
		return ServiceType.USER.name();
	}

	public Collection<User> listUsers() {
		ArrayList<User> users = new ArrayList<User>();
		try {
			for (User user : table.listUsers())
				users.add(user);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return users;
	}

	public Collection<User> listUsers(Pattern usernameRegex) {
		ArrayList<User> users = new ArrayList<User>();
		try {
			for (User user : table.grepUser(usernameRegex.pattern()))
				users.add(user);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return users;
	}

	public Iterator<User> iterator() {
		Iterator<User> it;
		try {
			it = table.listUsers().iterator();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return it;
	}

	public User lookupByUsername(String username) {
		try {
			User user = table.getUser(username);
			if (user == null) {
				throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND, "User not found");
			}
			return user;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public User deleteUser(User user) {
		if (user.username == null) {
			throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, "Missing 'username' field");
		}
		return deleteUserByUserName(user.username);
	}

	public User deleteUserByUserName(String username) {
		try {
			User user = table.getUser(username);
			if (user == null) {
				throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, "User '" + username + "' does not exist");
			}
			table.deleteUser(user.username);
			return user;
		} catch (IOException e) {
			throw new ResourceException(Status.SERVER_ERROR_INTERNAL, "Could not delete user", e);
		}
	}

	public Collection<SDARole> lookupRoles(String username) {
		Collection<SDARole> returnValue = new HashSet<SDARole>();
		try {
			User user = table.getUser(username);
			for (String role : user.getRoles()) {
				SDARole sdaRole = SDARole.valueOf(role);
				returnValue.add(sdaRole);
			}
		} catch (IOException e) {
			throw new ResourceException(Status.SERVER_ERROR_INTERNAL, "Could not find user's roles", e);
		}
		return returnValue;
	}

	public User addUser(User user) {
		if (user.username == null) {
			throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, "Missing 'username' field");
		}
		if (user.password == null)
			throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, "Missing 'password' field");
		try {
			table.putUser(user);
			return table.getUser(user.username);
		} catch (IOException e) {
			throw new ResourceException(e);
		}
	}

	public User updateUser(User user) {
		try {
			return table.updateUser(user);
		} catch (IOException e) {
			throw new ResourceException(e);
		}
	}

	public void addUserToRole(User user, SDARole role) {
		if (user.username == null)
			throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, "Missing 'username' field");
		try {
			User actual = table.getUser(user.username);
			actual.getRoles().add(role.toString());
			table.updateUser(actual);
		} catch (IOException e) {
			throw new ResourceException(e);
		}
	}

	public void removeUserFromRole(User user, SDARole role) {
		if (user.username == null)
			throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, "Missing 'username' field");
		try {
			table.deleteRoleFromUser(user.username, role.toString());
		} catch (IOException e) {
			throw new ResourceException(e);
		}
	}
}
