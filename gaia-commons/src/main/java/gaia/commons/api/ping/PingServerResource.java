package gaia.commons.api.ping;

import java.util.Collections;
import java.util.Map;

import org.restlet.resource.Get;
import org.restlet.resource.ServerResource;

public class PingServerResource extends ServerResource implements PingResource {
	@Get("json")
	public Map<String, String> ping() {
		return Collections.singletonMap("ping", "alive");
	}
}
