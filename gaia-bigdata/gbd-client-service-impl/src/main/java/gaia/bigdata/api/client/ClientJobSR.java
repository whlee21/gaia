package gaia.bigdata.api.client;

import gaia.bigdata.api.State;
import gaia.bigdata.api.job.JobResource;
import gaia.bigdata.services.ServiceType;
import gaia.commons.api.Configuration;
import gaia.commons.services.BaseServiceLocatorSR;
import gaia.commons.services.ServiceLocator;
import gaia.commons.util.RestletContainer;
import gaia.commons.util.RestletUtil;

import org.restlet.resource.Delete;
import org.restlet.resource.Get;
import org.restlet.resource.ResourceException;

import com.google.inject.Inject;

public class ClientJobSR extends BaseServiceLocatorSR implements JobResource {
	protected String id;

	@Inject
	public ClientJobSR(Configuration configuration, ServiceLocator serviceLocator) {
		super(configuration, serviceLocator);
	}

	protected void doInit() throws ResourceException {
		id = getRequest().getAttributes().get("id").toString();
	}

	@Get
	public State retrieve() {
		State returnValue = null;
		RestletContainer<JobResource> jrRc = RestletUtil.wrap(JobResource.class,
				serviceLocator.getServiceURI(ServiceType.JOB.name()), "/" + id);
		JobResource jr = (JobResource) jrRc.getWrapped();
		try {
			returnValue = jr.retrieve();
		} catch (Throwable e) {
			throw new RuntimeException(e);
		} finally {
			RestletUtil.release(jrRc);
		}
		return returnValue;
	}

	@Delete
	public State cancel() {
		State returnValue = null;
		RestletContainer<JobResource> jrRc = RestletUtil.wrap(JobResource.class,
				serviceLocator.getServiceURI(ServiceType.JOB.name()), "/" + id);
		JobResource jr = (JobResource) jrRc.getWrapped();
		try {
			returnValue = jr.cancel();
		} catch (Throwable e) {
			throw new RuntimeException(e);
		} finally {
			RestletUtil.release(jrRc);
		}
		return returnValue;
	}
}
