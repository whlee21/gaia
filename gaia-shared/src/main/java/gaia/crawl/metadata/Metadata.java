package gaia.crawl.metadata;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

public class Metadata implements CreativeCommons, DublinCore, HttpHeaders, Office, Feed {
	private Map<String, String[]> metadata = null;

	public Metadata() {
		metadata = new HashMap<String, String[]>();
	}

	public Set<Map.Entry<String, String[]>> entrySet() {
		return metadata.entrySet();
	}

	public boolean contains(String key) {
		return metadata.containsKey(key);
	}

	public boolean isMultiValued(String name) {
		return (metadata.get(name) != null) && (((String[]) metadata.get(name)).length > 1);
	}

	public String[] names() {
		return (String[]) metadata.keySet().toArray(new String[metadata.keySet().size()]);
	}

	public String get(String name) {
		String[] values = (String[]) metadata.get(name);
		if (values == null) {
			return null;
		}
		return values[0];
	}

	public String[] getValues(String name) {
		return _getValues(name);
	}

	private String[] _getValues(String name) {
		String[] values = (String[]) metadata.get(name);
		if (values == null) {
			values = new String[0];
		}
		return values;
	}

	public void add(String name, String value) {
		String[] values = (String[]) metadata.get(name);
		if (values == null) {
			set(name, value);
		} else {
			String[] newValues = new String[values.length + 1];
			System.arraycopy(values, 0, newValues, 0, values.length);
			newValues[(newValues.length - 1)] = value;
			metadata.put(name, newValues);
		}
	}

	public void setAll(Properties properties) {
		Enumeration<?> names = properties.propertyNames();
		while (names.hasMoreElements()) {
			String name = (String) names.nextElement();
			metadata.put(name, new String[] { properties.getProperty(name) });
		}
	}

	public void set(String name, String value) {
		metadata.put(name, new String[] { value });
	}

	public void remove(String name) {
		metadata.remove(name);
	}

	public int size() {
		return metadata.size();
	}

	public void clear() {
		metadata.clear();
	}

	public boolean equals(Object o) {
		if (o == null)
			return false;

		Metadata other = null;
		try {
			other = (Metadata) o;
		} catch (ClassCastException cce) {
			return false;
		}

		if (other.size() != size())
			return false;

		String[] names = names();
		for (int i = 0; i < names.length; i++) {
			String[] otherValues = other._getValues(names[i]);
			String[] thisValues = _getValues(names[i]);
			if (otherValues.length != thisValues.length) {
				return false;
			}
			for (int j = 0; j < otherValues.length; j++) {
				if (!otherValues[j].equals(thisValues[j])) {
					return false;
				}
			}
		}
		return true;
	}

	public String toString() {
		StringBuffer buf = new StringBuffer();
		String[] names = names();
		for (int i = 0; i < names.length; i++) {
			String[] values = _getValues(names[i]);
			for (int j = 0; j < values.length; j++) {
				buf.append(names[i]).append("=").append(values[j]).append(" ");
			}

		}

		return buf.toString();
	}
}
