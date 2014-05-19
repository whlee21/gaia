package gaia.utils;

import java.security.NoSuchAlgorithmException;

import javax.net.ssl.SSLContext;

import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.AbstractHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpClientSSLUtil {
	private static final Logger LOG = LoggerFactory.getLogger(HttpClientSSLUtil.class);

	public static void prepareClient(AbstractHttpClient client) {
		try {
			SSLContext sslContext = SSLContext.getDefault();
			Scheme scheme = new Scheme("https", 8443, new SSLSocketFactory(sslContext));
			client.getConnectionManager().getSchemeRegistry().register(scheme);
		} catch (NoSuchAlgorithmException e) {
			LOG.warn("Could not instrument http client to use default SSLContext", e);
		}
	}
}
