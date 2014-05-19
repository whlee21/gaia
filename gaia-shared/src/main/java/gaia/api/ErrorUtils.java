package gaia.api;

import gaia.Constants;
import gaia.utils.FileUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
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

	public static WebApplicationException statusExp(Response.Status status, Error error) {
		return statusExp(status, Collections.singletonList(error));
	}
	
	public static WebApplicationException statusExp(int statusCode, Error error) {
		return statusExp(statusCode, Collections.singletonList(error));
	}

	public static WebApplicationException statusExp(Response.Status status, List<Error> errors) {
		// JSONArray json = new JSONArray();
		List<Map<String, Object>> l = new ArrayList<Map<String, Object>>();
		for (Error error : errors) {
			// JSONObject jsonObject = new JSONObject();
			// try {
			// jsonObject.put("key", error.getKey());
			// jsonObject.put("code", error.getCode());
			// jsonObject.put("message", error.getMessage());
			Map<String, Object> m = new HashMap<String, Object>();
			m.put("key", error.getKey());
			m.put("code", error.getCode());
			m.put("message", error.getMessage());
			l.add(m);
			// } catch (JSONException e) {
			// throw new RuntimeException(e);
			// }
			// json.put(jsonObject);
		}
		try {
			long free = new File(Constants.GAIA_DATA_HOME).getFreeSpace();
			if (free < LOW_DISK_BYTES_WARNING_AMOUNT) {
				// JSONObject jsonObject = new JSONObject();
				// jsonObject.put("code", "error.low.disk.space");
				// jsonObject.put("message", "Low disk space available: " +
				// FileUtils.humanReadableUnits(free));
				// jsonObject.put("free.bytes", free);
				// json.put(jsonObject);
				Map<String, Object> m = new HashMap<String, Object>();
				m.put("code", "error.low.disk.space");
				m.put("message", "Low disk space available: " + FileUtils.humanReadableUnits(free));

				m.put("free.bytes", free);
				l.add(m);
			}
		} catch (Exception e) {
			LOG.error("Problem checking free disk space", e);
		}
		JsonSerializer serializer = new JsonSerializer();

		return new WebApplicationException(Response.status(status).entity(serializer.serialize(l)).build());
		// return Response.status(status).entity(l).build();
	}

	public static WebApplicationException statusExp(int statusCode, List<Error> errors) {
		// JSONArray json = new JSONArray();
		List<Map<String, Object>> l = new ArrayList<Map<String, Object>>();
		for (Error error : errors) {
			// JSONObject jsonObject = new JSONObject();
			// try {
			// jsonObject.put("key", error.getKey());
			// jsonObject.put("code", error.getCode());
			// jsonObject.put("message", error.getMessage());
			Map<String, Object> m = new HashMap<String, Object>();
			m.put("key", error.getKey());
			m.put("code", error.getCode());
			m.put("message", error.getMessage());
			l.add(m);
			// } catch (JSONException e) {
			// throw new RuntimeException(e);
			// }
			// json.put(jsonObject);
		}
		try {
			long free = new File(Constants.GAIA_DATA_HOME).getFreeSpace();
			if (free < LOW_DISK_BYTES_WARNING_AMOUNT) {
				// JSONObject jsonObject = new JSONObject();
				// jsonObject.put("code", "error.low.disk.space");
				// jsonObject.put("message", "Low disk space available: " +
				// FileUtils.humanReadableUnits(free));
				// jsonObject.put("free.bytes", free);
				// json.put(jsonObject);
				Map<String, Object> m = new HashMap<String, Object>();
				m.put("code", "error.low.disk.space");
				m.put("message", "Low disk space available: " + FileUtils.humanReadableUnits(free));

				m.put("free.bytes", free);
				l.add(m);
			}
		} catch (Exception e) {
			LOG.error("Problem checking free disk space", e);
		}
		JsonSerializer serializer = new JsonSerializer();

		return new WebApplicationException(Response.status(statusCode).entity(serializer.serialize(l)).build());
		// return Response.status(status).entity(l).build();
	}
	
	public static WebApplicationException statusExp(Response.Status status, String message) {
		return statusExp(status, new Error("", "error.other.exception", message));
	}
	
	public static WebApplicationException statusExp(int statusCode, String message) {
		return statusExp(statusCode, new Error("", "error.other.exception", message));
	}

	public static WebApplicationException statusExp(Exception e) {
		if ((e instanceof WebApplicationException)) {
			return (WebApplicationException) e;
		}
		return new WebApplicationException(e);
	}

	public static void toJavaMap(JSONObject o, Map<String, Object> b) throws JSONException {
		Iterator<Object> ji = o.keys();
		while (ji.hasNext()) {
			String key = (String) ji.next();
			Object val = o.get(key);
			if (val == null) {
				b.put(key, null);
			} else if (val.getClass() == JSONObject.class) {
				Map<String, Object> sub = new HashMap<String, Object>();
				toJavaMap((JSONObject) val, sub);
				b.put(key, sub);
			} else if (val.getClass() == JSONArray.class) {
				List<Object> l = new ArrayList<Object>();
				JSONArray arr = (JSONArray) val;
				for (int a = 0; a < arr.length(); a++) {
					Map<String, Object> sub = new HashMap<String, Object>();
					Object element = arr.get(a);
					if ((element instanceof JSONObject)) {
						toJavaMap((JSONObject) element, sub);
						l.add(sub);
					} else {
						l.add(element);
					}
				}
				b.put(key, l);
			} else {
				b.put(key, val);
			}
		}
	}

	public static void toJavaList(JSONArray ar, List<Object> ll) throws JSONException {
		int i = 0;
		while (i < ar.length()) {
			Object val = ar.get(i);
			if (val.getClass() == JSONObject.class) {
				Map<String, Object> sub = new HashMap<String, Object>();
				toJavaMap((JSONObject) val, sub);
				ll.add(sub);
			} else if (val.getClass() == JSONArray.class) {
				JSONArray arr = (JSONArray) val;
				for (int a = 0; a < arr.length(); a++) {
					Map<String, Object> sub = new HashMap<String, Object>();
					Object element = arr.get(a);
					if ((element instanceof JSONObject)) {
						toJavaMap((JSONObject) element, sub);
						ll.add(sub);
					} else {
						ll.add(element);
					}
				}
			} else {
				ll.add(val);
			}
			i++;
		}
	}

//	private ByteArrayOutputStream init() throws IOException {
//		ByteArrayOutputStream bytesOut = new ByteArrayOutputStream();
//		m_generator = createJsonGenerator(bytesOut);
//
//		DefaultPrettyPrinter p = new DefaultPrettyPrinter();
//		p.indentArraysWith(new DefaultPrettyPrinter.Lf2SpacesIndenter());
//		m_generator.setPrettyPrinter(p);
//
//		return bytesOut;
//	}
}
