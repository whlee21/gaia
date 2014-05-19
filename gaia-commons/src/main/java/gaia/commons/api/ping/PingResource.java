package gaia.commons.api.ping;

import java.util.Map;

import org.restlet.resource.Get;

public interface PingResource {
	@Get("json")
	public Map<String, String> ping();
}
