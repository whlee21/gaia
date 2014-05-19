package gaia.bigdata.api.client;

import com.google.inject.Inject;
import gaia.commons.api.Configuration;
import gaia.commons.services.BaseServiceLocatorSR;
import gaia.commons.services.ServiceLocator;
import gaia.commons.util.RestletContainer;
import gaia.commons.util.RestletUtil;
import gaia.bigdata.services.ServiceType;

import gaia.bigdata.api.workflow.Workflow;
import gaia.bigdata.api.workflow.WorkflowsResource;

import java.util.Map;

import org.restlet.resource.Get;

public class ClientWorkflowsSR extends BaseServiceLocatorSR implements WorkflowsResource {
	@Inject
	public ClientWorkflowsSR(Configuration configuration, ServiceLocator serviceLocator) {
		super(configuration, serviceLocator);
	}

	@Get
	public Map<String, Workflow> list() {
		Map<String, Workflow> returnValue = null;
		RestletContainer<WorkflowsResource> resourceRc = RestletUtil.wrap(WorkflowsResource.class,
				serviceLocator.getServiceURI(ServiceType.WORKFLOW.name()), "");
		WorkflowsResource resource = (WorkflowsResource) resourceRc.getWrapped();
		try {
			returnValue = resource.list();
		} catch (Throwable e) {
			throw new RuntimeException(e);
		} finally {
			RestletUtil.release(resourceRc);
		}
		return returnValue;
	}
}
