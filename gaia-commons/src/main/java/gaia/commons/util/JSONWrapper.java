package gaia.commons.util;

import org.codehaus.jackson.annotate.JsonRawValue;

public class JSONWrapper {
	private String json;

	public JSONWrapper(String json) {
		this.json = json;
	}

	@JsonRawValue
	public String getJSON() {
		return json;
	}
}
