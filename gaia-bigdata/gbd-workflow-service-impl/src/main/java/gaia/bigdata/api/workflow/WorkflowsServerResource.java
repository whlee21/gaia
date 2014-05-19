package gaia.bigdata.api.workflow;

import java.util.HashMap;
import java.util.Map;

import org.restlet.data.Status;
import org.restlet.resource.ResourceException;
import org.restlet.resource.ServerResource;

import com.google.inject.Inject;

public class WorkflowsServerResource extends ServerResource implements WorkflowsResource {
	private final WorkflowService client;

	@Inject
	public WorkflowsServerResource(WorkflowService client) {
		this.client = client;
	}

	public Map<String, Workflow> list() {
		try {
			Map<String, Workflow> out = new HashMap<String, Workflow>();
			for (Workflow workflow : client.listWorkflows()) {
				out.put(workflow.getId(), workflow);
			}
			return out;
		} catch (Exception e) {
			throw new ResourceException(Status.SERVER_ERROR_INTERNAL, e);
		}
	}
}
