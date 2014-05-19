package gaia.crawl.api;

import java.util.List;
import java.util.Map;
import org.restlet.resource.Delete;
import org.restlet.resource.Get;

public interface BatchesResource {
	@Get("json")
	public List<Map<String, Object>> listBatches() throws Exception;

	@Delete("json")
	public void deleteBatch() throws Exception;
}
