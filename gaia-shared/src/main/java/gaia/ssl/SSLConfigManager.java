package gaia.ssl;

import gaia.Defaults;
import gaia.Settings;
import gaia.api.Error;
import gaia.utils.MasterConfUtil;

import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class SSLConfigManager {
	public static final String PROP_AUTH_REQUIRE_AUTHORIZATION = "auth_require_authorization";
	public static final String PROP_AUTH_AUTHORIZED_CLIENTS = "auth_authorized_clients";
	private static final Logger LOG = LoggerFactory.getLogger(SSLConfigManager.class);
	private final Settings settings;
	private final List<String> propNames = Arrays.asList(new String[] { PROP_AUTH_AUTHORIZED_CLIENTS,
			PROP_AUTH_REQUIRE_AUTHORIZATION });

	@Inject
	public SSLConfigManager(Settings settings) {
		this.settings = settings;

		configure();
	}

	public void configure() {
		boolean authRequireSSL = false;
		try {
			URL coreAddress = MasterConfUtil.getGaiaSearchAddress();
			LOG.info("core address from master.conf:" + coreAddress);
			authRequireSSL = "https".equals(coreAddress.getProtocol());
		} catch (Throwable t) {
			t.printStackTrace();
		}

		if (authRequireSSL) {
			LOG.info("Configuring SSL authz.");
			SSLAuthorizer.setRequireSSL(true);
			SSLAuthorizer.setRequireAuthorization(settings.getBoolean(Settings.Group.ssl,
					PROP_AUTH_REQUIRE_AUTHORIZATION, Boolean.valueOf(false)));

			List clientList = (List) settings.getObject(Settings.Group.ssl, PROP_AUTH_AUTHORIZED_CLIENTS,
					Collections.EMPTY_LIST);

			SSLAuthorizer.setAuthorizedClients((String[]) clientList.toArray(new String[clientList.size()]));
		} else {
			LOG.info("Disabling SSL authz.");
			SSLAuthorizer.setRequireSSL(false);
			SSLAuthorizer.setAuthorizedClients(null);
			SSLAuthorizer.setRequireAuthorization(false);
		}

		try {
			Object obj = Defaults.injector.getInstance(Class.forName("gaia.ssl.SolrSSLConfigManager"));
		} catch (ClassNotFoundException e) {
		} catch (Throwable e) {
			LOG.warn("Could not configure Solr to use http/https scheme", e);
		}
	}

	public List<Error> setConfig(Map<String, Object> config) {
		List<Error> errors = new SSLConfigSpec().validate(config);

		if (errors.size() == 0) {
			for (Map.Entry<String, Object> val : config.entrySet()) {
				settings.set(Settings.Group.ssl, (String) val.getKey(), val.getValue());
			}
			configure();
		}

		return errors;
	}

	public Map<String, Object> getConfig() {
		Map<String, Object> config = new HashMap<String, Object>();
		for (String propName : propNames) {
			config.put(propName, settings.getObject(Settings.Group.ssl, propName, null));
		}
		return config;
	}
}
