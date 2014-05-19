package gaia.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class SortedStringList extends ArrayList<String> {
	private static final long serialVersionUID = 1L;
	protected Map<String, Boolean> map = new HashMap<String, Boolean>();

	public boolean add(String s) {
		if (map.containsKey(s)) {
			return true;
		}

		map.put(s, Boolean.valueOf(true));

		int n = size();
		for (int i = 0; i < n; i++) {
			String si = (String) get(i);
			int compare = s.compareTo(si);
			if (compare == 0) {
				return true;
			}
			if (compare < 0) {
				add(i, s);
				return true;
			}

		}

		return super.add(s);
	}

	public void clear() {
		map.clear();
		super.clear();
	}

	public String toString() {
		String tos = "";
		int n = size();
		for (int i = 0; i < n; i++)
			tos = new StringBuilder().append(tos).append(i > 0 ? Character.valueOf(' ') : "").append((String) get(i))
					.toString();
		return tos;
	}
}
