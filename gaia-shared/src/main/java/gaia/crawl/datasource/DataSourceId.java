package gaia.crawl.datasource;

import java.io.Serializable;

@SuppressWarnings("serial")
public class DataSourceId implements Serializable {
	public static final String DEFAULT_USER = "";
	public static final char SEPARATOR = '_';
	String id;
	String user;

	DataSourceId() {
	}

	public DataSourceId(String dsId) {
		int pos = dsId.indexOf('_');
		if (pos != -1) {
			user = dsId.substring(0, pos);
			id = replaceSeparators(dsId.substring(pos + 1));
		} else {
			user = "";
			id = dsId;
		}
	}

	public DataSourceId(String user, String id) {
		this.user = (user != null ? replaceSeparators(user) : "");
		this.id = replaceSeparators(id);
	}

	public DataSourceId(DataSourceId other) {
		user = other.user;
		id = other.id;
	}

	private String replaceSeparators(String val) {
		val = val.replace('_', '.');
		val = val.replaceAll("-", "");
		return val;
	}

	public String getUser() {
		return user;
	}

	public String getId() {
		return id;
	}

	public String toString() {
		if ((user != null) && (user.length() > 0)) {
			return user + '_' + id;
		}
		return id;
	}

	public int hashCode() {
		int prime = 31;
		int result = 1;
		result = prime * result + (id == null ? 0 : id.hashCode());
		result = prime * result + (user == null ? 0 : user.hashCode());
		return result;
	}

	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		DataSourceId other = (DataSourceId) obj;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		if (user == null) {
			if (other.user != null)
				return false;
		} else if (!user.equals(other.user))
			return false;
		return true;
	}
}
