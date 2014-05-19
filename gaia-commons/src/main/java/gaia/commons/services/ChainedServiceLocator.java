package gaia.commons.services;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ChainedServiceLocator extends BaseServiceLocator implements ServiceLocator {
	private static transient Logger LOG = LoggerFactory.getLogger(ChainedServiceLocator.class);
	private List<ServiceLocator> locators;

	public ChainedServiceLocator(ServiceLocator[] locators) {
		this.locators = Arrays.asList(locators);
	}

	public ChainedServiceLocator(List<ServiceLocator> locators) {
		this.locators = locators;
	}

	public URIPayload getServiceURI(String serviceType) {
		URIPayload result = null;
		for (ServiceLocator locator : locators) {
			result = locator.getServiceURI(serviceType);
			if (result != null) {
				break;
			}
		}
		return result;
	}

	public Map<String, Collection<URIPayload>> getServiceURIs() {
		Map<String, Collection<URIPayload>> result = new HashMap<String, Collection<URIPayload>>();
		for (ServiceLocator locator : locators) {
			Map<String, Collection<URIPayload>> uris = locator.getServiceURIs();

			for (Map.Entry<String, Collection<URIPayload>> entry : uris.entrySet()) {
				Collection<URIPayload> theResult = result.get(entry.getKey());
				if (theResult == null) {
					theResult = new ArrayList<URIPayload>();
					result.put(entry.getKey(), theResult);
				}
				theResult.addAll(entry.getValue());
			}
		}
		return result;
	}

	public Collection<URIPayload> getServiceURIs(String serviceType) {
		Collection<URIPayload> result = new ArrayList<URIPayload>();
		for (ServiceLocator locator : locators) {
			Collection<URIPayload> tmp = locator.getServiceURIs(serviceType);
			if ((tmp != null) && (!tmp.isEmpty())) {
				result.addAll(tmp);
			}
		}
		return result;
	}

	public void registerService(String name, URI endpoint, Map<String, String> payload) {
		for (ServiceLocator locator : locators)
			try {
				locator.registerService(name, endpoint, payload);
				return;
			} catch (UnsupportedOperationException e) {
			}
		throw new UnsupportedOperationException("No sub-locators specified");
	}

	public void unregisterService(String name, URI endpoint) {
		for (ServiceLocator locator : locators)
			try {
				locator.unregisterService(name, endpoint);
				return;
			} catch (UnsupportedOperationException e) {
			}
		throw new UnsupportedOperationException();
	}

	public void close() throws IOException {
		for (ServiceLocator locator : locators)
			try {
				locator.close();
			} catch (IOException e) {
				LOG.warn("Exception trying to close: " + locator, e);
			}
	}
}
