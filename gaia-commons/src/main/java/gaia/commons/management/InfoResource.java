package gaia.commons.management;

import java.util.Map;
import org.restlet.resource.Get;

public interface InfoResource {
	@Get
	public Map<String, Object> info();
}
