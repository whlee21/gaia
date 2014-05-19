package gaia.bigdata.api;

import org.restlet.security.Role;

public enum SDARole {
	ADMINISTRATOR("administrator", "Complete access to all APIs"),
	ANALYSIS("analysis", "Access to Analysis functions"),
	CLASSIFICATION("classification", "Access to classification functions"),
	COLLECTIONS("collections", "Access to Collections"),
	DATASOURCES("datasources","Access to Data Sources"),
	DEFAULT("default","Access to all non-admin endpoints"),
	DOCUMENTS("documents","Access to Documents"),
	JOBS("jobs", "Access to jobs"),
	GAIASEARCH("gaiasearch", "Access to GaiaSearch"),
	WEBHDFS("webhdfs","Access to WebHDFS"),
	WORKFLOWS("workflows", "Access to workflows"),
	USERS("users", "Access to User info");

	private Role theRole;

	private SDARole(String name, String desc) {
		theRole = new Role(name, desc);
	}

	public Role getRole() {
		return theRole;
	}
}
