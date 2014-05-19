package gaia.search.server.configuration;

import gaia.search.server.orm.JPATableGenerationStrategy;
import gaia.search.server.orm.PersistenceType;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Injector;
import com.google.inject.Singleton;

@Singleton
public class Configuration {

	public static final String CONFIG_FILE = "gaia-search.properties";

	public static final String WEBAPP_DIR_KEY = "webapp.dir";
	public static final String API_AUTHENTICATE_KEY = "api.authenticate";
	public static final String CONF_DIR_KEY = "conf.dir";
	public static final String APP_DIR_KEY = "app.dir";
	public static final String DATA_DIR_KEY = "data.dir";
	public static final String LOGS_DIR_KEY = "logs.dir";
	public static final String CRAWLER_DIR_KEY = "crawler.dir";
	public static final String CRAWLER_RESOURCE_DIR_KEY = "crawler.resource.dir";
	public static final String HADOOP_JOB_DIR_KEY = "hadoop.job.dir";
	public static final String CONNECTOR_URL_KEY = "connector.url";
	
	public static final String CLIENT_API_URL_KEY = "api.url";
	public static final String CLIENT_API_PREFIX_KEY = "api.prefix";
	public static final String CLIENT_API_PREFIX_DEFAULT = "/api/v1/";

	public static final String CLIENT_SECURITY_KEY = "client.security";

	public static final String API_USE_SSL = "api.ssl";
	public static final String SRVR_KSTR_DIR_KEY = "security.server.keys_dir";
	public static final String SRVR_CRT_NAME_KEY = "security.server.cert_name";
	public static final String SRVR_KEY_NAME_KEY = "security.server.key_name";
	public static final String KSTR_NAME_KEY = "security.server.keystore_name";
	public static final String SRVR_CRT_PASS_FILE_KEY = "security.server.crt_pass_file";
	public static final String SRVR_CRT_PASS_KEY = "security.server.crt_pass";
	public static final String SRVR_CRT_PASS_LEN_KEY = "security.server.crt_pass.len";
	public static final String PASSPHRASE_ENV_KEY = "security.server.passphrase_env_var";
	public static final String PASSPHRASE_KEY = "security.server.passphrase";
	public static final String RESOURCES_DIR_KEY = "resources.dir";
	public static final String METADETA_DIR_PATH = "metadata.path";
	public static final String SERVER_VERSION_FILE = "server.version.file";
	public static final String SERVER_VERSION_KEY = "version";
	
//	public static final String LICENSE_PUBLIC_KEY_FILE_KEY = "license.public.key.file";
//	public static final String LICENSE_FILE_KEY = "license.file";
	public static final String LICENSE_PRODUCT_KEY = "license.product.key";
	public static final String LICENSE_CLIENT_NAME_KEY = "license.client.name";
	
	public static final String LDAP_USE_SSL_KEY = "authentication.ldap.useSSL";
	public static final String LDAP_PRIMARY_URL_KEY = "authentication.ldap.primaryUrl";
	public static final String LDAP_SECONDARY_URL_KEY = "authentication.ldap.secondaryUrl";
	public static final String LDAP_BASE_DN_KEY = "authentication.ldap.baseDn";
	public static final String LDAP_BIND_ANONYMOUSLY_KEY = "authentication.ldap.bindAnonymously";
	public static final String LDAP_MANAGER_DN_KEY = "authentication.ldap.managerDn";
	public static final String LDAP_MANAGER_PASSWORD_KEY = "authentication.ldap.managerPassword";
	public static final String LDAP_USERNAME_ATTRIBUTE_KEY = "authentication.ldap.usernameAttribute";
	public static final String LDAP_GROUP_BASE_KEY = "authorization.ldap.groupBase";
	public static final String LDAP_GROUP_OBJECT_CLASS_KEY = "authorization.ldap.groupObjectClass";
	public static final String LDAP_GROUP_NAMING_ATTR_KEY = "authorization.ldap.groupNamingAttr";
	public static final String LDAP_GROUP_MEMEBERSHIP_ATTR_KEY = "authorization.ldap.groupMembershipAttr";
	public static final String LDAP_ADMIN_GROUP_MAPPING_RULES_KEY = "authorization.ldap.adminGroupMappingRules";
	public static final String LDAP_GROUP_SEARCH_FILTER_KEY = "authorization.ldap.groupSearchFilter";

	public static final String USER_ROLE_NAME_KEY = "authorization.userRoleName";
	public static final String ADMIN_ROLE_NAME_KEY = "authorization.adminRoleName";

