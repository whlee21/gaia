package gaia.api;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class JSONWrapper {
	public JSONObject jsonObject;
	public JSONArray jsonArray;

	public JSONWrapper(String json) throws JSONException {
		if (json.startsWith("["))
			this.jsonArray = new JSONArray(json);
		else if (json.startsWith("{"))
			this.jsonObject = new JSONObject(json);
	}

	public String toString(int indent) throws JSONException {
		if (this.jsonArray != null)
			return this.jsonArray.toString(indent);
		if (this.jsonObject != null) {
			return this.jsonObject.toString(indent);
		}

		return "";
	}
}
