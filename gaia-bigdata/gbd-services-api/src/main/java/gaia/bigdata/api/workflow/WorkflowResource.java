package gaia.bigdata.api.workflow;

import gaia.bigdata.api.State;
import java.util.Map;
import org.restlet.resource.Get;
import org.restlet.resource.Post;

public interface WorkflowResource {
	@Get
	public Workflow retrieve();

	@Post
	public State submit(Map<String, Object> paramMap);
}
