package gaia.bigdata.server.configuration;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Injector;

public class Configuration {

	public static final String CONFIG_FILE = "gaia-bigdata.properties";

	public static final String CONF_DIR_KEY = "conf.dir";
	public static final String DATA_DIR_KEY = "data.dir";
	public static final String LOGS_DIR_KEY = "logs.dir";

	public static final String CONF_DIR_DEFAULT = "/etc/gaia";
		public static final String DATA_DIR_DEFAULT = "/var/lib/gaia";
	public static final String LOGS_DIR_DEFAULT = "/var/log/gaia";
	
	public static final String CLIENT_API_URL_KEY = "api.url";
	public static final String CLIENT_API_URL_DEFAULT = "http://127.0.0.1:8081";

	public static final String CLIENT_API_PREFIX_KEY = "api.prefix";
	public static final String CLIENT_API_PREFIX_DEFAULT = "/gbd/v1/";

	public static final String CLIENT_SECURITY_KEY = "client.security";

	public static final String SERVER_CONNECTION_MAX_IDLE_TIME = "server.connection.max.idle.millis";

	private static final Logger LOG = LoggerFactory.getLogger(Configuration.class);

	private Properties properties;

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

	public int getConnectionMaxIdleTime() {
		return Integer.parseInt(properties.getProperty(SERVER_CONNECTION_MAX_IDLE_TIME, String.valueOf("900000")));
	}

	public String getConfDir() {
		LOG.info("Conf Dir : " + properties.getProperty(CONF_DIR_KEY));
		return properties.getProperty(CONF_DIR_KEY, CONF_DIR_DEFAULT);
	}

	public String getDataDir() {
		LOG.info("Data Dir : " + properties.getProperty(DATA_DIR_KEY));
		return properties.getProperty(DATA_DIR_KEY, DATA_DIR_DEFAULT);
	}

	public String getLogsDir() {
		LOG.info("Logs Dir : " + properties.getProperty(LOGS_DIR_KEY));
		return properties.getProperty(LOGS_DIR_KEY, LOGS_DIR_DEFAULT);
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
}
