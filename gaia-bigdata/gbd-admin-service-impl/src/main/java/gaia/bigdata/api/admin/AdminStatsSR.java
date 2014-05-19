package gaia.bigdata.api.admin;

import gaia.bigdata.management.SystemInfoService;
import gaia.commons.api.Configuration;
import gaia.commons.services.BaseServerResource;

import java.net.URI;
import java.util.Map;

import org.restlet.resource.Get;

import com.google.inject.Inject;

public class AdminStatsSR extends BaseServerResource implements AdminStatsResource {
	private SystemInfoService systemInfoService;

	@Inject
	public AdminStatsSR(Configuration configuration, SystemInfoService systemInfoService) {
		super(configuration);
		this.systemInfoService = systemInfoService;
	}

	@Get
	public Map<URI, Map<String, Object>> getStatistics() {
		return systemInfoService.collectStats();
	}
}
