package gaia.bigdata.api.data;

import gaia.bigdata.api.State;
import java.util.List;
import java.util.Map;
import org.restlet.resource.Get;
import org.restlet.resource.Post;

public interface SDACollectionsResource {
	@Get
	public List<State> listCollections();

	@Post
	public State add(Map<String, Object> paramMap);
}
