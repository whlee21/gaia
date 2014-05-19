package gaia.crawl.datasource;

import java.io.Serializable;

public class Authentication implements Serializable {
	public static final String HOST = "host";
	public static final String REALM = "realm";
	public static final String USERNAME = "username";
	public static final String PASSWORD = "password";
	private String realm;
	private String host;
	private String username;
	private String password;

	public String getRealm() {
		return realm;
	}

	public void setRealm(String realm) {
		this.realm = realm;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public String getHost() {
		return host;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String toString() {
		return host + "|" + realm + "|" + username + "|" + password;
	}
}
