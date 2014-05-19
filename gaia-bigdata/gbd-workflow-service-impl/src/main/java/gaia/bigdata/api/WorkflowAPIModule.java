package gaia.bigdata.api;

import gaia.bigdata.api.workflow.OozieWorkflowService;
import gaia.bigdata.api.workflow.WorkflowService;
import gaia.commons.api.APIModule;

public class WorkflowAPIModule extends APIModule {
	protected void defineBindings() {
		bind(WorkflowService.class).to(OozieWorkflowService.class);
	}
}
