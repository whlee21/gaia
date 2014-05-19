package gaia.hello.server.configuration;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Singleton;

@Singleton
public class Configuration {

	public static final String CONFIG_FILE = "gaia-hello.properties";
	public static final String WEBAPP_DIR = "webapp.dir";
	public static final String API_AUTHENTICATE = "api.authenticate";

	public static final String CLIENT_API_PORT_KEY = "client.api.port";
	private static final String SERVER_CONNECTION_MAX_IDLE_TIME = "server.connection.max.idle.millis";

	private static final int CLIENT_API_PORT_DEFAULT = 8080;

	private static final Logger LOG = LoggerFactory
			.getLogger(Configuration.class);

	private Properties properties;

	private Map<String, String> configsMap;

	public Configuration() {
		this(readConfigFile());
	}

	public Configuration(Properties properties) {
		this.properties = properties;
		configsMap = new HashMap<String, String>();
	}

	private static Properties readConfigFile() {
		Properties properties = new Properties();

		// Get property file stream from classpath
		InputStream inputStream = Configuration.class.getClassLoader()
				.getResourceAsStream(CONFIG_FILE);

		if (inputStream == null)
			throw new RuntimeException(CONFIG_FILE + " not found in classpath");

		// load the properties
		try {
			properties.load(inputStream);
		} catch (FileNotFoundException fnf) {
			LOG.info("No configuration file " + CONFIG_FILE
					+ " found in classpath.", fnf);
		} catch (IOException ie) {
			throw new IllegalArgumentException("Can't read configuration file "
					+ CONFIG_FILE, ie);
		}

		return properties;
	}

	public String getWebAppDir() {
		LOG.info("Web App DIR : " + properties.getProperty(WEBAPP_DIR));
		return properties.getProperty(WEBAPP_DIR, "web");
	}

	public int getClientApiPort() {
		return Integer.parseInt(properties.getProperty(CLIENT_API_PORT_KEY,
				String.valueOf(CLIENT_API_PORT_DEFAULT)));
	}

	public int getConnectionMaxIdleTime() {
		return Integer.parseInt(properties.getProperty(
				SERVER_CONNECTION_MAX_IDLE_TIME, String.valueOf("900000")));
	}

	/**
	 * Check to see if the API should be authenticated or not
	 * 
	 * @return false if not, true if the authentication is enabled.
	 */
	public boolean getApiAuthentication() {
		return ("true"
				.equals(properties.getProperty(API_AUTHENTICATE, "false")));
	}
}
