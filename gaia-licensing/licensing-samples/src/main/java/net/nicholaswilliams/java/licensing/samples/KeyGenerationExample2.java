package net.nicholaswilliams.java.licensing.samples;

import net.nicholaswilliams.java.licensing.encryption.*;
import net.nicholaswilliams.java.licensing.exception.*;
import java.security.KeyPair;
import static net.nicholaswilliams.java.licensing.encryption.RSAKeyPairGeneratorInterface.GeneratedClassDescriptor;

public class KeyGenerationExample2 {
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

		GeneratedClassDescriptor pd = new GeneratedClassDescriptor().setPackageName("my.packagename").setClassName(
				"PasswordProvider");

		try {
			generator.saveKeyPairToProviders(keyPair, rkd, ukd, "key password".toCharArray());
			generator.savePasswordToProvider("key password".toCharArray(), pd);
		} catch (AlgorithmNotSupportedException | InappropriateKeyException | InappropriateKeySpecificationException e) {
			return;
		}

		System.out.println(rkd.getJavaFileContents() + "\n\n" + ukd.getJavaFileContents() + "\n\n"
				+ pd.getJavaFileContents());
	}
}
