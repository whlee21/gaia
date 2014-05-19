package net.nicholaswilliams.java.licensing.samples;

import net.nicholaswilliams.java.licensing.encryption.*;
import net.nicholaswilliams.java.licensing.exception.*;
import java.io.*;
import java.security.KeyPair;

public class KeyGenerationExample3 {
	public static void main(String[] arguments) {
		RSAKeyPairGenerator generator = new RSAKeyPairGenerator();

		KeyPair keyPair;
		try {
			keyPair = generator.generateKeyPair();
		} catch (RSA2048NotSupportedException e) {
			return;
		}

		try {
			generator.saveKeyPairToFiles(keyPair, "private.key", "public.key", "private password".toCharArray(),
					"public password".toCharArray());
		} catch (IOException | AlgorithmNotSupportedException | InappropriateKeyException
				| InappropriateKeySpecificationException e) {
			return;
		}
	}
}