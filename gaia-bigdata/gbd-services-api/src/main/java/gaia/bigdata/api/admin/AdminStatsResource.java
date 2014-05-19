package gaia.bigdata.api.admin;

import java.net.URI;
import java.util.Map;
import org.restlet.resource.Get;

public interface AdminStatsResource {
	@Get
	public Map<URI, Map<String, Object>> getStatistics();
}
