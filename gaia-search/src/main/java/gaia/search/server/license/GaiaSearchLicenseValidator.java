package gaia.search.server.license;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import net.nicholaswilliams.java.licensing.License;
import net.nicholaswilliams.java.licensing.LicenseValidator;
import net.nicholaswilliams.java.licensing.exception.ExpiredLicenseException;
import net.nicholaswilliams.java.licensing.exception.InvalidLicenseException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GaiaSearchLicenseValidator implements LicenseValidator {
	private static final Logger LOG = LoggerFactory.getLogger(GaiaSearchLicenseValidator.class);
	
	private String productKey;

	public GaiaSearchLicenseValidator(String productKey) {
		this.productKey = productKey;
		LOG.debug("product key = " + productKey);
	}

	@Override
	public void validateLicense(License license) throws InvalidLicenseException {
		long time = Calendar.getInstance().getTimeInMillis();
		if (license.getGoodAfterDate() > time)
			throw new InvalidLicenseException("The " + this.getLicenseDescription(license) + " does not take effect until "
					+ this.getFormattedDate(license.getGoodAfterDate()) + ".");
		if (license.getGoodBeforeDate() < time)
			throw new ExpiredLicenseException("The " + this.getLicenseDescription(license) + " expired on "
					+ this.getFormattedDate(license.getGoodAfterDate()) + ".");
//		LOG.debug("(license holder, hwaddr) = (" + license.getHolder() + ", " +  getHardwareAddress() +")");
		if (!license.getHolder().equals(getHardwareAddress())) {
			throw new InvalidLicenseException("The " + this.getLicenseDescription(license) + " isn't valid.");
		}
		if (!license.getProductKey().equals(productKey)) {
			throw new InvalidLicenseException("The " + this.getLicenseDescription(license) + " has invalid product key.");
		}
	}

	public String getLicenseDescription(License license) {
		return license.getSubject() + " license for " + license.getHolder();
	}

	public String getFormattedDate(long time) {
		return new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss z (Z)").format(new Date(time));
	}

	public String getHardwareAddress() {
		InetAddress ip;
		try {
			ip = InetAddress.getLocalHost();
//			System.out.println("Current IP address : " + ip.getHostAddress());
			NetworkInterface network = NetworkInterface.getByInetAddress(ip);
			byte[] mac = network.getHardwareAddress();
//			System.out.print("Current MAC address : ");
			StringBuilder sb = new StringBuilder();
			for (int i = 0; i < mac.length; i++) {
				sb.append(String.format("%02x%s", mac[i], (i < mac.length - 1) ? ":" : ""));
			}
//			System.out.println(sb.toString());
			return sb.toString();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (SocketException e) {
			e.printStackTrace();
		}

		return null;
	}
}
