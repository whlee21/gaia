package gaia.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class ReSTClient {
	private static boolean PRINT_JSON = new Boolean(System.getProperty("gaia.rest.printjson", "false")).booleanValue();

	public static RESTResp deleteUrl(String url) throws IOException, JSONException {
		return getOrDeleteUrl(url, "DELETE");
	}

	public static RESTResp deleteUrl(String url, Map<String, Object> m) throws IOException, JSONException {
		return getOrDeleteUrl(url, "DELETE", toReader(m));
	}

	public static Reader toReader(Map<String, Object> map) throws IOException, JSONException {
		JSONObject rep = new JSONObject(map);
		Reader data = new StringReader(rep.toString());
		return data;
	}

	public static Reader toReader(List<?> list) throws IOException, JSONException {
		JSONArray rep = new JSONArray(list);
		Reader data = new StringReader(rep.toString());
		return data;
	}

	private static RESTResp getOrDeleteUrl(String url, String method) throws IOException, JSONException {
		return doUrl(url, method, null);
	}

	private static RESTResp getOrDeleteUrl(String url, String method, Reader data) throws IOException, JSONException {
		return doUrl(url, method, data);
	}

	public static RESTResp getUrl(String url) throws IOException, JSONException {
		return getOrDeleteUrl(url, "GET");
	}

	private static RESTResp doUrl(String url, String method, Reader data) throws IOException, JSONException {
		if (PRINT_JSON) {
			System.out.println(new StringBuilder().append("\n\nrequest:").append(url).toString());
			if (data != null) {
				data = new BufferedReader(data);
				data.mark(112068);

				StringBuilder json = new StringBuilder();
				String line;
				while ((line = ((BufferedReader) data).readLine()) != null) {
					json.append(line);
				}
				JSONWrapper jsonWrapper = new JSONWrapper(json.toString());

				System.out.println(jsonWrapper.toString(2));
				data.reset();
			}
		}

		URL u = new URL(url);
		RESTResp resp = null;

		HttpURLConnection conn = (HttpURLConnection) u.openConnection();

		conn.setRequestMethod(method);

		if ((method.equalsIgnoreCase("post")) || (method.equalsIgnoreCase("put")) || (method.equalsIgnoreCase("delete"))) {
			conn.setRequestProperty("Content-type", "application/json; charset=UTF-8");

			conn.setDoOutput(true);
			conn.setUseCaches(false);
			conn.setAllowUserInteraction(false);

			if (data != null) {
				OutputStream out = conn.getOutputStream();
				Writer writer = null;
				try {
					writer = new OutputStreamWriter(out, "UTF-8");
					pipe(data, writer);
				} finally {
					if (writer != null) {
						writer.close();
					}
					if (out != null) {
						out.close();
					}
				}
			}
		}

		BufferedReader rd = null;
		StringBuilder sb = new StringBuilder();
		InputStream es = null;
		InputStream is = null;
		InputStreamReader isr = null;
		try {
			try {
				is = conn.getInputStream();
			} catch (IOException e) {
				es = conn.getErrorStream();

				if (es != null) {
					String errorText = IOUtils.toString(es);
					resp = new RESTResp(conn.getResponseCode(), 1);

					resp.setJson(errorText);

					if (resp.string != null) {
						throw new RuntimeException(errorText, e);
					}

					if (resp.jsonObject != null) {
						resp.jsonObject.put("test_url:", url);
					}

					throw new JSONObjectException(resp.jsonObject);
				}

				throw e;
			}

			resp = new RESTResp(conn.getResponseCode());
			resp.headers = conn.getHeaderFields();

			isr = new InputStreamReader(is);
			rd = new BufferedReader(isr);
			String line;
			while ((line = rd.readLine()) != null)
				sb.append(line);
		} finally {
			if (rd != null) {
				rd.close();
			}
			if (is != null) {
				is.close();
			}
			if (es != null) {
				es.close();
			}
			if (isr != null) {
				isr.close();
			}
			conn.disconnect();
		}

		String jsonString = sb.toString();
		if (PRINT_JSON) {
			System.out.println(new StringBuilder().append("\n\nresponse:").append(url).toString());
		}
		resp.setJson(jsonString);

		return resp;
	}

	public static RESTResp postUrl(String url, Reader data) throws IOException, JSONException {
		return doUrl(url, "POST", data);
	}

	public static RESTResp putUrl(String url, Reader data) throws IOException, JSONException {
		return doUrl(url, "PUT", data);
	}

	public static Map<String, JSONObject> hashFields(JSONArray json) throws JSONException {
		return listToHash(json, "name");
	}

	public static Map<String, JSONObject> listToHash(JSONArray json, String key) throws JSONException {
		Map<String, JSONObject> objs = new HashMap<String, JSONObject>();
		for (int i = 0; i < json.length(); i++) {
			JSONObject obj = (JSONObject) json.get(i);
			String keyValue = obj.get(key).toString();
			if (objs.containsKey(keyValue)) {
				throw new IllegalStateException(new StringBuilder()
						.append("json array contains multiple objects with same value for property ").append(key).append(": ")
						.append(keyValue).toString());
			}

			objs.put(keyValue, obj);
		}
		return objs;
	}

	private static void pipe(Reader reader, Writer writer) throws IOException {
		char[] buf = new char[1024];
		int read = 0;
		while ((read = reader.read(buf)) >= 0) {
			writer.write(buf, 0, read);
		}
		writer.flush();
	}

	public static RESTResp postUrl(String url, List<?> list) throws IOException, JSONException {
		return postUrl(url, toReader(list));
	}

	public static RESTResp postUrl(String url) throws IOException, JSONException {
		return postUrl(url, (Reader) null);
	}

	public static RESTResp putUrl(String url, List<String> users) throws IOException, JSONException {
		return putUrl(url, toReader(users));
	}

	public static <T> Set<T> toSet(Class<T> z, JSONArray array) throws JSONException {
		Set<T> set = new HashSet<T>(array.length());
		for (int i = 0; i < array.length(); i++) {
			set.add((T) array.get(i));
		}
		return set;
	}

	public static <T> Map<String, T> toMap(Class<String> z, JSONObject jsonObject) throws JSONException {
		Map<String, T> map = new HashMap<String, T>(jsonObject.length());
		Iterator<T> keys = jsonObject.keys();
		while (keys.hasNext()) {
			String key = (String) keys.next();

			map.put(key, (T) jsonObject.get(key));
		}
		return map;
	}

	public static RESTResp postUrl(String url, Map<String, Object> m) throws IOException, JSONException {
		return postUrl(url, toReader(m));
	}

	public static RESTResp putUrl(String url, Map<String, Object> m) throws IOException, JSONException {
		return putUrl(url, toReader(m));
	}

	public static class JSONWrapper {
		private JSONObject jsonObject;
		private JSONArray jsonArray;

		public JSONWrapper(String json) throws JSONException {
			if (json.startsWith("["))
				jsonArray = new JSONArray(json);
			else if (json.startsWith("{"))
				jsonObject = new JSONObject(json);
		}

		public String toString(int indent) throws JSONException {
			if (jsonArray != null)
				return jsonArray.toString(indent);
			if (jsonObject != null) {
				return jsonObject.toString(indent);
			}

			return "";
		}
	}

	public static class RESTResp {
		public String string;
		public int responseCode;
		public Map<String, List<String>> headers;
		public JSONObject jsonObject;
		public JSONArray jsonArray;
		public Boolean bool;
		public int numErrors;

		public String toString() {
			StringBuilder sb = new StringBuilder();
			try {
				if (jsonObject != null) {
					sb.append(jsonObject.toString(2));
				}
				if (jsonArray != null) {
					sb.append(jsonArray.toString(2));
				}
				if (bool != null) {
					sb.append(bool);
				}
				if (string != null)
					sb.append(string);
			} catch (JSONException e) {
				throw new RuntimeException(e);
			}
			return sb.toString();
		}

		public void setJson(String jsonString) throws JSONException {
			if (jsonString.length() > 0)
				if (jsonString.startsWith("[")) {
					JSONArray json = new JSONArray(jsonString);
					jsonArray = json;

					if (ReSTClient.PRINT_JSON) {
						System.out.println(jsonArray.toString(2));
					}
				} else if (jsonString.startsWith("{")) {
					JSONObject jsonObject = new JSONObject(jsonString);
					this.jsonObject = jsonObject;

					if (ReSTClient.PRINT_JSON) {
						System.out.println(jsonObject.toString(2));
					}
				} else if ((jsonString.equals("true")) || (jsonString.equals("false"))) {
					bool = new Boolean(jsonString);

					if (ReSTClient.PRINT_JSON)
						System.out.println(jsonString);
				} else {
					string = jsonString;

					if (ReSTClient.PRINT_JSON)
						System.out.println(jsonString);
				}
		}

		public RESTResp(int responseCode) {
			this.responseCode = responseCode;
		}

		public RESTResp(int responseCode, int numErrors) {
			this.responseCode = responseCode;
			this.numErrors = numErrors;
		}
	}

	static enum JsonResponse {
		LIST, MAP;
	}
}
