package gaia.security.ad;

import java.util.HashSet;

class ADGroup {
	private final String dn;
	private String sid;
	private HashSet<ADGroup> parents = new HashSet<ADGroup>();

	private HashSet<ADGroup> children = new HashSet<ADGroup>();

	public ADGroup(String dn) {
		this.dn = dn;
	}

	public String getSid() {
		return sid;
	}

	public void setSid(String sid) {
		this.sid = sid;
	}

	public String getDN() {
		return dn;
	}

	public HashSet<ADGroup> getParents() {
		return parents;
	}

	public void addParent(ADGroup parent) {
		parents.add(parent);
	}

	public void addChildren(ADGroup child) {
		children.add(child);
	}

	public int hashCode() {
		return dn.hashCode();
	}

	public boolean equals(Object obj) {
		ADGroup other = (ADGroup) obj;
		return dn.equals(other.dn);
	}
}