	public static final String SSL_TRUSTSTORE_PATH_KEY = "ssl.trustStore.path";
	public static final String SSL_TRUSTSTORE_PASSWORD_KEY = "ssl.trustStore.password";
	public static final String SSL_TRUSTSTORE_TYPE_KEY = "ssl.trustStore.type";

	public static final String SERVER_PERSISTENCE_TYPE_KEY = "server.persistence.type";
	public static final String SERVER_JDBC_USER_NAME_KEY = "server.jdbc.user.name";
	public static final String SERVER_JDBC_USER_PASSWD_KEY = "server.jdbc.user.passwd";
	public static final String SERVER_JDBC_DRIVER_KEY = "server.jdbc.driver";
	public static final String SERVER_JDBC_URL_KEY = "server.jdbc.url";

	public static final String SERVER_CONNECTION_MAX_IDLE_TIME = "server.connection.max.idle.millis";

	public static final String CONF_DIR_DEFAULT = "/etc/gaia";
	public static final String APP_DIR_DEFAULT = "/usr/lib/gaia";
	public static final String DATA_DIR_DEFAULT = "/var/lib/gaia";
	public static final String LOGS_DIR_DEFAULT = "/var/log/gaia";
	public static final String CRAWLER_DIR_DEFAULT = "/usr/lib/gaia/crawler";
	public static final String CRAWLER_RESOURCE_DIR_DEFAULT = "/usr/lib/gaia/crawler-resources";
	public static final String HADOOP_JOB_DIR_DEFAULT = "/usr/lib/gaia/hadoop";
	public static final String CONNECTOR_URL_DEFAULT = "http://127.0.0.1:8765/connector/v1/mgr";

	public static final String CLIENT_API_URL_DEFAULT = "http://127.0.0.1:8080";

	private static final String SRVR_KSTR_DIR_DEFAULT = ".";
	public static final String SRVR_CRT_NAME_DEFAULT = "ca.crt";
	public static final String SRVR_KEY_NAME_DEFAULT = "ca.key";
	public static final String KSTR_NAME_DEFAULT = "keystore.p12";
	private static final String SRVR_CRT_PASS_FILE_DEFAULT = "pass.txt";
	private static final String SRVR_CRT_PASS_LEN_DEFAULT = "50";

	private static final String LDAP_BIND_ANONYMOUSLY_DEFAULT = "true";

	// TODO For embedded server only - should be removed later
	private static final String LDAP_PRIMARY_URL_DEFAULT = "localhost:33389";
	private static final String LDAP_BASE_DN_DEFAULT = "dc=gaia,dc=laonz,dc=net";
	private static final String LDAP_USERNAME_ATTRIBUTE_DEFAULT = "uid";
	private static final String LDAP_GROUP_BASE_DEFAULT = "ou=groups,dc=gaia,dc=laonz,dc=net";
	private static final String LDAP_GROUP_OBJECT_CLASS_DEFAULT = "group";
	private static final String LDAP_GROUP_NAMING_ATTR_DEFAULT = "cn";
	private static final String LDAP_GROUP_MEMBERSHIP_ATTR_DEFAULT = "member";
	private static final String LDAP_ADMIN_GROUP_MAPPING_RULES_DEFAULT = "Gaia Administrators";
	private static final String LDAP_GROUP_SEARCH_FILTER_DEFAULT = "";

	private static final String PASSPHRASE_ENV_DEFAULT = "GAIA_PASSPHRASE";
	private static final String RESOURCES_DIR_DEFAULT = "/var/share/gaia/resources/";
	private static final String USER_ROLE_NAME_DEFAULT = "user";
	private static final String ADMIN_ROLE_NAME_DEFAULT = "admin";

	private static final String SERVER_PERSISTENCE_TYPE_DEFAULT = "local";

	public static final String JDBC_UNIT_NAME = "gaia-search";

	// public static final String JDBC_LOCAL_URL =
	// "jdbc:postgresql://localhost/gaia";
	// public static final String JDBC_LOCAL_DRIVER = "org.postgresql.Driver";

	// public static final String JDBC_LOCAL_URL =
	// "jdbc:h2:file:${data.dir}/gaia";
	// public static final String JDBC_LOCAL_DRIVER = "org.h2.Driver";

	public static final String JDBC_LOCAL_URL_PREFIX = "jdbc:h2:file:";
	public static final String JDBC_LOCAL_URL_SUFFIX = "/gaia";
	public static final String JDBC_LOCAL_DRIVER = "org.h2.Driver";

