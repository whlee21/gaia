package gaia.crawl.resource;

import java.util.HashMap;
import java.util.Map;

public class Resource {
	public static final String NAME = "name";
	public static final String PROPERTIES = "properties";
	String name;
	Map<String, String> properties;

	public Resource(String name, Map<String, String> properties) {
		this.name = name;
		this.properties = properties;
	}

	public String getName() {
		return name;
	}

	public Map<String, String> getProperties() {
		return properties;
	}

	public Map<String, Object> toMap() {
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("name", name);
		map.put("properties", properties);
		return map;
	}

	public static Resource fromMap(Map<String, Object> m) {
		if (m == null) {
			return null;
		}
		Resource res = new Resource((String) m.get("name"), (Map) m.get("properties"));
		return res;
	}
}
