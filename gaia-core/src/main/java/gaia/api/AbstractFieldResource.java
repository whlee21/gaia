package gaia.api;

import java.util.Map;

import org.restlet.resource.Get;

public interface AbstractFieldResource {
	@Get("json")
	public Map<String, Object> retrieve();
}