	public static final String JDBC_IN_MEMORY_URL = "jdbc:derby:memory:myDB/gaia;create=true";
	public static final String JDBC_IN_MEMROY_DRIVER = "org.apache.derby.jdbc.EmbeddedDriver";

	private static final String SERVER_JDBC_USER_NAME_DEFAULT = "gaia-search";
	private static final String SERVER_JDBC_USER_PASSWD_DEFAULT = "gaia";

	public static final String SERVER_JDBC_GENERATE_TABLES_KEY = "server.jdbc.generateTables";

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
		configsMap = new HashMap<String, String>();
		configsMap.put(SRVR_KSTR_DIR_KEY, properties.getProperty(SRVR_KSTR_DIR_KEY, SRVR_KSTR_DIR_DEFAULT));
		configsMap.put(SRVR_KSTR_DIR_KEY, properties.getProperty(SRVR_KSTR_DIR_KEY, SRVR_KSTR_DIR_DEFAULT));
		configsMap.put(SRVR_CRT_NAME_KEY, properties.getProperty(SRVR_CRT_NAME_KEY, SRVR_CRT_NAME_DEFAULT));
		configsMap.put(SRVR_KEY_NAME_KEY, properties.getProperty(SRVR_KEY_NAME_KEY, SRVR_KEY_NAME_DEFAULT));
		configsMap.put(KSTR_NAME_KEY, properties.getProperty(KSTR_NAME_KEY, KSTR_NAME_DEFAULT));
		configsMap.put(SRVR_CRT_PASS_FILE_KEY, properties.getProperty(SRVR_CRT_PASS_FILE_KEY, SRVR_CRT_PASS_FILE_DEFAULT));
		configsMap.put(PASSPHRASE_ENV_KEY, properties.getProperty(PASSPHRASE_ENV_KEY, PASSPHRASE_ENV_DEFAULT));
		configsMap.put(PASSPHRASE_KEY, System.getenv(configsMap.get(PASSPHRASE_ENV_KEY)));
		configsMap.put(USER_ROLE_NAME_KEY, properties.getProperty(USER_ROLE_NAME_KEY, USER_ROLE_NAME_DEFAULT));
		configsMap.put(ADMIN_ROLE_NAME_KEY, properties.getProperty(ADMIN_ROLE_NAME_KEY, ADMIN_ROLE_NAME_DEFAULT));
		configsMap.put(RESOURCES_DIR_KEY, properties.getProperty(RESOURCES_DIR_KEY, RESOURCES_DIR_DEFAULT));
		configsMap.put(SRVR_CRT_PASS_LEN_KEY, properties.getProperty(SRVR_CRT_PASS_LEN_KEY, SRVR_CRT_PASS_LEN_DEFAULT));

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
		System.setProperty("hadoop.dir", getHadoopJobDir());
		System.setProperty("connector.url", getConnectorUrl());
		
//		System.setProperty("zkHost", "192.168.1.135:5001,192.168.1.135:5002,192.168.1.135:5003");
//		System.setProperty("numShards", "2");
//		
//		System.setProperty("bootstrap_conf", "true");
//		System.setProperty("bootstrap_confdir", "conf/solr/cores/collection1_0/conf");
		
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

	public String getWebAppDir() {
		LOG.info("Web App DIR : " + properties.getProperty(WEBAPP_DIR_KEY));
		return properties.getProperty(WEBAPP_DIR_KEY, "web");
	}

	public String getProductKey() {
		LOG.info("Product Key : " + properties.getProperty(LICENSE_PRODUCT_KEY));
		return properties.getProperty(LICENSE_PRODUCT_KEY);
	}
	
	public String getClientName() {
		LOG.info("Client Name : " + properties.getProperty(LICENSE_CLIENT_NAME_KEY));
		return properties.getProperty(LICENSE_CLIENT_NAME_KEY);
	}
	
	// public int getClientApiPort() {
	// return Integer.parseInt(properties.getProperty(CLIENT_API_PORT_KEY,
	// String.valueOf(CLIENT_API_PORT_DEFAULT)));
	// }

	public int getConnectionMaxIdleTime() {
		return Integer.parseInt(properties.getProperty(SERVER_CONNECTION_MAX_IDLE_TIME, String.valueOf("900000")));
	}

