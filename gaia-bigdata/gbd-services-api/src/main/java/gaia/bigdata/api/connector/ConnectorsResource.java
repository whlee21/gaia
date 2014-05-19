package gaia.bigdata.api.connector;

import gaia.bigdata.api.State;
import java.util.List;
import java.util.Map;
import org.restlet.resource.Get;
import org.restlet.resource.Post;

public interface ConnectorsResource {
	@Post
	public State create(Map<String, Object> paramMap) throws Exception;

	@Get
	public List<State> listConnectors() throws Exception;
}
