package gaia.crawl.api;

import java.io.IOException;
import java.util.Map;
import org.restlet.resource.Delete;
import org.restlet.resource.Get;
import org.restlet.resource.Put;

public interface FieldMappingResource {
	@Get("json")
	public Object retrieve() throws IOException;

	@Delete("json")
	public void remove() throws IOException;

	@Put("json")
	public void update(Map<String, Object> paramMap) throws IOException;
}
