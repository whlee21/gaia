package gaia.commons.services;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.curator.x.discovery.ProviderStrategy;
import org.apache.curator.x.discovery.ServiceDiscovery;
import org.apache.curator.x.discovery.ServiceInstance;
import org.apache.curator.x.discovery.ServiceInstanceBuilder;
import org.apache.curator.x.discovery.ServiceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;

public class ZooKeeperServiceLocator extends BaseServiceLocator implements ServiceLocator {
	private static Logger LOG = LoggerFactory.getLogger(ZooKeeperServiceLocator.class);
	private final ServiceDiscovery<Map> discovery;
	private final ProviderStrategy<Map> strategy;
	private Map<String, ServiceProvider<Map>> providers;

	@Inject
	public ZooKeeperServiceLocator(ServiceDiscovery<Map> discovery, ProviderStrategy<Map> strategy) throws Exception {
		this.discovery = discovery;

		this.strategy = strategy;
		providers = new HashMap<String, ServiceProvider<Map>>();
	}

	public void registerService(String name, URI endpoint, Map<String, String> payload) {
		try {
			ServiceInstanceBuilder<Map> builder = ServiceInstance.builder();
			Map<String, String> realPayload = copyPayload(payload);
			realPayload.put("scheme", endpoint.getScheme());
			realPayload.put("path", endpoint.getPath());
			ServiceInstance<Map> service = builder.name(name).address(endpoint.getHost()).port(endpoint.getPort())
					.payload(realPayload).build();

			LOG.info("Registering Service: " + service);
			discovery.registerService(service);

			ServiceProvider<Map> provider = discovery.serviceProviderBuilder().serviceName(name).providerStrategy(strategy)
					.build();

			provider.start();
			providers.put(name, provider);
		} catch (Exception e) {
			throw new RuntimeException("Could not register service '" + name + "'", e);
		}
	}

	public void unregisterService(String name, URI endpoint) {
		try {
			Collection<ServiceInstance<Map>> instances = discovery.queryForInstances(name);
			List<ServiceInstance<Map>> toRemove = new ArrayList<ServiceInstance<Map>>();
			for (ServiceInstance<Map> instance : instances) {
				Map<String, String> payload = instance.getPayload();
				URI uri = new URI(payload.get("scheme").toString(), null, instance.getAddress(), instance.getPort().intValue(),
						payload.get("path").toString(), null, null);
				if (uri.equals(endpoint)) {
					toRemove.add(instance);
				}
			}
			if (!toRemove.isEmpty())
				for (ServiceInstance<Map> instance : toRemove) {
					LOG.info("Removing: " + instance.getName());
					discovery.unregisterService(instance);
				}
		} catch (Exception e) {
			LOG.error("Exception", e);
		}
	}

	public URIPayload getServiceURI(String String) {
		try {
			ServiceProvider<Map> provider = providers.get(String);
			if (provider != null) {
				ServiceInstance<Map> service = provider.getInstance();
				if (service == null) {
					return null;
				}
				Map<String, String> payload = service.getPayload();
				URI uri = new URI(payload.get("scheme").toString(), null, service.getAddress(), service.getPort().intValue(),
						payload.get("path").toString(), null, null);
				Map<String, String> thePay = copyPayload(payload);
				return new URIPayload(uri, thePay);
			}

			return null;
		} catch (Exception e) {
			throw new RuntimeException("Could not get service '" + String + "'", e);
		}
	}

	private Map<String, String> copyPayload(Map<String, String> payload) {
		Map<String, String> thePay = new HashMap<String, String>();
		for (Map.Entry<String, String> entry : payload.entrySet()) {
			if ((!entry.getKey().equals("scheme")) && (!entry.getKey().equals("path"))) {
				thePay.put(entry.getKey().toString(), entry.getValue().toString());
			}
		}
		return thePay;
	}

	public Map<String, Collection<URIPayload>> getServiceURIs() {
		Map<String, Collection<URIPayload>> result = new HashMap<String, Collection<URIPayload>>();
		for (Map.Entry<String, ServiceProvider<Map>> entry : providers.entrySet()) {
			Collection<URIPayload> coll = result.get(entry.getKey());
			if (coll == null) {
				coll = new ArrayList<URIPayload>();
				result.put(entry.getKey(), coll);
			}
			getServiceURIs((String) entry.getKey(), coll);
		}
		return result;
	}

	protected void getServiceURIs(String name, Collection<URIPayload> colls) {
		try {
			Collection<ServiceInstance<Map>> instances = discovery.queryForInstances(name);
			if ((instances != null) && (!instances.isEmpty())) {
				LOG.info("Found {} instances for {}", Integer.valueOf(instances.size()), name);
				for (ServiceInstance<Map> instance : instances) {
					Map<String, String> payload = instance.getPayload();
					URI uri = new URI(payload.get("scheme").toString(), null, instance.getAddress(), instance.getPort()
							.intValue(), payload.get("path").toString(), null, null);
					Map<String, String> thePay = copyPayload(payload);
					colls.add(new URIPayload(uri, thePay));
				}
			} else {
				LOG.info("No instances for " + name);
			}
		} catch (Exception e) {
			LOG.error("Exception", e);
		}
	}

	public Collection<URIPayload> getServiceURIs(String name) {
		Collection<URIPayload> result = new ArrayList<URIPayload>();
		getServiceURIs(name, result);

		return result;
	}

	public void close() throws IOException {
	}
}
