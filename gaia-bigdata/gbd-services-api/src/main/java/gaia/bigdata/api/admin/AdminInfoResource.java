package gaia.bigdata.api.admin;

import java.util.Map;
import org.restlet.resource.Get;

public interface AdminInfoResource {
	@Get
	public Map<String, Map<String, Object>> info();
}
