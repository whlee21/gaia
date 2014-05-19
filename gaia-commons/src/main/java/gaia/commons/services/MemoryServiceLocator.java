package gaia.commons.services;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class MemoryServiceLocator extends BaseServiceLocator implements ServiceLocator {
	protected Map<String, Collection<URIPayload>> services;

	public MemoryServiceLocator() {
		services = new HashMap<String, Collection<URIPayload>>();
	}

	public MemoryServiceLocator(Map<String, Collection<URIPayload>> services) {
		this.services = services;
	}

	public URIPayload getServiceURI(String serviceType) {
		Iterator<URIPayload> iterator = services.get(serviceType).iterator();

		return iterator.hasNext() ? (URIPayload) iterator.next() : null;
	}

	public Map<String, Collection<URIPayload>> getServiceURIs() {
		return services;
	}

	public Collection<URIPayload> getServiceURIs(String serviceType) {
		return services.get(serviceType);
	}

	public void registerService(String name, URI endpoint, Map<String, String> payload) {
		Collection<URIPayload> coll = services.get(name);
		if (coll == null) {
			coll = new ArrayList<URIPayload>();
			services.put(name, coll);
		}
		coll.add(new URIPayload(endpoint, payload));
	}

	public void unregisterService(String name, URI endpoint) {
		Collection<URIPayload> coll = services.get(name);
		if (coll != null)
			coll.remove(new URIPayload(endpoint));
	}

	public void close() throws IOException {
	}
}
