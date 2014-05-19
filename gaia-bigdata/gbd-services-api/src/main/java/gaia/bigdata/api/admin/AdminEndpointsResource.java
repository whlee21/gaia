package gaia.bigdata.api.admin;

import java.util.Collection;
import java.util.Map;
import org.restlet.resource.Get;

public interface AdminEndpointsResource {
	@Get
	public Map<String, Collection<String>> endpoints();
}
