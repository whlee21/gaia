package gaia.crawl.http.protocol;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.NTCredentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.HttpClient;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.message.BasicHeader;
import org.apache.http.params.HttpConnectionParamBean;
import org.apache.http.params.HttpParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gaia.crawl.datasource.Authentication;
import gaia.crawl.http.robots.BaseRobotRules;
import gaia.crawl.http.robots.BaseRobotsParser;
import gaia.crawl.http.robots.RobotUtils;
import gaia.crawl.http.robots.RobotsCache;
import gaia.crawl.http.robots.SimpleRobotRules;
import gaia.crawl.http.robots.SimpleRobotRulesParser;
import gaia.utils.UrlUtils;

public class HttpProtocol {
	static final Logger LOG = LoggerFactory.getLogger(HttpProtocol.class);
	static final int BUFFER_SIZE = 8192;
	private ThreadSafeClientConnManager connectionManager;
	private DefaultHttpClient client;
	HttpProtocolConfig cfg;
	private RobotsCache robots;

	public HttpProtocol(HttpProtocolConfig cfg, RobotsCache robots) {
		connectionManager = new ThreadSafeClientConnManager();
		client = new DefaultHttpClient(connectionManager);
		this.cfg = cfg;
		this.robots = robots;
		configureClient();
	}

	public HttpProtocolConfig getConfig() {
		return cfg;
	}

	public ProtocolOutput getProtocolOutput(URL url, long lastModified) {
		return getProtocolOutput(url, lastModified, cfg.getMaxRedirects() > 0, cfg.getMaxSize(),
				cfg.isIgnoreRobots(), Method.GET);
	}

	public ProtocolOutput getProtocolOutput(URL url, long lastModified, Method m) {
		return getProtocolOutput(url, lastModified, cfg.getMaxRedirects() > 0, cfg.getMaxSize(),
				cfg.isIgnoreRobots(), m);
	}

	public ProtocolOutput getProtocolOutput(URL url, long lastModified, boolean followRedirects, int maxSize,
			boolean ignoreRobots, Method m) {
		ProtocolOutput output = null;
		int redirCount = 0;
		ArrayList<String> redirs = new ArrayList<String>();
		ArrayList<String> redirHosts = new ArrayList<String>();
		do {
			output = getSingleProtocolOutput(url, lastModified, maxSize, ignoreRobots, m);
			if ((output.getStatus().code != ProtocolStatus.Code.REDIRECT_PERM)
					&& (output.getStatus().code != ProtocolStatus.Code.REDIRECT_TEMP)
					&& (output.getStatus().code != ProtocolStatus.Code.REDIRECT_PROXY)) {
				break;
			}
			redirCount++;
			try {
				url = new URL(url, output.getStatus().message);
				url = UrlUtils.normalizeURL(url.toExternalForm());
				redirs.add(url.toExternalForm());
				redirHosts.add(url.getHost());
			} catch (MalformedURLException mue) {
				LOG.warn("Redirect from " + url + " to " + output.getStatus().message + " failed.", mue);
				break;
			}

		}

		while ((followRedirects) && (redirCount < cfg.getMaxRedirects()));
		HttpContentUtils.setNumRedirects(output.getMetadata(), redirCount);
		HttpContentUtils.setRedirects(redirs, redirHosts, output.getMetadata());
		LOG.debug("OUTPUT: " + output.getStatus() + " url=" + url);
		return output;
	}