	/**
	 * Check to see if the API should be authenticated or not
	 * 
	 * @return false if not, true if the authentication is enabled.
	 */
	public boolean getApiAuthentication() {
		return ("true".equals(properties.getProperty(API_AUTHENTICATE_KEY, "false")));
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

	public String getHadoopJobDir() {
		LOG.info("Hadoop Job Dir : " + properties.getProperty(HADOOP_JOB_DIR_KEY));
		return properties.getProperty(HADOOP_JOB_DIR_KEY, HADOOP_JOB_DIR_DEFAULT);
	}
	
	public String getConnectorUrl() {
		LOG.info("Hadoop Job Dir : " + properties.getProperty(CONNECTOR_URL_KEY));
		return properties.getProperty(CONNECTOR_URL_KEY, CONNECTOR_URL_DEFAULT);
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

	public PersistenceType getPersistenceType() {
		String value = properties.getProperty(SERVER_PERSISTENCE_TYPE_KEY, SERVER_PERSISTENCE_TYPE_DEFAULT);
		return PersistenceType.fromString(value);
	}

	public String getDatabaseDriver() {
		return properties.getProperty(SERVER_JDBC_DRIVER_KEY);
	}

	public String getDatabaseUrl() {
		return properties.getProperty(SERVER_JDBC_URL_KEY);
	}

	public String getDatabaseUser() {
		return properties.getProperty(SERVER_JDBC_USER_NAME_KEY, SERVER_JDBC_USER_NAME_DEFAULT);
	}

	public JPATableGenerationStrategy getJPATableGenerationStrategy() {
		// return
		// JPATableGenerationStrategy.fromString(System.getProperty(SERVER_JDBC_GENERATE_TABLES_KEY));
		return JPATableGenerationStrategy.fromString(properties.getProperty(SERVER_JDBC_GENERATE_TABLES_KEY));
	}

	public String getDatabasePassword() {
		String filePath = properties.getProperty(SERVER_JDBC_USER_PASSWD_KEY);
		return readPassword(filePath, SERVER_JDBC_USER_PASSWD_DEFAULT);
	}

	public String getLocalDatabaseUrl() {
		return JDBC_LOCAL_URL_PREFIX + getDataDir() + JDBC_LOCAL_URL_SUFFIX;
	}

	public String getLocalDatabaseDriver() {
		return JDBC_LOCAL_DRIVER;
	}

	private String readPassword(String filePath, String defaultPassword) {
		if (filePath == null) {
			LOG.debug("DB password file not specified - using default");
			return defaultPassword;
		} else {
			LOG.debug("Reading password from file {}", filePath);
			String password;
			try {
				password = FileUtils.readFileToString(new File(filePath));
			} catch (IOException e) {
				throw new RuntimeException("Unable to read database password", e);
			}
			return password;
		}
	}

	public URL getGaiaSearchAddress() throws IOException {
		String addr = getClientApiUrl().toString();
		if (addr == null) {
			return null;
		}
		if (addr.endsWith("/")) {
			addr = addr.replaceAll("[/]+$", "");
		}
		return new URL(addr);
	}

	public URL getSolrAddress(boolean internal, String collection) throws IOException {
		URL coreURL = getGaiaSearchAddress();
		if (coreURL == null) {
			return null;
		}
		String path = "/solr";
		if (collection != null) {
			path = path + "/" + collection;
		}
		return convert(coreURL, path);
	}

	private URL convert(URL coreURL, String path) throws MalformedURLException {
		int port = coreURL.getPort() != -1 ? coreURL.getPort() : coreURL.getDefaultPort();
		URL gcmURL = new URL(coreURL.getProtocol(), coreURL.getHost(), port, path);

		return gcmURL;
	}

	public URI getCollectionUri(String collection) throws IOException, URISyntaxException {
		URL coreURL = getGaiaSearchAddress();
		if (coreURL == null) {
			return null;
		}
		String apiPrefix = getClientApiPrefix();
		if (!apiPrefix.startsWith("/")) {
			apiPrefix = "/" + apiPrefix;
		}
		if (apiPrefix.endsWith("/")) {
			apiPrefix = apiPrefix.replaceAll("[/]+$", "");
		}
		String path = apiPrefix + "/collections/" + collection;
		URL collectionUrl = convert(coreURL, path);
		return collectionUrl.toURI();
	}
	
	public URI generateUri(String path) throws IOException, URISyntaxException {
		URL coreURL = getGaiaSearchAddress();
		if (coreURL == null) {
			return null;
		}
		String apiPrefix = getClientApiPrefix();
		if (!apiPrefix.startsWith("/")) {
			apiPrefix = "/" + apiPrefix;
		}
		if (apiPrefix.endsWith("/")) {
			apiPrefix = apiPrefix.replaceAll("[/]+$", "");
		}
		if (!path.startsWith("/")) {
			path = "/" + path;
		}
		path = apiPrefix + path;
		URL collectionUrl = convert(coreURL, path);
		return collectionUrl.toURI();
	}

}
