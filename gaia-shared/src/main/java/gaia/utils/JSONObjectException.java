package gaia.utils;

import org.json.JSONObject;

public class JSONObjectException extends RuntimeException {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private JSONObject json;

	public JSONObjectException(JSONObject json) {
		this.json = json;
	}

	public JSONObject getJson() {
		return json;
	}

	public String toString() {
		return json.toString();
	}
}
