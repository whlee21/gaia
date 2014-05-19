package gaia.bigdata.api.admin;

import gaia.bigdata.management.SystemInfoService;
import gaia.commons.api.Configuration;
import gaia.commons.services.BaseServerResource;

import java.net.URI;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.google.inject.Inject;

public class AdminServicesSR extends BaseServerResource implements AdminServicesResource {
	protected SystemInfoService sysInfoService;

	@Inject
	public AdminServicesSR(Configuration configuration, SystemInfoService systemInfoService) {
		super(configuration);
		this.sysInfoService = systemInfoService;
	}

	public Map<String, Collection<Object>> collectServices() {
		Map<String, Collection<Object>> result = new HashMap<String, Collection<Object>>();
		Map<String, Collection<URI>> map = sysInfoService.collectServices();
		for (Map.Entry<String, Collection<URI>> entry : map.entrySet()) {
			Set<Object> vals = new HashSet<Object>();
			for (URI uri : entry.getValue()) {
				vals.add(uri.toString());
			}
			result.put(entry.getKey(), vals);
		}
		return result;
	}
}
