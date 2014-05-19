package gaia.crawl.security;

import java.util.HashMap;
import java.util.Map;

public class SecurityFilter {
	private String filter;
	private Map<String, String> nestedClauses = new HashMap<String, String>();

	public SecurityFilter(String filter) {
		this.filter = filter;
	}

	public SecurityFilter(String filter, Map<String, String> nestedClauses) {
		this.filter = filter;
		this.nestedClauses = nestedClauses;
	}

	public String getFilter() {
		return filter;
	}

	public void setFilter(String filter) {
		this.filter = filter;
	}

	public Map<String, String> getNestedClauses() {
		return nestedClauses;
	}

	public void setNestedClauses(Map<String, String> nestedClauses) {
		this.nestedClauses = nestedClauses;
	}

	public String toString() {
		return "SecurityFilter{filter=" + filter + ", " + "nestedClauses=" + nestedClauses.toString() + '}';
	}

	public Map<String, Object> toMap() {
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("filter", filter);
		if (nestedClauses != null) {
			map.put("nestedClauses", nestedClauses);
		}
		return map;
	}

	public static SecurityFilter fromMap(Map<String, Object> map) {
		Object obj = map.get("filter");
		if ((obj == null) || (!(obj instanceof String))) {
			return null;
		}
		String filter = (String) obj;

		SecurityFilter res = new SecurityFilter(filter);

		obj = map.get("nestedClauses");
		if ((obj != null) && ((obj instanceof Map))) {
			res.setNestedClauses((Map) obj);
		}
		return res;
	}
}
