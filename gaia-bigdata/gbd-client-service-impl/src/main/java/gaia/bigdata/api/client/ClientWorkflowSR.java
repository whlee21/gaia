package gaia.bigdata.api.client;

import gaia.bigdata.api.State;
import gaia.bigdata.api.workflow.Workflow;
import gaia.bigdata.api.workflow.WorkflowResource;
import gaia.bigdata.services.ServiceType;
import gaia.commons.api.Configuration;
import gaia.commons.services.BaseServiceLocatorSR;
import gaia.commons.services.ServiceLocator;
import gaia.commons.util.RestletContainer;
import gaia.commons.util.RestletUtil;

import java.util.Map;

import org.restlet.resource.Get;
import org.restlet.resource.Post;
import org.restlet.resource.ResourceException;

import com.google.inject.Inject;

public class ClientWorkflowSR extends BaseServiceLocatorSR implements WorkflowResource {
	protected String workflow;

	@Inject
	public ClientWorkflowSR(Configuration configuration, ServiceLocator serviceLocator) {
		super(configuration, serviceLocator);
	}

	protected void doInit() throws ResourceException {
		workflow = getRequest().getAttributes().get("workflow").toString();
	}

	@Get
	public Workflow retrieve() {
		Workflow returnValue = null;
		RestletContainer<WorkflowResource> wrRc = RestletUtil.wrap(WorkflowResource.class,
				serviceLocator.getServiceURI(ServiceType.WORKFLOW.name()), "/" + workflow);
		WorkflowResource wr = (WorkflowResource) wrRc.getWrapped();
		try {
			returnValue = wr.retrieve();
		} catch (Throwable e) {
			throw new RuntimeException(e);
		} finally {
			RestletUtil.release(wrRc);
		}
		return returnValue;
	}

	@Post
	public State submit(Map<String, Object> entity) {
		State returnValue = null;
		RestletContainer<WorkflowResource> wrRc = RestletUtil.wrap(WorkflowResource.class,
				serviceLocator.getServiceURI(ServiceType.WORKFLOW.name()), "/" + workflow);
		WorkflowResource wr = (WorkflowResource) wrRc.getWrapped();
		try {
			returnValue = wr.submit(entity);
		} catch (Throwable e) {
			throw new RuntimeException(e);
		} finally {
			RestletUtil.release(wrRc);
		}
		return returnValue;
	}
}
