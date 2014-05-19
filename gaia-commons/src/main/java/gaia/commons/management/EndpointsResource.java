package gaia.commons.management;

import java.util.Collection;
import java.util.Map;
import org.restlet.resource.Get;

public interface EndpointsResource {
	@Get
	public Map<String, Collection<String>> endpoints();
}
