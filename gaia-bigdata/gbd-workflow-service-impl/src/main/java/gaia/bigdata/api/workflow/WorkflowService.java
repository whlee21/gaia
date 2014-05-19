package gaia.bigdata.api.workflow;

import java.util.Collection;
import java.util.Properties;

public interface WorkflowService {
	public Collection<Workflow> listWorkflows() throws Exception;

	public Workflow getWorkflowById(String paramString) throws Exception;

	public Properties loadWorkflowProperties(Workflow paramWorkflow) throws Exception;
}
