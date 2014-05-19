package gaia.commons.management;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import gaia.commons.api.API;
import gaia.commons.api.ResourceFinder;
import java.util.Map;

@Singleton
public class MonitoringAPI extends API {
	@Inject
	public MonitoringAPI(ResourceFinder finder) {
		super(finder);
		setDescription("Provides info and stats about this server");
	}

	protected void initAttachments() {
		attach("/info", InfoSR.class);
		attach("/statistics", StatsSR.class);
		attach("/endpoints", EndpointsSR.class);
	}

	public String getAPIRoot() {
		return "/monitor";
	}

	public String getAPIName() {
		return "MONITOR";
	}

	public Map<String, Object> getStatistics() {
		Map<String, Object> result = super.getStatistics();
		Runtime runtime = Runtime.getRuntime();
		result.put("freeMemory", Long.valueOf(runtime.freeMemory()));
		result.put("totalMemory", Long.valueOf(runtime.totalMemory()));
		result.put("maxMemory", Long.valueOf(runtime.maxMemory()));
		result.put("availableProcessors", Integer.valueOf(runtime.availableProcessors()));
		return result;
	}
}
