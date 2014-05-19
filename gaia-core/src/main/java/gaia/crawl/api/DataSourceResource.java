package gaia.crawl.api;

import java.util.Map;
import org.restlet.resource.Delete;
import org.restlet.resource.Get;
import org.restlet.resource.Put;

public interface DataSourceResource {
	@Delete
	public void remove() throws Exception;

	@Put("json")
	public void update(Map<String, Object> paramMap) throws Exception;

	@Get("json")
	public Map<String, Object> retrieve();
}
