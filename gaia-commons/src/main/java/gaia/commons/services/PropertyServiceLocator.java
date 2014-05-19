package gaia.commons.services;

import gaia.commons.api.Configuration;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;

public class PropertyServiceLocator extends BaseServiceLocator implements ServiceLocator {
	private static transient Logger LOG = LoggerFactory.getLogger(PropertyServiceLocator.class);
	private Properties properties;

	public PropertyServiceLocator(Properties props) {
		properties = props;
	}

	@Inject
	public PropertyServiceLocator(Configuration config) {
		properties = config.getProperties();
	}

	public URIPayload getServiceURI(String serviceType) {
		URIPayload result = null;
		String prop = properties.getProperty(serviceType);
		if (prop != null) {
			result = null;
			try {
				result = new URIPayload(new URI(prop));
			} catch (URISyntaxException e) {
				LOG.error("Exception", e);
				result = null;
			}
		}
		return result;
	}

	public Map<String, Collection<URIPayload>> getServiceURIs() {
		Map<String, Collection<URIPayload>> result = new HashMap<String, Collection<URIPayload>>();
		for (Object type : properties.values()) {
			String name = type.toString();
			if (name.startsWith("service.")) {
				name = name.replace("service.", "");
				URIPayload uriPayload = getServiceURI(name);
				if (uriPayload != null) {
					result.put(name, Collections.singleton(uriPayload));
				}
			}
		}
		return result;
	}

	public Collection<URIPayload> getServiceURIs(String serviceType) {
		URIPayload uri = getServiceURI(serviceType);
		return uri != null ? Collections.singleton(uri) : null;
	}

	public void registerService(String name, URI endpoint, Map<String, String> payload) {
		properties.setProperty(name, endpoint.toString());
	}

	public void unregisterService(String name, URI endpoint) {
		properties.remove(name);
	}

	public Properties getProperties() {
		return properties;
	}

	public void setProperties(Properties properties) {
		this.properties = properties;
	}

	public void close() throws IOException {
	}
}
