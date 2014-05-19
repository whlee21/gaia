package gaia.search.ui.configuration;

import gaia.search.ui.orm.JPATableGenerationStrategy;
import gaia.search.ui.orm.PersistenceType;
import gaia.search.ui.security.ClientSecurityType;
import gaia.search.ui.security.authorization.LdapServerProperties;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xnap.commons.i18n.I18n;
import org.xnap.commons.i18n.I18nFactory;

import com.google.inject.Injector;
import com.google.inject.Singleton;

@Singleton
public class Configuration {

	public static final String CONFIG_FILE = "gaia-search-ui.properties";

	public static final String DEST_SERVER_NAME_KEY = "proxy.dest.host";
	public static final String DEST_SERVER_NAME_DEFAULT = "localhost";
	public static final String DEST_SERVER_PORT_KEY = "proxy.dest.port";
	public static final String DEST_SERVER_PORT_DEFAULT = "8088";
	public static final String LOCAL_SERVER_PORT_KEY = "proxy.local.port";
	public static final String LOCAL_SERVER_PORT_DEFAULT = "8088";

	public static final String API_CORE_PATH_KEY = "api.core.path";
	public static final String API_CORE_PATH_DEFAULT = "/api/v1/*";
	public static final String API_ADMIN_PATH_KEY = "api.admin.path";
	public static final String API_ADMIN_PATH_DEFAULT = "/admin/*";
	
	public static final String WEBAPP_DIR_NAME = "web";
	public static final String DATA_DIR_NAME = "data";
	public static final String CONF_DIR_NAME = "conf";
	public static final String LOGS_DIR_NAME = "logs";

	public static final String API_AUTHENTICATE_KEY = "api.authenticate";
	public static final String CLIENT_SECURITY_KEY = "security.client.type";
	
	
	//TODO: under configurations not used yet.
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

	public static final String JDBC_UNIT_NAME = "gaia-search-ui";

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
	
	private static I18n i18n = I18nFactory.getI18n(Configuration.class);
	private static final Logger LOG = LoggerFactory.getLogger(Configuration.class);
	private Properties properties;

	private Map<String, String> configsMap;

	private Injector injector;
	
	public static final String HOME_PATH = System.getProperty("home.dir");

	public Configuration() {
		this(readConfigFile());
		setSystemProperties();
	}

	public Configuration(Properties properties) {
		this.properties = properties;
		configsMap = new HashMap<String, String>();
		configsMap.put(DEST_SERVER_NAME_KEY, properties.getProperty(DEST_SERVER_NAME_KEY, DEST_SERVER_NAME_DEFAULT));
		configsMap.put(DEST_SERVER_PORT_KEY, properties.getProperty(DEST_SERVER_PORT_KEY, DEST_SERVER_PORT_DEFAULT));
		configsMap.put(LOCAL_SERVER_PORT_KEY, properties.getProperty(LOCAL_SERVER_PORT_KEY, LOCAL_SERVER_PORT_DEFAULT));
		
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
		System.setProperty("webapp.dir", getWebAppDir());
		System.setProperty("conf.dir", getConfDir());
		System.setProperty("data.dir", getDataDir());
		System.setProperty("logs.dir", getLogsDir());
	}