	public ProtocolOutput getSingleProtocolOutput(URL url, long lastModified, int maxSize, boolean ignoreRobots, Method m) {
		if (!ignoreRobots) {
			String host = url.getHost().toLowerCase();
			BaseRobotRules rules = null;
			if (robots != null)
				rules = (BaseRobotRules) robots.get(host);
			if (rules == null) {
				URL robotsUrl = null;
				try {
					robotsUrl = new URL(url.getProtocol(), host, url.getPort(), "/robots.txt");
					BaseRobotsParser parser = new SimpleRobotRulesParser();

					rules = RobotUtils.getRobotRules(this, robots, parser, robotsUrl);
				} catch (Exception e) {
					LOG.debug("Can't retrieve /robots.txt url for url " + url + " - " + e.toString());
					rules = null;

					rules = new SimpleRobotRules(SimpleRobotRules.RobotRulesMode.ALLOW_ALL);
					rules.setCrawlDelay(cfg.getCrawlDelay());
					if (robots != null)
						robots.put(host, rules);
				}
			}
			if (rules != null) {
				if (rules.isDeferVisits()) {
					LOG.info("Deferred visit to " + url);
					return new ProtocolOutput(url, null, null, new ProtocolStatus(ProtocolStatus.Code.DEFERRED, null, 503));
				}
				if (!rules.isAllowed(url.toExternalForm())) {
					LOG.info("Robots denied visit to " + url);
					return new ProtocolOutput(url, null, null, new ProtocolStatus(ProtocolStatus.Code.ROBOTS_DENIED, null, 503));
				}
			}
		}
		try {
			HttpResponse response = new HttpResponse(this, url, lastModified, false, maxSize, m);

			int code = response.getCode();
			if (code == 200)
				return new ProtocolOutput(response, ProtocolStatus.OK);
			if ((code >= 300) && (code < 400)) {
				String location = response.getMeta("Location");

				if (location == null)
					location = response.getMeta("location");
				if (location == null)
					location = "";
				URL u = new URL(url, location);
				ProtocolStatus.Code sCode;
				switch (code) {
				case 300:
				case 301:
					sCode = ProtocolStatus.Code.REDIRECT_PERM;
					break;
				case 305:
					sCode = ProtocolStatus.Code.REDIRECT_PROXY;
					break;
				case 302:
				case 303:
				case 307:
					sCode = ProtocolStatus.Code.REDIRECT_TEMP;
					break;
				case 304:
					sCode = ProtocolStatus.Code.NOT_MODIFIED;
					break;
				case 306:
				default:
					sCode = ProtocolStatus.Code.REDIRECT_PERM;
				}

				return new ProtocolOutput(response, new ProtocolStatus(sCode, u.toExternalForm(), code));
			}
			if (code == 400)
				return new ProtocolOutput(response, new ProtocolStatus(ProtocolStatus.Code.GONE, url.toExternalForm(), code));
			if ((code == 401) || (code == 403)) {
				return new ProtocolOutput(response, new ProtocolStatus(ProtocolStatus.Code.ACCESS_DENIED, "WWW-Authenticate: "
						+ response.getMeta("WWW-Authenticate"), code));
			}

			if (code == 404)
				return new ProtocolOutput(response, new ProtocolStatus(ProtocolStatus.Code.NOT_FOUND, null, code));
			if (code == 410) {
				return new ProtocolOutput(response, new ProtocolStatus(ProtocolStatus.Code.GONE, null, code));
			}
			return new ProtocolOutput(response, new ProtocolStatus(ProtocolStatus.Code.EXCEPTION, "http code=" + code, code));
		} catch (Throwable e) {
			LOG.info("Exception fetching " + url + " - " + e.toString());
			return new ProtocolOutput(url, null, null, new ProtocolStatus(ProtocolStatus.Code.EXCEPTION, e.toString(), 500));
		}
	}

	public HttpClient getClient() {
		return client;
	}

