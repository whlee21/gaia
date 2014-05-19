package gaia.bigdata.api.connector;

import gaia.bigdata.api.State;
import java.util.Map;
import org.restlet.resource.Delete;
import org.restlet.resource.Get;
import org.restlet.resource.Post;
import org.restlet.resource.Put;

public interface ConnectorResource {
	@Get
	public State status();

	@Put
	public State update(Map<String, Object> paramMap);

	@Delete
	public boolean remove();

	@Post
	public State execute(Map<String, Object> paramMap);
}
