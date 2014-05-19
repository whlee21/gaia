package gaia.bigdata.api.workflow;

import java.io.Serializable;
import java.net.URI;
import java.util.Date;
import java.util.Properties;

public class Workflow implements Serializable {
	private static final long serialVersionUID = 1L;
	private String id;
	private URI uri;
	private Properties properties;
	private Date modificationTime;

	public Workflow() {
	}

	public Workflow(String id, URI uri, Properties properties, Date modificationTime) {
		this.id = id;
		this.uri = uri;
		this.properties = properties;
		this.modificationTime = modificationTime;
	}

	public String getId() {
		return this.id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public URI getUri() {
		return this.uri;
	}

	public void setUri(URI uri) {
		this.uri = uri;
	}

	public Properties getProperties() {
		return this.properties;
	}

	public void setProperties(Properties properties) {
		this.properties = properties;
	}

	public Date getModificationTime() {
		return this.modificationTime;
	}

	public void setModificationTime(Date modificationTime) {
		this.modificationTime = modificationTime;
	}

	public String toString() {
		return "Workflow [id=" + id + ", uri=" + uri + ", properties=" + properties + ", modificationTime="
				+ modificationTime + "]";
	}
}
