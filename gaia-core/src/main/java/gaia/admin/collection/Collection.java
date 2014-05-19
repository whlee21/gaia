package gaia.admin.collection;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import gaia.NamedObject;
import gaia.admin.roles.Role;

public class Collection extends NamedObject implements Serializable {
	protected Map<String, Role> roles = new HashMap<String, Role>();
	private Map<String, ScheduledSolrCommand> cmds = new HashMap<String, ScheduledSolrCommand>();
	private String instanceDir;

	public Collection() {
		commonInit();
	}

	public Collection(Long id) {
		super(id);
		commonInit();
	}

	public Collection(String name) {
		super(name);
		commonInit();
	}

	private void commonInit() {
	}

	public Map<String, Role> getRoles() {
		return roles;
	}

	public void reset() {
		roles.clear();
	}

	public void setRoles(Map<String, Role> roles) {
		this.roles = roles;
	}

	public int hashCode() {
		int prime = 31;
		int result = super.hashCode();
		result = prime * result + (int) (id.longValue() ^ id.longValue() >>> 32);
		return result;
	}

	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		Collection other = (Collection) obj;
		if (!id.equals(other.id))
			return false;
		return true;
	}

	public String toString() {
		return name;
	}

	public String getInstanceDir() {
		return instanceDir;
	}

	public void setInstanceDir(String instanceDir) {
		this.instanceDir = instanceDir;
	}

	public Map<String, ScheduledSolrCommand> getCmds() {
		return cmds;
	}

	public void setCmds(Map<String, ScheduledSolrCommand> cmds) {
		this.cmds = cmds;
	}
}
