package gaia.bigdata.api.user;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.map.annotate.JsonSerialize;

@JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
public class User {
	public final String username;

	@JsonIgnore
	public final long version;
	public String password;
	public Map<String, String> properties;
	public Set<String> roles;

	public User(String username) {
		this(username, null, 0L);
	}

	@JsonCreator
	public User(@JsonProperty("username") String username, @JsonProperty("password") String password) {
		this.username = username;
		this.password = password;
		this.version = 0L;
	}

	public User(String username, String password, long version) {
		this.username = username;
		this.password = password;
		this.version = version;
	}

	public String getProperty(String key) {
		if (properties == null) {
			return null;
		}
		return (String) properties.get(key);
	}

	public void setProperty(String key, String value) {
		if (properties == null) {
			properties = new HashMap<String, String>();
		}
		properties.put(key, value);
	}

	public Set<String> getRoles() {
		if (roles == null) {
			roles = new HashSet<String>();
		}
		return roles;
	}

	public String toString() {
		return "User [username=" + username + ", properties=" + properties + ", roles=" + roles + "]";
	}
}
