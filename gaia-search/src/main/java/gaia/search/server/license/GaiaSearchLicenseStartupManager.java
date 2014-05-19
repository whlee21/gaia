package gaia.search.server.license;

import gaia.search.server.configuration.Configuration;
import net.nicholaswilliams.java.licensing.LicenseManager;
import net.nicholaswilliams.java.licensing.LicenseManagerProperties;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class GaiaSearchLicenseStartupManager {

	private Configuration configuration;

	@Inject
	public GaiaSearchLicenseStartupManager(Configuration configuration) {
		this.configuration = configuration;
	}

	public void startup() {
		// ...
		LicenseManagerProperties.setPublicKeyDataProvider(new GaiaSearchPublicKeyDataProvider());
		LicenseManagerProperties.setPublicKeyPasswordProvider(new GaiaSearchPublicKeyPasswordProvider());
		LicenseManagerProperties.setLicenseProvider(new GaiaSearchLicenseProvider(configuration.getConfDir()));

		// Optional; set only if you are using a different password to encrypt
		// licenses than your public key
//		LicenseManagerProperties.setLicensePasswordProvider(new GaiaSearchLicensePasswordProvider());

		// Optional; set only if you wish to validate licenses
		LicenseManagerProperties.setLicenseValidator(new GaiaSearchLicenseValidator(configuration.getProductKey()));

		// Optional; defaults to 0, which translates to a 10-second (minimum) cache
		// time
		// LicenseManagerProperties.setCacheTimeInMinutes(5);

		LicenseManager.getInstance();
	}
}
