package gaia.crawl.gcm.api;

public class ConnectorType {
	private String version;
	private String type;

	public String getVersion() {
		return this.version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public String getType() {
		return this.type;
	}

	public void setType(String typeName) {
		this.type = typeName;
	}

	public String toString() {
		return getType() + "(" + getVersion() + ")";
	}
}
