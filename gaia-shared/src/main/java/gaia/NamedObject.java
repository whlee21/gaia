package gaia;

import java.io.Serializable;

public class NamedObject extends IdObject implements Serializable {
	protected String name;
	protected String description = "";

	public NamedObject() {
	}

	public NamedObject(Long id) {
		super(id);
	}

	public NamedObject(String name) {
		this.name = name;
	}

	public NamedObject(Long id, String name) {
		super(id);
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	protected NamedObject clone() {
		NamedObject no = (NamedObject) super.clone();
		no.description = description;
		no.name = name;
		return no;
	}
}
