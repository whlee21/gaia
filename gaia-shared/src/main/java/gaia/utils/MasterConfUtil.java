package gaia.utils;

import gaia.Constants;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MasterConfUtil {
	private static Logger LOG = LoggerFactory.getLogger(MasterConfUtil.class);

	// @Inject
	// private static Configuration configs;

	public static URL getGaiaSearchAddress() throws IOException {
		// String addr = (String) read().get("lwecore.address");
		 String addr = "http://127.0.0.1:8088";//configs.getClientApiUrl().toString();
		 if (addr == null) {
			 return null;
		 }
		 if (addr.endsWith("/")) {
			 addr = addr.replaceAll("[/]+$", "");
		 }
		 return new URL(addr);
//		return null;
	}

	public static URL getConnectorsAddress() throws IOException {
		String addr = (String) read().get("lweconnectors.address");
		if (addr == null) {
			return null;
		}
		if (!addr.endsWith("/")) {
			addr = addr + "/";
		}
		return new URL(addr + "connectors/v1/mgr");
	}

	private static URL convert(boolean internal, URL coreURL, String path) throws MalformedURLException {
		int port = coreURL.getPort() != -1 ? coreURL.getPort() : coreURL.getDefaultPort();
		URL gcmURL = new URL(coreURL.getProtocol(), coreURL.getHost(), port, path);

		return gcmURL;
	}

	public static Map<String, Object> read() throws IOException {
		File masterConf = new File(Constants.GAIA_CONF_HOME, "master.conf");
		Map<String, Object> conf = new HashMap<String, Object>();
		if ((masterConf.exists()) && (masterConf.canRead())) {
			Properties props = new Properties();
			BufferedInputStream is = new BufferedInputStream(new FileInputStream(masterConf));
			try {
				props.load(is);
				Set<String> names = props.stringPropertyNames();
				for (String name : names)
					conf.put(name, props.get(name));
			} finally {
				IOUtils.closeQuietly(is);
			}
		} else {
			LOG.warn("Could not find master.conf:" + masterConf);
		}

		return conf;
	}

	public static URL getSolrAddress(boolean internal, String collection) throws IOException {
		URL coreURL = getGaiaSearchAddress();
		if (coreURL == null) {
			return null;
		}
		String path = "/solr";
		if (collection != null) {
			path = path + "/" + collection;
		}
		return convert(internal, coreURL, path);
	}
}
