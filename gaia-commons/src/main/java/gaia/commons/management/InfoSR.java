package gaia.commons.management;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.restlet.resource.Get;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import gaia.commons.api.Configuration;
import gaia.commons.services.BaseServerResource;

public class InfoSR extends BaseServerResource implements InfoResource {
	private static transient Logger LOG = LoggerFactory.getLogger(InfoSR.class);
	public Map<String, Object> infoMap;

	@Inject
	public InfoSR(Configuration configuration, JMXMonitoredMap jmx) throws IOException {
		super(configuration);

		File versionFile = new File(System.getProperty("user.dir"), "VERSION.txt");
		LOG.info("Looking for Information properties in " + versionFile);
		Properties props = null;
		if (versionFile.exists()) {
			props = new Properties();
			props.load(new FileReader(versionFile));
			infoMap = new HashMap<String, Object>(props.size());
			for (Map.Entry<Object, Object> entry : props.entrySet()) {
				infoMap.put(entry.getKey().toString(), entry.getValue());
			}
		} else {
			infoMap = new HashMap<String, Object>();
			infoMap.put("status", "development");
			for (Map.Entry<Object, Object> entry : System.getProperties().entrySet()) {
				infoMap.put(entry.getKey().toString(), entry.getValue());
			}
			for (Map.Entry<String, String> entry : System.getenv().entrySet()) {
				infoMap.put(entry.getKey(), entry.getValue());
			}
		}
		for (Map.Entry<String, GaiaMBean> entry : jmx.entrySet()) {
			Map<String, Object> beanInfoMap = new HashMap<String, Object>();
			infoMap.put(((GaiaMBean) entry.getValue()).getName(), beanInfoMap);
			beanInfoMap.put("description", ((GaiaMBean) entry.getValue()).getDescription());
			if ((entry.getValue() instanceof APIMBean))
				beanInfoMap.put("endpoints", ((APIMBean) entry.getValue()).getEndpoints());
		}
	}

	@Get
	public Map<String, Object> info() {
		return infoMap;
	}
}
