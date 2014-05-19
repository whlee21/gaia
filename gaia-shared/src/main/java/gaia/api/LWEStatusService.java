package gaia.api;

import org.restlet.service.StatusService;

public class LWEStatusService extends StatusService {
//	private static transient Log log = LogFactory.getLog(LWEStatusService.class);
//	public static final String HTTP_STATUS_NAME = "http_status_name";
//	public static final String HTTP_STATUS_CODE = "http_status_code";
//	public static final String HTTP_STATUS_CAUSE = "http_status_cause";
//	public static final String HTTP_STATUS_STACK = "http_status_stack";
//	public static Set<Pattern> ignorePatterns;
//
//	public Status getStatus(Throwable t, Request req, Response resp) {
//		String stackTrace = SolrException.toStr(t);
//		String ignore = doIgnore(stackTrace);
//		if (ignore == null)
//			log.error("", t);
//		else {
//			log.info(ignore);
//		}
//
//		return super.getStatus(t, req, resp);
//	}
//
//	public Representation getRepresentation(Status status, Request request, Response response) {
//		JSONObject json = new JSONObject();
//
//		JSONArray errors = null;
//		try {
//			String jsonString = status.getDescription();
//			if (jsonString.trim().length() > 0) {
//				try {
//					errors = new JSONArray(jsonString);
//					json.put("errors", errors);
//				} catch (JSONException e) {
//					errors = new JSONArray();
//					JSONObject ob = new JSONObject();
//					ob.put("message", jsonString);
//					ob.put("key", "");
//					errors.put(ob);
//					json.put("errors", errors);
//				}
//			} else {
//				json.put("errors", Collections.EMPTY_LIST);
//			}
//
//			json.put(HTTP_STATUS_CODE, status.getCode());
//			json.put(HTTP_STATUS_NAME, status.getName());
//			if (status.getThrowable() != null) {
//				json.put(HTTP_STATUS_CAUSE, status.getThrowable().toString());
//				StackTraceElement[] stack = status.getThrowable().getStackTrace();
//				JSONArray arr = new JSONArray();
//				for (StackTraceElement el : stack) {
//					arr.put(el.toString());
//				}
//				json.put(HTTP_STATUS_STACK, arr);
//			}
//		} catch (JSONException e) {
//			throw new RuntimeException(status.getDescription(), e);
//		}
//
//		JsonRepresentation rep = new JsonRepresentation(json);
//
//		return rep;
//	}
//
//	public static void addIgnore(String regex) {
//		ignorePatterns.add(Pattern.compile(regex));
//	}
//
//	public static String doIgnore(String m) {
//		if ((ignorePatterns == null) || (m == null))
//			return null;
//		for (Pattern pattern : ignorePatterns) {
//			Matcher matcher = pattern.matcher(m);
//			if (matcher.find()) {
//				return "Ignoring exception matching " + pattern;
//			}
//		}
//		return null;
//	}
}
