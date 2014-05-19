package gaia.crawl.http.protocol;

import gaia.crawl.metadata.Metadata;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpContentUtils {
	private static final Logger LOG = LoggerFactory.getLogger(HttpContentUtils.class);
	public static final String X_REDIRECTS_HEADER = "X-Proto-Redirects";
	public static final String X_REDIRECTS_URL = "X-Proto-Redirects-URL";
	public static final String X_REDIRECTS_HOST = "X-Proto-Redirects-Host";
	public static final String X_CONTENT_LENGTH = "X-Content-Length-Uncompressed";

	public static int getNumRedirects(Metadata meta) {
		String val = meta.get(X_REDIRECTS_HEADER);
		if (val != null) {
			try {
				return Integer.parseInt(val);
			} catch (Exception e) {
				return 0;
			}
		}
		return 0;
	}

	public static void setNumRedirects(Metadata meta, int num) {
		meta.set(X_REDIRECTS_HEADER, String.valueOf(num));
	}

	public static void setRedirects(List<String> urls, List<String> hosts, Metadata meta) {
		for (String s : urls) {
			meta.add(X_REDIRECTS_URL, s);
		}
		for (String s : hosts)
			meta.add(X_REDIRECTS_HOST, s);
	}

	public static void getRedirects(List<String> urls, List<String> hosts, Metadata meta) {
		if (urls != null) {
			String[] val = meta.getValues(X_REDIRECTS_URL);
			if ((val != null) && (val.length > 0)) {
				urls.addAll(Arrays.asList(val));
			}
		}
		if (hosts != null) {
			String[] val = meta.getValues(X_REDIRECTS_HOST);
			if ((val != null) && (val.length > 0))
				hosts.addAll(Arrays.asList(val));
		}
	}

	public static byte[] processGzipEncoded(byte[] compressed, URL url, int maxSize) throws IOException {
		LOG.trace("uncompressing....");

		byte[] content = GZIPUtils.unzipBestEffort(compressed, maxSize);

		if (content == null) {
			throw new IOException("unzipBestEffort returned null");
		}

		LOG.trace("fetched " + compressed.length + " bytes of compressed content (expanded to " + content.length
				+ " bytes) from " + url);

		return content;
	}

	public static byte[] processDeflateEncoded(byte[] compressed, URL url, int maxSize) throws IOException {
		LOG.trace("inflating....");

		byte[] content = DeflateUtils.inflateBestEffort(compressed, maxSize);

		if (content == null) {
			throw new IOException("inflateBestEffort returned null");
		}
		LOG.trace("fetched " + compressed.length + " bytes of compressed content (expanded to " + content.length
				+ " bytes) from " + url);

		return content;
	}
}
