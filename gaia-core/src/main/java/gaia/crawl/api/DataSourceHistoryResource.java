package gaia.crawl.api;

import java.util.List;
import java.util.Map;
import org.restlet.resource.Get;

public interface DataSourceHistoryResource {
	@Get("json")
	public List<Map<String, Object>> retrieve() throws Exception;
}
