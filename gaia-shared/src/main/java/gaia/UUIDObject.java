package gaia;

import java.io.Serializable;
import java.util.Date;
import java.util.UUID;

public class UUIDObject implements Serializable {
	private static final long serialVersionUID = -5749489281759820597L;
	protected String id;
	protected Date createDate;
	protected Date lastModified;

	public UUIDObject() {
		createDate = new Date();
		createId();
	}

	protected void createId() {
		id = UUID.randomUUID().toString().replaceAll("-", "");
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public Date getCreateDate() {
		return createDate;
	}

	public void setCreateDate(Date createDate) {
		this.createDate = createDate;
	}

	public Date getLastModified() {
		return lastModified;
	}

	public void setLastModified(Date lastModified) {
		this.lastModified = lastModified;
	}
}
