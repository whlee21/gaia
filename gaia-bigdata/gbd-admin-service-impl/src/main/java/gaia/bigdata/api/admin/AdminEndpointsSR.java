package gaia.bigdata.api.admin;

import gaia.bigdata.management.SystemInfoService;
import gaia.commons.api.Configuration;
import gaia.commons.services.BaseServerResource;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import com.google.inject.Inject;

public class AdminEndpointsSR extends BaseServerResource implements AdminEndpointsResource {
	private SystemInfoService systemInfoService;

	@Inject
	public AdminEndpointsSR(Configuration configuration, SystemInfoService systemInfoService) {
		super(configuration);
		this.systemInfoService = systemInfoService;
	}

	public Map<String, Collection<String>> endpoints() {
		Map<String, Collection<String>> result = new HashMap<String, Collection<String>>();
		Map<String, Collection<String>> map = systemInfoService.collectEndpoints();
		for (Map.Entry<String, Collection<String>> entry : map.entrySet()) {
			result.put(((String) entry.getKey()).toString(), entry.getValue());
		}
		return result;
	}
}
