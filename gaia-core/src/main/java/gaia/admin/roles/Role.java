package gaia.admin.roles;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import gaia.NamedObject;

public class Role extends NamedObject implements Cloneable {
	public static final String DEFAULT_ROLE = "DEFAULT";
	private List<String> groups = new ArrayList<String>();
	private List<String> users = new ArrayList<String>();
	private volatile Map<String, List<String>> info = new HashMap<String, List<String>>();

	public Role() {
	}

	public Role(String name) {
		this.name = name;
	}

	public synchronized Role clone() {
		Role role = (Role) super.clone();
		role.setName(name);
		return role;
	}

	public synchronized void addUser(String[] users) {
		for (String user : users)
			this.users.add(user);
	}

	public synchronized void addGroup(String[] groups) {
		for (String group : groups)
			this.groups.add(group);
	}

	public synchronized void add(String user, String group) {
		groups.add(group);
		users.add(user);
	}

	public List<String> getGroups() {
		return Collections.unmodifiableList(groups);
	}

	public List<String> getUsers() {
		return Collections.unmodifiableList(users);
	}

	public void setGroups(List<String> groups) {
		this.groups = groups;
	}

	public void setUsers(List<String> users) {
		this.users = users;
	}

	public Map<String, List<String>> getInfo() {
		return info;
	}

	public void setInfo(Map<String, List<String>> info) {
		this.info = info;
	}
}
