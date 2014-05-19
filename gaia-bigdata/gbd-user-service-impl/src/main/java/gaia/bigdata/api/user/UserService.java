package gaia.bigdata.api.user;

import gaia.bigdata.api.SDARole;
import java.util.Collection;
import java.util.Iterator;
import java.util.regex.Pattern;

public interface UserService {
	public Collection<User> listUsers();

	public Collection<User> listUsers(Pattern paramPattern);

	public Iterator<User> iterator();

	public User lookupByUsername(String paramString);

	public User deleteUser(User paramUser);

	public User deleteUserByUserName(String paramString);

	public User addUser(User paramUser);

	public User updateUser(User paramUser);

	public Collection<SDARole> lookupRoles(String paramString);

	public void addUserToRole(User paramUser, SDARole paramSDARole);

	public void removeUserFromRole(User paramUser, SDARole paramSDARole);
}
