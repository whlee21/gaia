package gaia.bigdata.api;

import gaia.bigdata.api.workflow.WorkflowServerResource;
import gaia.bigdata.api.workflow.WorkflowsServerResource;
import gaia.bigdata.services.ServiceType;
import gaia.commons.api.API;
import gaia.commons.api.ResourceFinder;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class WorkflowAPI extends API {
	@Inject
	public WorkflowAPI(ResourceFinder finder) {
		super(finder);
	}

	protected void initAttachments() {
		attach("", WorkflowsServerResource.class);
		attach("/{workflow}", WorkflowServerResource.class);
	}

	public String getAPIRoot() {
		return "/workflow";
	}

	public String getAPIName() {
		return ServiceType.WORKFLOW.name();
	}
}
