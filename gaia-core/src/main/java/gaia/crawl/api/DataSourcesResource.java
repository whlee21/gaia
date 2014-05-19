package gaia.crawl.api;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.restlet.resource.Get;
import org.restlet.resource.Post;

public interface DataSourcesResource {
	@Post("json")
	public Map<String, Object> add(Map<String, Object> paramMap) throws Exception;

	@Get("json")
	public List<Map<String, Object>> retrieve() throws IOException;
}
