package gaia.bigdata.api.admin;

import gaia.bigdata.management.SystemInfoService;
import gaia.commons.api.Configuration;
import gaia.commons.services.BaseServerResource;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import org.restlet.resource.Get;

import com.google.inject.Inject;

public class AdminInfoSR extends BaseServerResource implements AdminInfoResource {
	private SystemInfoService systemInfoService;

	@Inject
	public AdminInfoSR(Configuration configuration, SystemInfoService systemInfoService) {
		super(configuration);
		this.systemInfoService = systemInfoService;
	}

	@Get
	public Map<String, Map<String, Object>> info() {
		Map<String, Map<String, Object>> result = new HashMap<String, Map<String, Object>>();
		Map<URI, Map<String, Object>> map = systemInfoService.collectServiceInfo();

		for (Map.Entry<URI, Map<String, Object>> entry : map.entrySet()) {
			result.put(((URI) entry.getKey()).toString(), entry.getValue());
		}
		return result;
	}
}
