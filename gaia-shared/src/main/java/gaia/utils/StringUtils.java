package gaia.utils;

import java.nio.charset.Charset;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.TimeZone;

public class StringUtils {
	public static final Charset UTF_8 = Charset.forName("UTF-8");

	public static final String LINE_SEPARATOR = System.getProperty("line.separator");

	public static String DEFAULT_FORMAT = "yyyy-MM-dd'T'HH:mm:ssZ";

	private static SimpleDateFormat[] acceptedFormats = { new SimpleDateFormat(DEFAULT_FORMAT),
			new SimpleDateFormat("d MMM yyyy HH:mm:ss Z"), new SimpleDateFormat("yyyyMMdd'T'HHmmssZ") };

	public static synchronized Date parseDate(String dateStr) throws ParseException {
		for (SimpleDateFormat fmt : acceptedFormats)
			try {
				return fmt.parse(dateStr);
			} catch (ParseException e) {
			}
		throw new ParseException(new StringBuilder().append("cannot parse ").append(dateStr).toString(), 0);
	}

	public static String formatDate(Date d) {
		if (d == null) {
			return null;
		}
		SimpleDateFormat sdf = new SimpleDateFormat(DEFAULT_FORMAT);
		sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
		return sdf.format(d);
	}

	public static String formatDate(long time) {
		if (time == -1L) {
			return null;
		}
		SimpleDateFormat sdf = new SimpleDateFormat(DEFAULT_FORMAT);
		sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
		return sdf.format(Long.valueOf(time));
	}

	public static String preserveCase(String left, String right) {
		StringBuilder result = new StringBuilder(left);
		int upper = Math.min(left.length(), right.length());
		char[] rightChars = right.toCharArray();
		for (int i = 0; i < upper; i++) {
			char leftChar = result.charAt(i);
			if ((Character.isUpperCase(rightChars[i])) && (!Character.isUpperCase(leftChar))) {
				result.setCharAt(i, Character.toUpperCase(leftChar));
			} else if ((!Character.isUpperCase(rightChars[i])) && (Character.isUpperCase(leftChar) == true)) {
				result.setCharAt(i, Character.toLowerCase(leftChar));
			}
		}
		return result.toString();
	}

	public static String convertCamelCaseToUnderscore(String src) {
		StringBuilder result = new StringBuilder();
		for (int i = 0; i < src.length(); i++) {
			char ch = src.charAt(i);
			if (Character.isUpperCase(ch)) {
				result.append("_");
			}
			result.append(Character.toLowerCase(ch));
		}
		return result.toString();
	}

	public static String convertUnderscoreToCamelCase(String src) {
		StringBuilder result = new StringBuilder();
		for (int i = 0; i < src.length(); i++) {
			char ch = src.charAt(i);
			if ('_' == ch) {
				i++;
				if (i < src.length())
					result.append(Character.toUpperCase(src.charAt(i)));
			} else {
				result.append(ch);
			}
		}
		return result.toString();
	}

	public static Boolean getBoolean(Object o) {
		return getBoolean(o, true);
	}

	public static Boolean getBoolean(Object o, boolean nullIsFalse) {
		if ((!nullIsFalse) && (o == null)) {
			return null;
		}
		if ((o instanceof Boolean))
			return (Boolean) o;
		if ((o instanceof String))
			return Boolean.valueOf(Boolean.parseBoolean((String) o));
		if ((o instanceof Number)) {
			return Boolean.valueOf(((Number) o).longValue() != 0L);
		}
		return Boolean.valueOf(false);
	}

	public static String getString(Object o) {
		return getString(o, "");
	}

	public static String getString(Object o, String defaultValue) {
		if (o == null)
			return defaultValue;
		return (String) o;
	}

	public static <T> List<T> getList(Class<T> z, Object o) {
		if (o == null) {
			return null;
		}
		if ((o instanceof List))
			return (List<T>) o;
		return Collections.singletonList((T) o);
	}

	public static String listToString(List<?> list) {
		StringBuilder sb = new StringBuilder();
		for (Iterator<?> iter = list.iterator(); iter.hasNext();) {
			Object o = iter.next();
			sb.append(o.toString());
			sb.append(',');
		}
		sb.setLength(sb.length() - 1);
		return sb.toString();
	}

	public static boolean isFullText(byte[] bytes) {
		int spaceCount = 0;
		int newlineCount = 0;
		int nonText = 0;
		int len = bytes.length;
		int maxCheck = 1024;
		int n = maxCheck < len ? maxCheck : len;

		if (len == 0) {
			return true;
		}

		for (int i = 0; i < n; i++) {
			byte b = bytes[i];
			if ((b == -1) || ((b >= 0) && (b < 9)))
				nonText++;
			else if (b == 32)
				spaceCount++;
			else if (b == 10) {
				newlineCount++;
			}
		}

		int shortFileLen = 300;
		if ((len < shortFileLen) && (nonText < 3)) {
			return true;
		}

		int minSpaceCount = 20;
		int minNewlineCount = 1;

		if (nonText > len / 100) {
			return false;
		}
		if ((spaceCount + newlineCount >= minSpaceCount) && (newlineCount >= minNewlineCount)) {
			return true;
		}
		return false;
	}
}
