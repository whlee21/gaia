package gaia.crawl.http.protocol;

import gaia.Defaults;
import gaia.Defaults.Group;
import gaia.crawl.datasource.Authentication;
import gaia.crawl.datasource.DataSource;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang.StringUtils;

public class HttpProtocolConfig {
	public static final String AGENT_STRING_KEY = "agent.string";
	public static final String AGENT_NAME_KEY = "agent.name";
	public static final String AGENT_EMAIL_KEY = "agent.email";
	public static final String AGENT_URL_KEY = "agent.url";
	public static final String AGENT_VERSION_KEY = "agent.version";
	public static final String AGENT_BROWSER_KEY = "agent.browser";
	public static final String NUM_THREADS_KEY = "num.threads";
	public static final String TIMEOUT_KEY = "timeout";
	public static final String ACCEPT_CHARSET_KEY = "accept.charset";
	public static final String ACCEPT_LANGUAGE_KEY = "accept.language";
	public static final String ACCEPT_MIME_KEY = "accept.mime";
	public static final String ACCEPT_ENCODING_KEY = "accept.encoding";
	public static final String MAX_REDIRECTS_KEY = "max.redirects";
	public static final String USE_HTTP11_KEY = "use.http11";
	public static final String CRAWL_DELAY = "crawl.delay";
	private int numThreads;
	private int timeout;
	private String proxyHost;
	private int proxyPort;
	private String proxyUsername;
	private String proxyPassword;
	private String acceptCharset;
	private String acceptLanguage;
	private String acceptMime;
	private String acceptEncoding;
	private UserAgent userAgent;
	private int maxSize = -1;
	private int maxRedirects = 3;
	private boolean useHttp11 = true;
	private boolean ignoreRobots = false;
	private long crawlDelay = 2000L;
	private DataSource ds;

	public HttpProtocolConfig(Map<String, Object> properties) {
		DataSource gds = new DataSource((String) properties.get("type"), (String) properties.get("crawler"), "reachability");

		gds.setProperties(properties);
		init(gds);
	}

	public HttpProtocolConfig(DataSource ds) {
		init(ds);
	}

	private void init(DataSource ds) {
		this.ds = ds;
		String agentString = Defaults.INSTANCE.getString(Defaults.Group.http, "agent.string");
		String agentName = Defaults.INSTANCE.getString(Defaults.Group.http, "agent.name");
		String agentEmail = Defaults.INSTANCE.getString(Defaults.Group.http, "agent.email");
		String agentUrl = Defaults.INSTANCE.getString(Defaults.Group.http, "agent.url");
		String agentBrowser = Defaults.INSTANCE.getString(Defaults.Group.http, "agent.browser");
		String agentVersion = Defaults.INSTANCE.getString(Defaults.Group.http, "agent.version");

		if (!StringUtils.isBlank(agentString))
			userAgent = new UserAgent(agentString);
		else {
			userAgent = new UserAgent(agentName, agentEmail, agentUrl, agentBrowser, agentVersion);
		}
		numThreads = Defaults.INSTANCE.getInt(Defaults.Group.http, "num.threads");
		timeout = Defaults.INSTANCE.getInt(Defaults.Group.http, "timeout");
		acceptCharset = Defaults.INSTANCE.getString(Defaults.Group.http, "accept.charset");
		acceptLanguage = Defaults.INSTANCE.getString(Defaults.Group.http, "accept.language");
		acceptMime = Defaults.INSTANCE.getString(Defaults.Group.http, "accept.mime");
		acceptEncoding = Defaults.INSTANCE.getString(Defaults.Group.http, "accept.encoding");
		maxRedirects = Defaults.INSTANCE.getInt(Defaults.Group.http, "max.redirects");
		useHttp11 = Defaults.INSTANCE.getBoolean(Defaults.Group.http, "use.http11");
		maxSize = ds.getInt("max_bytes", Defaults.INSTANCE.getInt(Defaults.Group.datasource, "max_bytes"));

		proxyHost = ((String) ds.getProperty("proxy_host",
				Defaults.INSTANCE.getString(Defaults.Group.datasource, "proxy_host", System.getProperty("http.proxyHost"))));

		Integer proxyPortSysProperty = Integer.valueOf(-1);
		if (System.getProperty("http.proxyPort") != null)
			try {
				proxyPortSysProperty = Integer.valueOf(Integer.parseInt(System.getProperty("http.proxyPort")));
			} catch (NumberFormatException e) {
			}
		proxyPort = ds.getInt("proxy_port",
				Defaults.INSTANCE.getInt(Defaults.Group.datasource, "proxy_port", proxyPortSysProperty));

		proxyUsername = ((String) ds.getProperty("proxy_username",
				Defaults.INSTANCE.getString(Defaults.Group.datasource, "proxy_username")));

		proxyPassword = ((String) ds.getProperty("proxy_password",
				Defaults.INSTANCE.getString(Defaults.Group.datasource, "proxy_password")));

		ignoreRobots = ds.getBoolean("ignore_robots",
				Defaults.INSTANCE.getBoolean(Defaults.Group.datasource, "ignore_robots"));

		crawlDelay = Defaults.INSTANCE.getLong(Defaults.Group.http, "crawl.delay");
	}

	public DataSource getDataSource() {
		return ds;
	}

	public List<Authentication> getAuthentications() {
		Object o = ds.getProperty("auth");
		if ((o != null) && ((o instanceof List))) {
			return (List) o;
		}
		return Collections.emptyList();
	}

	public long getCrawlDelay() {
		return crawlDelay;
	}

	public int getNumThreads() {
		return numThreads;
	}

	public int getTimeout() {
		return timeout;
	}

	public void setTimeout(int timeout) {
		this.timeout = timeout;
	}

	public String getProxyHost() {
		return proxyHost;
	}

	public int getProxyPort() {
		return proxyPort;
	}

	public String getAcceptCharset() {
		return acceptCharset;
	}

	public String getAcceptLanguage() {
		return acceptLanguage;
	}

	public String getAcceptMime() {
		return acceptMime;
	}

	public String getAcceptEncoding() {
		return acceptEncoding;
	}

	public UserAgent getUserAgent() {
		return userAgent;
	}

	public boolean isUseHttp11() {
		return useHttp11;
	}

	public boolean isIgnoreRobots() {
		return ignoreRobots;
	}

	public String getProxyUsername() {
		return proxyUsername;
	}

	public String getProxyPassword() {
		return proxyPassword;
	}

	public int getMaxSize() {
		return maxSize;
	}

	public int getMaxRedirects() {
		return maxRedirects;
	}
}
