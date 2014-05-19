package gaia.commons.services;

import java.io.IOException;
import java.net.URI;
import java.util.Collection;
import java.util.Map;

public interface ServiceLocator {
	public void registerService(String name, URI endpoint);

	public void registerService(String name, URI endpoint, Map<String, String> payload);

	public void unregisterService(String name, URI endpoint);

	public URIPayload getServiceURI(String serviceType);

	public Map<String, Collection<URIPayload>> getServiceURIs();

	public Collection<URIPayload> getServiceURIs(String serviceType);

	public void close() throws IOException;
}
