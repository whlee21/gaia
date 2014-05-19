package gaia.bigdata.management.sda;

import gaia.bigdata.management.SystemInfoService;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import gaia.commons.api.Configuration;
import gaia.commons.management.EndpointsResource;
import gaia.commons.management.InfoResource;
import gaia.commons.management.StatsResource;
import gaia.commons.services.BaseService;
import gaia.commons.services.ServiceLocator;
import gaia.commons.services.URIPayload;
import gaia.commons.util.RestletContainer;
import gaia.commons.util.RestletUtil;
import gaia.bigdata.services.ServiceType;

@Singleton
public class SDASystemInfoService extends BaseService implements SystemInfoService {
	private static transient Logger log = LoggerFactory.getLogger(SDASystemInfoService.class);

	@Inject
	public SDASystemInfoService(Configuration config, ServiceLocator locator) {
		super(config, locator);
	}

	public Map<URI, Map<String, Object>> collectServiceInfo() {
		Map<URI, Map<String, Object>> result = new HashMap<URI, Map<String, Object>>();
		Collection<URIPayload> serviceURIs = locator.getServiceURIs(ServiceType.MONITOR.name());
		if ((serviceURIs != null) && (!serviceURIs.isEmpty()))
			for (URIPayload uriP : serviceURIs) {
				log.info("Collecting info from {}", uriP);
				RestletContainer<InfoResource> resRc = RestletUtil.wrap(InfoResource.class, uriP.uri, "/info");
				InfoResource res = (InfoResource) resRc.getWrapped();
				try {
					Map<String, Object> info = res.info();
					if (info != null)
						result.put(uriP.uri, info);
					else
						result.put(uriP.uri, Collections.<String, Object> singletonMap("No Info", uriP.toString()));
				} catch (Throwable e) {
					log.error("Exception", e);
					result.put(uriP.uri, Collections.<String, Object> singletonMap("Error", e));
				} finally {
					RestletUtil.release(resRc);
				}
			}
		else {
			log.warn("No services to collect stats on");
		}
		return result;
	}

	public Map<URI, Map<String, Object>> collectStats() {
		Map<URI, Map<String, Object>> result = new HashMap<URI, Map<String, Object>>();
		Collection<URIPayload> serviceURIs = locator.getServiceURIs(ServiceType.MONITOR.name());
		if ((serviceURIs != null) && (!serviceURIs.isEmpty()))
			for (URIPayload uriP : serviceURIs) {
				log.info("Collecting stats from {}", uriP);
				RestletContainer<StatsResource> resRc = RestletUtil.wrap(StatsResource.class, uriP.uri, "/statistics");
				StatsResource res = (StatsResource) resRc.getWrapped();
				try {
					Map<String, Object> info = res.getStatistics();
					if (info != null)
						result.put(uriP.uri, info);
					else
						result.put(uriP.uri, Collections.<String, Object> singletonMap("No Info", uriP.toString()));
				} catch (Throwable e) {
					log.error("Exception", e);
					result.put(uriP.uri, Collections.<String, Object> singletonMap("Error", e));
				} finally {
					RestletUtil.release(resRc);
				}
			}
		else {
			log.warn("No services to collect stats on");
		}

		return result;
	}

	public Map<String, Collection<String>> collectEndpoints() {
		Map<String, Collection<String>> result = new HashMap<String, Collection<String>>();
		Collection<URIPayload> serviceURIs = locator.getServiceURIs(ServiceType.MONITOR.name());
		if ((serviceURIs != null) && (!serviceURIs.isEmpty()))
			for (URIPayload uriP : serviceURIs) {
				log.info("Collecting stats from {}", uriP);
				RestletContainer<EndpointsResource> resRc = RestletUtil.wrap(EndpointsResource.class, uriP.uri, "/endpoints");
				EndpointsResource res = (EndpointsResource) resRc.getWrapped();
				try {
					Map<String, Collection<String>> info = res.endpoints();
					if (info != null)
						for (Map.Entry<String, Collection<String>> entry : info.entrySet()) {
							Collection<String> uris = result.get(ServiceType.valueOf((String) entry.getKey()).name());
							if (uris == null) {
								uris = new HashSet<String>();
								result.put(ServiceType.valueOf((String) entry.getKey()).name(), uris);
							}
							uris.addAll(entry.getValue());
						}
					else
						log.info("No endpoints for " + uriP);
				} catch (Throwable e) {
					log.error("Exception", e);
				} finally {
					RestletUtil.release(resRc);
				}
			}
		else {
			log.warn("No services to collect endpoints");
		}
		return result;
	}

	public Map<String, Collection<URI>> collectServices() {
		Map<String, Collection<URI>> result = new HashMap<String, Collection<URI>>();
		Map<String, Collection<URIPayload>> vals = locator.getServiceURIs();
		for (Map.Entry<String, Collection<URIPayload>> entry : vals.entrySet()) {
			Collection<URI> coll = new ArrayList<URI>();
			for (URIPayload payload : entry.getValue()) {
				coll.add(payload.uri);
			}
			result.put(entry.getKey(), coll);
		}
		return result;
	}

	public String getType() {
		return ServiceType.ADMIN.name();
	}
}
