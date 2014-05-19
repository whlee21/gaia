package gaia.crawl.api;

import java.util.Map;
import org.restlet.resource.Delete;
import org.restlet.resource.Get;
import org.restlet.resource.Put;

public interface DataSourceJobResource {
	@Put("json")
	public void start(Map<String, Object> paramMap) throws Exception;

	@Get("json")
	public Object status() throws Exception;

	@Delete("json")
	public void stopOrAbort() throws Exception;
}
