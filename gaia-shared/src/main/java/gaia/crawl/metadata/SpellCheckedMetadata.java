package gaia.crawl.metadata;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

public class SpellCheckedMetadata extends Metadata {
	private static final int TRESHOLD_DIVIDER = 3;
	private static final Map<String, String> NAMES_IDX = new HashMap<String, String>();

	private static String[] normalized = (String[]) NAMES_IDX.keySet().toArray(new String[NAMES_IDX.size()]);

	private static String normalize(String str) {
		StringBuffer buf = new StringBuffer();
		for (int i = 0; i < str.length(); i++) {
			char c = str.charAt(i);
			if (Character.isLetter(c)) {
				buf.append(Character.toLowerCase(c));
			}
		}
		return buf.toString();
	}

	public static String getNormalizedName(String name) {
		String searched = normalize(name);
		String value = (String) NAMES_IDX.get(searched);

		if ((value == null) && (normalized != null)) {
			int threshold = searched.length() / TRESHOLD_DIVIDER;
			for (int i = 0; (i < normalized.length) && (value == null); i++) {
				if (StringUtils.getLevenshteinDistance(searched, normalized[i]) < threshold) {
					value = (String) NAMES_IDX.get(normalized[i]);
				}
			}
		}
		return value != null ? value : name;
	}

	public void remove(String name) {
		super.remove(getNormalizedName(name));
	}

	public void add(String name, String value) {
		super.add(getNormalizedName(name), value);
	}

	public String[] getValues(String name) {
		return super.getValues(getNormalizedName(name));
	}

	public boolean contains(String name) {
		return super.contains(getNormalizedName(name));
	}

	public String get(String name) {
		return super.get(getNormalizedName(name));
	}

	public void set(String name, String value) {
		super.set(getNormalizedName(name), value);
	}

	static {
		Class<?>[] spellthese = { HttpHeaders.class };

		for (Class<?> spellCheckedNames : spellthese)
			for (Field field : spellCheckedNames.getFields()) {
				int mods = field.getModifiers();
				if ((Modifier.isFinal(mods)) && (Modifier.isPublic(mods)) && (Modifier.isStatic(mods))
						&& (field.getType().equals(String.class)))
					try {
						String val = (String) field.get(null);
						NAMES_IDX.put(normalize(val), val);
					} catch (Exception e) {
					}
			}
	}
}
