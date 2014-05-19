package net.nicholaswilliams.java.licensing.samples;

import net.nicholaswilliams.java.licensing.LicenseManager;
import net.nicholaswilliams.java.licensing.LicenseManagerProperties;

public class ClientSoftwareStartupManager {
	// ...
	public void startup() {
		// ...
		LicenseManagerProperties.setPublicKeyDataProvider(new MyPublicKeyProvider());
		LicenseManagerProperties.setPublicKeyPasswordProvider(new MyPublicKeyPasswordProvider());
		LicenseManagerProperties.setLicenseProvider(new MyLicenseProvider());

		// Optional; set only if you are using a different password to encrypt
		// licenses than your public key
		LicenseManagerProperties.setLicensePasswordProvider(new MyLicensePasswordProvider());

		// Optional; set only if you wish to validate licenses
		LicenseManagerProperties.setLicenseValidator(new MyLicenseValidator());

		// Optional; defaults to 0, which translates to a 10-second (minimum) cache
		// time
		LicenseManagerProperties.setCacheTimeInMinutes(5);

		LicenseManager.getInstance();
	}
	// ...
}