	private static Properties readConfigFile() {
		if (Configuration.HOME_PATH == null || "".equals(Configuration.HOME_PATH)) {
			LOG.error(i18n.tr("Gaia Search UI Server home path can't be null."));
			System.exit(-1);
		}
		LOG.info("Home dir : " + Configuration.HOME_PATH);
		
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
	
	public String getDestServerName() {
		return properties.getProperty(DEST_SERVER_NAME_KEY, DEST_SERVER_NAME_DEFAULT);
	}
	
	public int getDestServerPort() {
		return Integer.parseInt(properties.getProperty(DEST_SERVER_PORT_KEY, DEST_SERVER_PORT_DEFAULT));
	}
	
	public int getLocalServerPort() {
		return Integer.parseInt(properties.getProperty(LOCAL_SERVER_PORT_KEY, LOCAL_SERVER_PORT_DEFAULT));
	}
	
	public LdapServerProperties getLdapServerProperties() {
		LdapServerProperties ldapServerProperties = new LdapServerProperties();

		ldapServerProperties.setPrimaryUrl(properties.getProperty(LDAP_PRIMARY_URL_KEY, LDAP_PRIMARY_URL_DEFAULT));
		ldapServerProperties.setSecondaryUrl(properties.getProperty(LDAP_SECONDARY_URL_KEY));
		ldapServerProperties.setUseSsl("true".equalsIgnoreCase(properties.getProperty(LDAP_USE_SSL_KEY)));
		ldapServerProperties.setAnonymousBind("true".equalsIgnoreCase(properties.getProperty(LDAP_BIND_ANONYMOUSLY_KEY,
				LDAP_BIND_ANONYMOUSLY_DEFAULT)));
		ldapServerProperties.setManagerDn(properties.getProperty(LDAP_MANAGER_DN_KEY));
		ldapServerProperties.setManagerPassword(properties.getProperty(LDAP_MANAGER_PASSWORD_KEY));
		ldapServerProperties.setBaseDN(properties.getProperty(LDAP_BASE_DN_KEY, LDAP_BASE_DN_DEFAULT));
		ldapServerProperties.setUsernameAttribute(properties.getProperty(LDAP_USERNAME_ATTRIBUTE_KEY,
				LDAP_USERNAME_ATTRIBUTE_DEFAULT));
		ldapServerProperties.setGroupBase(properties.getProperty(LDAP_GROUP_BASE_KEY, LDAP_GROUP_BASE_DEFAULT));
		ldapServerProperties.setGroupObjectClass(properties.getProperty(LDAP_GROUP_OBJECT_CLASS_KEY,
				LDAP_GROUP_OBJECT_CLASS_DEFAULT));
		ldapServerProperties.setGroupMembershipAttr(properties.getProperty(LDAP_GROUP_MEMEBERSHIP_ATTR_KEY,
				LDAP_GROUP_MEMBERSHIP_ATTR_DEFAULT));
		ldapServerProperties.setGroupNamingAttr(properties.getProperty(LDAP_GROUP_NAMING_ATTR_KEY,
				LDAP_GROUP_NAMING_ATTR_DEFAULT));
		ldapServerProperties.setAdminGroupMappingRules(properties.getProperty(LDAP_ADMIN_GROUP_MAPPING_RULES_KEY,
				LDAP_ADMIN_GROUP_MAPPING_RULES_DEFAULT));
		ldapServerProperties.setGroupSearchFilter(properties.getProperty(LDAP_GROUP_SEARCH_FILTER_KEY,
				LDAP_GROUP_SEARCH_FILTER_DEFAULT));

		return ldapServerProperties;
	}

	/**
	 * Gets client security type
	 * 
	 * @return appropriate ClientSecurityType
	 */
	public ClientSecurityType getClientSecurityType() {
		return ClientSecurityType.fromString(properties.getProperty(CLIENT_SECURITY_KEY));
	}

	public void setClientSecurityType(ClientSecurityType type) {
		properties.setProperty(CLIENT_SECURITY_KEY, type.toString());
	}

	public String getWebAppDir() {
		LOG.info("Web App DIR : " + generatePath(WEBAPP_DIR_NAME));
		return generatePath(WEBAPP_DIR_NAME);
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
		LOG.info("Conf Dir : " + generatePath(CONF_DIR_NAME));
		return generatePath(CONF_DIR_NAME);
	}

	public String getDataDir() {
		LOG.info("Data Dir : " + generatePath(DATA_DIR_NAME));
		return generatePath(DATA_DIR_NAME);
	}

	public String getLogsDir() {
		LOG.info("Logs Dir : " + generatePath(LOGS_DIR_NAME));
		return generatePath(LOGS_DIR_NAME);
	}

	public String getConfig(String key, String defaultValue) {
		return properties.getProperty(key, defaultValue);
	}

	public String getConfig(String key) {
		return properties.getProperty(key);
	}

	public String getApiCorePath() {
		return properties.getProperty(API_CORE_PATH_KEY, API_CORE_PATH_DEFAULT);
	}

	public String getApiAdminPath() {
		return properties.getProperty(API_ADMIN_PATH_KEY, API_ADMIN_PATH_DEFAULT);
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

	private URL convert(URL coreURL, String path) throws MalformedURLException {
		int port = coreURL.getPort() != -1 ? coreURL.getPort() : coreURL.getDefaultPort();
		URL gcmURL = new URL(coreURL.getProtocol(), coreURL.getHost(), port, path);

		return gcmURL;
	}
	
	private String generatePath(String dirName){
		if (Configuration.HOME_PATH.endsWith(File.separator)) {
			return Configuration.HOME_PATH + dirName;
		} else {
			return Configuration.HOME_PATH + File.separator + dirName;
		}
	}

}
