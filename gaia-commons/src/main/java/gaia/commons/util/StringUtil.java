package gaia.commons.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.security.spec.KeySpec;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.hash.HashCode;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;

public class StringUtil {
	private static transient Logger LOG = LoggerFactory.getLogger(StringUtil.class);

	public static HashFunction md5 = Hashing.md5();

	public static Map<Object, Object> parsePropsFile(File propsFile) {
		Map<Object, Object> result = new HashMap<Object, Object>();

		if (propsFile.exists()) {
			Properties p = new Properties();
			InputStream is = null;
			try {
				is = new FileInputStream(propsFile);
				p.load(is);
				result = Collections.unmodifiableMap(p);
			} catch (IOException e) {
				LOG.error("Failure reading version file: " + propsFile, e);
			} finally {
				try {
					is.close();
				} catch (IOException e) {
				}
			}
		} else {
			LOG.warn("Could not find file with version info: " + propsFile);
		}

		return result;
	}

	public static final String md5Hash(CharSequence toHash) {
		HashCode hashCode = md5.hashString(toHash, Charset.forName("UTF-8"));
		return hashCode.toString();
	}

	public static byte[] hashPassword(String password) throws IOException {
		try {
			SecretKeyFactory f = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
			KeySpec ks = new PBEKeySpec(password.toCharArray(), "NaCl".getBytes(), 1024, 128);
			SecretKey s = f.generateSecret(ks);
			return s.getEncoded();
		} catch (Exception e) {
			throw new IOException("Cannot hash password", e);
		}
	}

	public static void main(String[] args) throws IOException {
		CLI cmdLine = new CLI();
		cmdLine.addOption("md5", "m", "MD5 hash a string");
		Map<String, List<String>> argMap = cmdLine.parseArguments(args);
		if (argMap == null) {
			System.out.println("Couldn't parse args: " + Arrays.asList(args));
			return;
		}
		if (cmdLine.hasOption("md5")) {
			String toHash = cmdLine.getOption("md5");
			String res = md5Hash(toHash);
			System.out.println("Result: " + res);
		}
	}
}
