package gaia.utils;

import gaia.Constants;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class VersionUtil {
	private static transient Log log = LogFactory.getLog(VersionUtil.class);
	private static final String VERSION_FILE = "VERSION.txt";
	private static final String SOLR_VERSION_FILE = "SOLR_VERSION.txt";

	public static Map<String, String> getGaiaWorksVersionInfo() {
		return parsePropsFile(new File(Constants.GAIA_APP_HOME, VERSION_FILE));
	}

	public static Map<String, String> getSolrVersionInfo() {
		return parsePropsFile(new File(Constants.GAIA_APP_HOME, SOLR_VERSION_FILE));
	}

	private static Map<String, String> parsePropsFile(File propsFile) {
		Map<String, String> result = new HashMap<String, String>();
		if (propsFile.exists()) {
			Properties p = new Properties();
			InputStream is = null;
			try {
				is = new FileInputStream(propsFile);
				p.load(is);
				result = new HashMap(p);
			} catch (IOException e) {
				log.error("Failure reading version file: " + propsFile, e);
			} finally {
				IOUtils.closeQuietly(is);
			}
		} else {
			log.warn("Could not find file with version info: " + propsFile);
		}

		return Collections.unmodifiableMap(result);
	}
}
