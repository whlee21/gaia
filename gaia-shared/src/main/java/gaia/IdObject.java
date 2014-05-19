package gaia;

import java.io.Serializable;
import java.util.Date;

public class IdObject implements Serializable {
	protected Long id = Long.valueOf(-1L);
	protected Date createDate;
	protected Date lastModified;

	public IdObject() {
		createDate = new Date();
		lastModified = createDate;
	}

	public IdObject(Long id) {
		this.id = id;
		createDate = new Date();
		lastModified = createDate;
	}

	public Date getCreateDate() {
		return createDate;
	}

	public void setCreateDate(Date createDate) {
		this.createDate = createDate;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Date getLastModified() {
		return lastModified;
	}

	public void setLastModified(Date lastModified) {
		this.lastModified = lastModified;
	}

	public boolean equals(Object o) {
		if (this == o)
			return true;
		if ((o == null) || (getClass() != o.getClass()))
			return false;

		IdObject idObject = (IdObject) o;

		if (id != null ? !id.equals(idObject.id) : idObject.id != null)
			return false;

		return true;
	}

	public int hashCode() {
		return id != null ? id.hashCode() : 0;
	}

	protected IdObject clone() {
		IdObject idObject;
		try {
			idObject = (IdObject) super.clone();
		} catch (CloneNotSupportedException e) {
			throw new IllegalStateException(e);
		}
		idObject.setId(id);
		idObject.setCreateDate(createDate);
		idObject.setLastModified(lastModified);
		return idObject;
	}
}
