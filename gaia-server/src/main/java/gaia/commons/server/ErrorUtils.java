package gaia.commons.server;

import gaia.commons.server.api.services.ResultStatus;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ErrorUtils {
	private static final Logger LOG = LoggerFactory.getLogger(ErrorUtils.class);

	public static long LOW_DISK_BYTES_WARNING_AMOUNT = 4096L;

	public static Map<String, Object> mapError(String msg) {
		HashMap<String, Object> m = new HashMap<String, Object>(1);
		m.put("error", msg);
		return m;
	}

	public static List<String> listError(String msg) {
		ArrayList<String> l = new ArrayList<String>(1);
		l.add(msg);
		return l;
	}

	public static GaiaException statusExp(ResultStatus.STATUS status, Error error) {
		return statusExp(status, Collections.singletonList(error));
	}

	public static GaiaException statusExp(ResultStatus.STATUS status, List<Error> errors) {
		// JSONArray json = new JSONArray();
		// for (Error error : errors) {
		// JSONObject jsonObject = new JSONObject();
		// try {
		// jsonObject.put("key", error.getKey());
		// jsonObject.put("code", error.getCode());
		// jsonObject.put("message", error.getMessage());
		// } catch (JSONException e) {
		// throw new RuntimeException(e);
		// }
		// json.put(jsonObject);
		// }
		// FIXME: by whlee21
		// try {
		// long free = new File(Constants.GAIA_DATA_HOME).getFreeSpace();
		// if (free < LOW_DISK_BYTES_WARNING_AMOUNT) {
		// JSONObject jsonObject = new JSONObject();
		// jsonObject.put("code", Error.E_LOW_DISK_SPACE);
		// jsonObject.put("message", "Low disk space available: " +
		// FileUtils.humanReadableUnits(free));
		//
		// jsonObject.put("free.bytes", free);
		// json.put(jsonObject);
		// }
		// } catch (Exception e) {
		// LOG.error("Problem checking free disk space", e);
		// }

		return new GaiaException(status, errors);
	}

	public static GaiaException statusExp(ResultStatus.STATUS status, String message) {
		return statusExp(status, new Error("", Error.E_EXCEPTION, message));
	}

	public static GaiaException statusExp(Exception e) {
		if ((e instanceof GaiaException)) {
			return (GaiaException) e;
		}
		return new GaiaException(e);
	}

	// public static void toJavaMap(JSONObject o, Map<String, Object> b) throws
	// JSONException {
	// Iterator ji = o.keys();
	// while (ji.hasNext()) {
	// String key = (String) ji.next();
	// Object val = o.get(key);
	// if (val == null) {
	// b.put(key, null);
	// } else if (val.getClass() == JSONObject.class) {
	// Map<String, Object> sub = new HashMap<String, Object>();
	// toJavaMap((JSONObject) val, sub);
	// b.put(key, sub);
	// } else if (val.getClass() == JSONArray.class) {
	// List<Object> l = new ArrayList<Object>();
	// JSONArray arr = (JSONArray) val;
	// for (int a = 0; a < arr.length(); a++) {
	// Map<String, Object> sub = new HashMap<String, Object>();
	// Object element = arr.get(a);
	// if ((element instanceof JSONObject)) {
	// toJavaMap((JSONObject) element, sub);
	// l.add(sub);
	// } else {
	// l.add(element);
	// }
	// }
	// b.put(key, l);
	// } else {
	// b.put(key, val);
	// }
	// }
	// }

	// public static void toJavaList(JSONArray ar, List<Object> ll) throws
	// JSONException {
	// int i = 0;
	// while (i < ar.length()) {
	// Object val = ar.get(i);
	// if (val.getClass() == JSONObject.class) {
	// Map<String, Object> sub = new HashMap<String, Object>();
	// toJavaMap((JSONObject) val, sub);
	// ll.add(sub);
	// } else if (val.getClass() == JSONArray.class) {
	// JSONArray arr = (JSONArray) val;
	// for (int a = 0; a < arr.length(); a++) {
	// Map<String, Object> sub = new HashMap<String, Object>();
	// Object element = arr.get(a);
	// if ((element instanceof JSONObject)) {
	// toJavaMap((JSONObject) element, sub);
	// ll.add(sub);
	// } else {
	// ll.add(element);
	// }
	// }
	// } else {
	// ll.add(val);
	// }
	// i++;
	// }
	// }
}
