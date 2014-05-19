package gaia.bigdata.api.document;

import gaia.bigdata.ResultType;
import gaia.bigdata.api.State;
import java.util.EnumSet;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public interface DocumentService {
	public EnumSet<ResultType> getSupportedResultTypes();

	public JSONObject retrieve(String paramString1, String paramString2);

	public JSONObject retrieve(String paramString1, String paramString2, Map<String, Object> paramMap);

	public JSONObject retrieve(String paramString, DocServiceRequest paramDocServiceRequest);

	public State add(String paramString, JSONArray paramJSONArray, JSONObject paramJSONObject1,
			JSONObject paramJSONObject2) throws UnsupportedOperationException;

	public State update(String paramString, JSONArray paramJSONArray, JSONObject paramJSONObject1,
			JSONObject paramJSONObject2) throws UnsupportedOperationException;

	public State delete(String paramString, JSONArray paramJSONArray, JSONObject paramJSONObject)
			throws UnsupportedOperationException, JSONException;
}
