package gaia.bigdata.users;

import gaia.bigdata.hbase.Key;

public class UserKey implements Key {
	public String username;

	public UserKey(String username) {
		this.username = username;
	}

	public String toString() {
		return "UserKey [username=" + username + "]";
	}
}
