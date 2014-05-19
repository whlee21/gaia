package gaia.crawl.connector.configuration;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Injector;

public class Configuration {

	public static final String CONFIG_FILE = "gaia-connector.properties";

	public static final String CONF_DIR_KEY = "conf.dir";
	public static final String APP_DIR_KEY = "app.dir";
	public static final String DATA_DIR_KEY = "data.dir";
	public static final String LOGS_DIR_KEY = "logs.dir";
	public static final String CRAWLER_DIR_KEY = "crawler.dir";
	public static final String CRAWLER_RESOURCE_DIR_KEY = "crawler.resource.dir";
	
	public static final String CONF_DIR_DEFAULT = "/etc/gaia";
	public static final String APP_DIR_DEFAULT = "/usr/lib/gaia";
	public static final String DATA_DIR_DEFAULT = "/var/lib/gaia";
	public static final String LOGS_DIR_DEFAULT = "/var/log/gaia";
	public static final String CRAWLER_DIR_DEFAULT = "/usr/lib/gaia/crawler";
	public static final String CRAWLER_RESOURCE_DIR_DEFAULT = "/usr/lib/gaia/crawler-resources";
	
	public static final String CLIENT_API_URL_KEY = "api.url";
	public static final String CLIENT_API_URL_DEFAULT = "http://127.0.0.1:8881";
	
	public static final String SOLR_URL_KEY = "solr.url";
	public static final String SOLR_URL_DEFAULT = "http://127.0.0.1:8088/solr";
	
	public static final String CLIENT_API_PREFIX_KEY = "api.prefix";
	public static final String CLIENT_API_PREFIX_DEFAULT = "/connector/v1/";

	public static final String CLIENT_SECURITY_KEY = "client.security";
	
	public static final String SERVER_CONNECTION_MAX_IDLE_TIME = "server.connection.max.idle.millis";

	private static final Logger LOG = LoggerFactory.getLogger(Configuration.class);

	private Properties properties;

	private Map<String, String> configsMap;

	private Injector injector;

	public Configuration() {
		this(readConfigFile());
		setSystemProperties();
	}

	public Configuration(Properties properties) {
		this.properties = properties;
	}

	public void setInjector(Injector injector) {
		this.injector = injector;
	}

	public Injector getInjector() {
		return injector;
	}

	private void setSystemProperties() {
		System.setProperty("conf.dir", getConfDir());
		System.setProperty("data.dir", getDataDir());
		System.setProperty("logs.dir", getLogsDir());
		System.setProperty("app.dir", getAppDir());
		System.setProperty("crawler.dir", getCrawlerDir());
		System.setProperty("crawler.resource.dir", getCrawlerResourceDir());
	}

	private static Properties readConfigFile() {
		Properties properties = new Properties();

		// Get property file stream from classpath
		InputStream inputStream = Configuration.class.getClassLoader().getResourceAsStream(CONFIG_FILE);

		if (inputStream == null)
			throw new RuntimeException(CONFIG_FILE + " not found in classpath");

		// load the properties
		try {
			properties.load(inputStream);
		} catch (FileNotFoundException fnf) {
			LOG.info("No configuration file " + CONFIG_FILE + " found in classpath.", fnf);
		} catch (IOException ie) {
			throw new IllegalArgumentException("Can't read configuration file " + CONFIG_FILE, ie);
		}

		return properties;
	}

	public Map<String, String> getConfigsMap() {
		return configsMap;
	}

	public int getConnectionMaxIdleTime() {
		return Integer.parseInt(properties.getProperty(SERVER_CONNECTION_MAX_IDLE_TIME, String.valueOf("900000")));
	}

	public String getConfDir() {
		LOG.info("Conf Dir : " + properties.getProperty(CONF_DIR_KEY));
		return properties.getProperty(CONF_DIR_KEY, CONF_DIR_DEFAULT);
	}

	public String getAppDir() {
		LOG.info("App Dir : " + properties.getProperty(APP_DIR_KEY));
		return properties.getProperty(APP_DIR_KEY, APP_DIR_DEFAULT);
	}

	public String getDataDir() {
		LOG.info("Data Dir : " + properties.getProperty(DATA_DIR_KEY));
		return properties.getProperty(DATA_DIR_KEY, DATA_DIR_DEFAULT);
	}

	public String getLogsDir() {
		LOG.info("Logs Dir : " + properties.getProperty(LOGS_DIR_KEY));
		return properties.getProperty(LOGS_DIR_KEY, LOGS_DIR_DEFAULT);
	}

	public String getCrawlerDir() {
		LOG.info("Crawler Dir : " + properties.getProperty(CRAWLER_DIR_KEY));
		return properties.getProperty(CRAWLER_DIR_KEY, CRAWLER_DIR_DEFAULT);
	}

	public String getCrawlerResourceDir() {
		LOG.info("Crawler Resource Dir : " + properties.getProperty(CRAWLER_RESOURCE_DIR_KEY));
		return properties.getProperty(CRAWLER_RESOURCE_DIR_KEY, CRAWLER_RESOURCE_DIR_DEFAULT);
	}

	public String getConfig(String key, String defaultValue) {
		return properties.getProperty(key, defaultValue);
	}

	public String getConfig(String key) {
		return properties.getProperty(key);
	}

	public URL getClientApiUrl() {
		URL clientApiUrl = null;
		try {
			clientApiUrl = new URL(properties.getProperty(CLIENT_API_URL_KEY, CLIENT_API_URL_DEFAULT));
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return clientApiUrl;
	}

	public int getClientApiPort() {
		URL clientApiUrl = getClientApiUrl();
		return clientApiUrl.getPort();
	}

	public String getClientApiPrefix() {
		return properties.getProperty(CLIENT_API_PREFIX_KEY, CLIENT_API_PREFIX_DEFAULT);
	}
	
	public URL getSolrAddress(boolean internal, String collection) throws IOException {
		String addr = properties.getProperty(SOLR_URL_KEY, SOLR_URL_DEFAULT);
		if (addr == null) {
			return null;
		}
		if (addr.endsWith("/")) {
			addr = addr.replaceAll("[/]+$", "");
		}
		URL coreURL = new URL(addr);
		String path = "/solr";
		if (collection != null) {
			path = path + collection;
		}
		return convert(coreURL, path);
	}

	private URL convert(URL coreURL, String path) throws MalformedURLException {
		int port = coreURL.getPort() != -1 ? coreURL.getPort() : coreURL.getDefaultPort();
		URL gcmURL = new URL(coreURL.getProtocol(), coreURL.getHost(), port, path);

		return gcmURL;
	}
}
