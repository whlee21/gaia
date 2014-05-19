package gaia.api;

import java.util.List;
import java.util.Map;
import org.restlet.resource.Get;
import org.restlet.resource.Post;

public interface ActivitiesResource {
	@Post("json")
	public Map<String, Object> add(Map<String, Object> paramMap);

	@Get("json")
	public List<Map<String, Object>> retrieve();
}
