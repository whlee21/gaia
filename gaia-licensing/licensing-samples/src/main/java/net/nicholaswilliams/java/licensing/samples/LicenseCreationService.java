package net.nicholaswilliams.java.licensing.samples;

import net.nicholaswilliams.java.licensing.License;
import net.nicholaswilliams.java.licensing.licensor.LicenseCreator;

public class LicenseCreationService {
	long expirationDate;
	long feature2ExpDate;
	public void createLicense() {
		License license = new License.Builder()
				.withProductKey("5565-1039-AF89-GGX7-TN31-14AL")
				.withHolder("Customer Name")
				.withGoodBeforeDate(expirationDate)
				.withFeature("FEATURE1")
				.withFeature("FEATURE2", feature2ExpDate).build();

		byte[] licenseData = LicenseCreator.getInstance()
				.signAndSerializeLicense(license, "license password".toCharArray());

	}
	// encode licenseData
	//String trns = org.apache.commons.codec.binary.Base64.encodeBase64(licenseData);
	
	// decode licenseData
//	byte[] licenseData = org.apache.commons.codec.binary.Base64.decodeBase64(trns);
}
