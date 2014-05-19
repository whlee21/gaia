package gaia.crawl.aperture;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

@SuppressWarnings("serial")
public class FieldValues extends HashMap<String, Set<FieldValue>> {
	public void add(FieldValue val) {
		String key = val.getSuffix();
		Set<FieldValue> vals = get(key);
		if (vals == null) {
			vals = new HashSet<FieldValue>();
			put(key, vals);
		}
		vals.add(val);
	}
}
