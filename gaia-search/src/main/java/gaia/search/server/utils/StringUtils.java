package gaia.search.server.utils;

public class StringUtils {

	public static String join(CharSequence separator, Iterable<String> strings) {
		StringBuffer sb = new StringBuffer();
		boolean first = true;
		for (String s : strings) {
			if (first) {
				first = false;
			} else {
				sb.append(separator);
			}
			sb.append(s);
		}
		return sb.toString();
	}

}
