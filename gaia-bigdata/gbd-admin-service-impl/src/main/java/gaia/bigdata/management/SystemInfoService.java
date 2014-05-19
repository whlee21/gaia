package gaia.bigdata.management;

import java.net.URI;
import java.util.Collection;
import java.util.Map;

public interface SystemInfoService {
	public Map<URI, Map<String, Object>> collectServiceInfo();

	public Map<URI, Map<String, Object>> collectStats();

	public Map<String, Collection<String>> collectEndpoints();

	public Map<String, Collection<URI>> collectServices();
}
