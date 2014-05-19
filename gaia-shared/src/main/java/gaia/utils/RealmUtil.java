package gaia.utils;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.AbstractHttpClient;
import org.apache.http.protocol.HttpContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gaia.Constants;

public class RealmUtil {
	private static Logger LOG = LoggerFactory.getLogger(RealmUtil.class);

	public static Properties getRealm() throws IOException {
		Properties props = new Properties();
		File realm = new File(Constants.GAIA_CONF_HOME + File.separator + "jetty" + File.separator + "search"
				+ File.separator + "etc" + File.separator + "realm.properties");

		BufferedInputStream is = new BufferedInputStream(new FileInputStream(realm));
		try {
			props.load(is);
		} finally {
			is.close();
		}
		return props;
	}

	public static void prepareClient(AbstractHttpClient client) {
		Properties realm;
		try {
			realm = getRealm();
		} catch (IOException e1) {
			return;
		}

		String username = getUser(realm);
		String password = getPassword(realm, username);

		client.addRequestInterceptor(new PreEmptiveBasicAuthenticator(username, password));
	}

	private static String getPassword(Properties realm, String username) {
		String passwordgroups = realm.getProperty(username);
		if (passwordgroups != null) {
			String[] splits = passwordgroups.split(",");
			return splits[0];
		}
		return null;
	}

	private static String getUser(Properties realm) {
		Set<Entry<Object, Object>> users = realm.entrySet();
		if (users.size() == 0) {
			throw new RuntimeException("Realm file has no entries.");
		}
		// String username = (String) ((Map.Entry<Object, Object>)
		// users.iterator().next()).getKey();
		String username = (String) users.iterator().next().getKey();
		if (users.size() > 1)
			LOG.info("Realm file has more than one entry, using the first entry: " + username);
		else {
			LOG.info("Using username: " + username);
		}

		return username;
	}

	public static Authenticator getAuthenticator() {
		Properties realm;
		try {
			realm = getRealm();
		} catch (IOException e1) {
			return null;
		}

		final String username = getUser(realm);
		final String password = getPassword(realm, username);

		return new Authenticator() {
			protected PasswordAuthentication getPasswordAuthentication() {
				return new PasswordAuthentication(username, password.toCharArray());
			}
		};
	}

	private static class PreEmptiveBasicAuthenticator implements HttpRequestInterceptor {
		private final UsernamePasswordCredentials credentials;

		public PreEmptiveBasicAuthenticator(String user, String pass) {
			credentials = new UsernamePasswordCredentials(user, pass);
		}

		public void process(HttpRequest request, HttpContext context) throws HttpException, IOException {
			request.addHeader(BasicScheme.authenticate(credentials, "US-ASCII", false));
		}
	}
}
