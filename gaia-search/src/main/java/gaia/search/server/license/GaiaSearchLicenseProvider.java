package gaia.search.server.license;

import net.nicholaswilliams.java.licensing.FileLicenseProvider;

public class GaiaSearchLicenseProvider extends FileLicenseProvider {
	
	public GaiaSearchLicenseProvider(String configDir) {
		super();
		setFilePrefix(configDir + "/license-");
		setFileSuffix(".lic");
	}
	
	
}
