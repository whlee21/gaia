package gaia.admin.collection;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.solr.common.params.SolrParams;

public class GaiaSolrParams extends SolrParams implements Cloneable {
	private Map<String, List<String>> vals = new HashMap<String, List<String>>();

	public GaiaSolrParams() {
	}

	public GaiaSolrParams(Map<String, List<String>> vals) {
	}

	public GaiaSolrParams(GaiaSolrParams params) {
		vals = cloneVals(params);
	}

	public GaiaSolrParams(SolrParams params) {
		Iterator<String> it = params.getParameterNamesIterator();
		while (it.hasNext()) {
			String name = (String) it.next();
			String[] values = params.getParams(name);
			vals.put(name, Arrays.asList(values));
		}
	}

	protected GaiaSolrParams clone() {
		return new GaiaSolrParams(this);
	}

	private Map<String, List<String>> cloneVals(GaiaSolrParams params) {
		Map<String, List<String>> vals = new HashMap<String, List<String>>();

		Set<Map.Entry<String, List<String>>> entries = params.vals.entrySet();
		for (Map.Entry<String, List<String>> entry : entries) {
			String key = (String) entry.getKey();
			List<String> val = entry.getValue();
			List<String> newVal = new ArrayList<String>();
			newVal.addAll(val);
			vals.put(key, newVal);
		}
		return vals;
	}

	public GaiaSolrParams set(String name, String[] val) {
		if ((val == null) || ((val.length == 1) && (val[0] == null)))
			vals.remove(name);
		else {
			vals.put(name, Arrays.asList(val));
		}
		return this;
	}

	public String get(String param) {
		List<String> v = vals.get(param);
		if ((v != null) && (v.size() > 0)) {
			return (String) v.get(0);
		}
		return null;
	}

	public Iterator<String> getParameterNamesIterator() {
		return vals.keySet().iterator();
	}

	public String[] getParams(String param) {
		List<String> values = vals.get(param);
		if (values == null) {
			return null;
		}
		return values.toArray(new String[0]);
	}

	public Map<String, List<String>> getVals() {
		return vals;
	}

	public void setVals(Map<String, List<String>> vals) {
		this.vals = vals;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder(128);
		try {
			boolean first = true;
			Set<Map.Entry<String, List<String>>> entrySet = vals.entrySet();
			for (Map.Entry<String, List<String>> entry : entrySet) {
				String key = (String) entry.getKey();
				List<String> valarr = entry.getValue();
				for (String val : valarr) {
					if (!first)
						sb.append('&');
					first = false;
					sb.append(key);
					sb.append('=');
					if (val != null)
						sb.append(URLEncoder.encode(val, "UTF-8"));
				}
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return sb.toString();
	}
}
