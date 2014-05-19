package net.nicholaswilliams.java.licensing.samples;

import net.nicholaswilliams.java.licensing.encryption.*;
import net.nicholaswilliams.java.licensing.exception.*;
import java.security.KeyPair;
import static net.nicholaswilliams.java.licensing.encryption.RSAKeyPairGeneratorInterface.GeneratedClassDescriptor;

public class KeyGenerationExample4 {
	public static void main(String[] arguments) {
		RSAKeyPairGenerator generator = new RSAKeyPairGenerator();

		KeyPair keyPair;
		try {
			keyPair = generator.generateKeyPair();
		} catch (RSA2048NotSupportedException e) {
			return;
		}

		GeneratedClassDescriptor rkd = new GeneratedClassDescriptor().setPackageName("my.packagename").setClassName(
				"PrivateKeyProvider");

		GeneratedClassDescriptor ukd = new GeneratedClassDescriptor().setPackageName("my.packagename").setClassName(
				"PublicKeyProvider");

		GeneratedClassDescriptor pd1 = new GeneratedClassDescriptor().setPackageName("my.packagename").setClassName(
				"PrivatePasswordProvider");

		GeneratedClassDescriptor pd2 = new GeneratedClassDescriptor().setPackageName("my.packagename").setClassName(
				"PublicPasswordProvider");

		try {
			generator.saveKeyPairToProviders(keyPair, rkd, ukd, "private password".toCharArray(),
					"public password".toCharArray());
			generator.savePasswordToProvider("private password".toCharArray(), pd1);
			generator.savePasswordToProvider("public password".toCharArray(), pd2);
		} catch (AlgorithmNotSupportedException | InappropriateKeyException | InappropriateKeySpecificationException e) {
			return;
		}

		System.out.println(rkd.getJavaFileContents() + "\n\n" + ukd.getJavaFileContents() + "\n\n"
				+ pd1.getJavaFileContents() + "\n\n" + pd2.getJavaFileContents());
	}
}