	private void configureClient() {
		try {
			SSLSocketFactory ssl = new SSLSocketFactory(new TrustAnyStrategy());
			Scheme https = new Scheme("https", 443, ssl);
			connectionManager.getSchemeRegistry().register(https);
		} catch (Exception e) {
			LOG.warn("Could not install trust manager, SSL connections may fail: " + e.toString());
		}

		HttpParams params = client.getParams();
		HttpConnectionParamBean bean = new HttpConnectionParamBean(params);

		bean.setSoTimeout(cfg.getTimeout());

		bean.setConnectionTimeout(cfg.getTimeout());
		bean.setSocketBufferSize(8192);

		bean.setLinger(0);

		bean.setStaleCheckingEnabled(false);

		List<String> authPrefs = new ArrayList<String>(3);
		authPrefs.add("Basic");
		authPrefs.add("Digest");
		authPrefs.add("NTLM");
		authPrefs.add("negotiate");
		params.setParameter("http.auth.target-scheme-pref", authPrefs);
		params.setParameter("http.auth.proxy-scheme-pref", authPrefs);

		ArrayList<BasicHeader> headers = new ArrayList<BasicHeader>();

		headers.add(new BasicHeader("User-Agent", cfg.getUserAgent().toString()));

		if (!StringUtils.isBlank(cfg.getAcceptLanguage())) {
			headers.add(new BasicHeader("Accept-Language", cfg.getAcceptLanguage()));
		}

		if (!StringUtils.isBlank(cfg.getAcceptCharset())) {
			headers.add(new BasicHeader("Accept-Charset", cfg.getAcceptCharset()));
		}

		if (!StringUtils.isBlank(cfg.getAcceptMime())) {
			headers.add(new BasicHeader("Accept", cfg.getAcceptMime()));
		}

		if (!StringUtils.isBlank(cfg.getAcceptEncoding())) {
			headers.add(new BasicHeader("Accept-Encoding", cfg.getAcceptEncoding()));
		}
		params.setParameter("http.default-headers", headers);

		if ((cfg.getProxyHost() != null) && (cfg.getProxyHost().trim().length() > 0)) {
			HttpHost proxy = new HttpHost(cfg.getProxyHost(), cfg.getProxyPort(), "http");
			params.setParameter("http.route.default-proxy", proxy);

			if ((cfg.getProxyUsername() != null) && (cfg.getProxyUsername().trim().length() > 0)) {
				AuthScope proxyAuthScope = getAuthScope(cfg.getProxyHost(), cfg.getProxyPort(), null);

				Credentials proxyCredentials = new UsernamePasswordCredentials(cfg.getProxyUsername(),
						cfg.getProxyPassword());

				client.getCredentialsProvider().setCredentials(proxyAuthScope, proxyCredentials);
			}

		}

		List<Authentication> auths = cfg.getAuthentications();
		if ((auths == null) || (auths.size() == 0)) {
			return;
		}
		for (Authentication a : auths) {
			int port = -1;
			URL u = null;
			String host;
			try {
				u = new URL(a.getHost());
				host = u.getHost();
				port = u.getPort();
			} catch (Exception e) {
				host = a.getHost();
			}
			client.getCredentialsProvider().setCredentials(getAuthScope(host, port, a.getRealm()),
					new UsernamePasswordCredentials(a.getUsername(), a.getPassword()));

			client.getCredentialsProvider().setCredentials(getAuthScope(host, port, a.getRealm(), "ntlm"),
					new NTCredentials(a.getUsername(), a.getPassword(), host, a.getRealm()));
		}
	}

	private static AuthScope getAuthScope(String host, int port, String realm, String scheme) {
		if ((host == null) || (host.length() == 0)) {
			host = AuthScope.ANY_HOST;
		}
		if (port < 0) {
			port = -1;
		}
		if ((realm == null) || (realm.length() == 0)) {
			realm = AuthScope.ANY_REALM;
		}
		if ((scheme == null) || (scheme.length() == 0)) {
			scheme = AuthScope.ANY_SCHEME;
		}
		return new AuthScope(host, port, realm, scheme);
	}

	private static AuthScope getAuthScope(String host, int port, String realm) {
		return getAuthScope(host, port, realm, null);
	}

	public static enum Method {
		GET, HEAD, POST;
	}
}
