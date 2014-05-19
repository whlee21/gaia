package gaia.bigdata.api.job;

import gaia.bigdata.api.State;
import java.util.List;
import java.util.Properties;
import org.restlet.resource.Get;
import org.restlet.resource.Post;

public interface JobsResource {
	@Get
	public List<State> list();

	@Post
	public State send(Properties paramProperties);
}
