package net.nicholaswilliams.java.licensing;

public class MockLicenseHelper {
	public static License deserialize(byte[] data) {
		return License.deserialize(data);
	}
}
