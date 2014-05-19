package gaia.utils;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.Iterator;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeSet;

public class UrlUtils {
	public static void validateUrl(String purpose, String url) throws Exception {
		if (url == null)
			throw new Exception(new StringBuilder()
					.append(purpose != null ? new StringBuilder().append(purpose).append(": ").toString() : "")
					.append("Null pointer; probably application error").toString());
		if (url.trim().length() < 1)
			throw new Exception(new StringBuilder()
					.append(purpose != null ? new StringBuilder().append(purpose).append(": ").toString() : "")
					.append("Empty URL/path; possible application error").toString());
		if ((!url.startsWith("http")) && (!url.startsWith("https")) && (!url.startsWith("file:"))) {
			int i = url.indexOf(':');
			if (i < 3) {
				throw new Exception(new StringBuilder()
						.append(purpose != null ? new StringBuilder().append(purpose).append(": ").toString() : "")
						.append("Missing URL scheme:").append(url).toString());
			}
			String scheme = url.substring(0, i);
			throw new Exception(new StringBuilder()
					.append(purpose != null ? new StringBuilder().append(purpose).append(": ").toString() : "")
					.append("Unsupported URL scheme: ").append(scheme).toString());
		}
	}

	public static File url2File(URL u) {
		if (!"file".equalsIgnoreCase(u.getProtocol()))
			return null;
		try {
			return new File(URLDecoder.decode(u.getFile(), "UTF-8"));
		} catch (Exception e) {
		}
		return new File(URLDecoder.decode(u.getFile()));
	}

	public static URL normalizeURL(String urlString) throws MalformedURLException {
		urlString = urlString.replace(" ", "%20");
		URL url = new URL(urlString);

		String protocol = url.getProtocol();
		String host = url.getHost();
		int port = url.getPort();
		String path = url.getPath();
		String query = url.getQuery();

		protocol = protocol.toLowerCase();
		host = host.toLowerCase();

		if (port == url.getDefaultPort()) {
			port = -1;
		}

		String file = normalizePath(path);

		if (query != null) {
			query = normalizeQuery(query);
			file = new StringBuilder().append(file).append("?").append(query).toString();
		}

		return new URL(protocol, host, port, file);
	}

	public static String normalizePath(String path) {
		String result = path;

		result = result.replace("//", "/");

		result = result.replace("/./", "/");

		result = result.replaceAll("/[^/]+/\\.\\./", "/");

		return result;
	}

	public static String normalizeQuery(String query) {
		Set<String> sortedSet = new TreeSet<String>();

		StringTokenizer tokenizer = new StringTokenizer(query, "&");
		while (tokenizer.hasMoreTokens()) {
			sortedSet.add(tokenizer.nextToken());
		}

		StringBuilder result = new StringBuilder(query.length());

		Iterator<String> iterator = sortedSet.iterator();
		while (iterator.hasNext()) {
			result.append(iterator.next());

			if (iterator.hasNext()) {
				result.append('&');
			}
		}

		return result.toString();
	}
}
