package gaia.bigdata.api.admin;

import java.util.Collection;
import java.util.Map;
import org.restlet.resource.Get;

public interface AdminServicesResource {
	@Get
	public Map<String, Collection<Object>> collectServices();
}
