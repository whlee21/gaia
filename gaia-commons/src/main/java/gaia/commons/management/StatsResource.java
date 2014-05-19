package gaia.commons.management;

import java.util.Map;

import org.restlet.resource.Get;

public interface StatsResource {
	@Get
	public Map<String, Object> getStatistics();
}
