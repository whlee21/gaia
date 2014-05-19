package gaia.api;

import java.util.Map;
import org.restlet.resource.Get;
import org.restlet.resource.Put;

public interface SSLConfigResource {
	@Get("json")
	public Map<String, Object> retrieve();

	@Put("json")
	public void set(Map<String, Object> paramMap);
}
