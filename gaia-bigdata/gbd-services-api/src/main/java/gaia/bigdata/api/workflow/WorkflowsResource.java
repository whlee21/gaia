package gaia.bigdata.api.workflow;

import java.util.Map;
import org.restlet.resource.Get;

public interface WorkflowsResource {
	@Get
	public Map<String, Workflow> list();
}
