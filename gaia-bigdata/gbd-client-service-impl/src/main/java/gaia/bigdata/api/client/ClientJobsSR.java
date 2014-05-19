package gaia.bigdata.api.client;

import gaia.bigdata.api.State;
import gaia.bigdata.api.job.JobsResource;
import gaia.bigdata.services.ServiceType;
import gaia.commons.api.Configuration;
import gaia.commons.services.BaseServiceLocatorSR;
import gaia.commons.services.ServiceLocator;
import gaia.commons.util.RestletContainer;
import gaia.commons.util.RestletUtil;

import java.util.List;
import java.util.Properties;

import org.restlet.resource.Get;
import org.restlet.resource.Post;

import com.google.inject.Inject;

public class ClientJobsSR extends BaseServiceLocatorSR implements JobsResource {
	@Inject
	public ClientJobsSR(Configuration configuration, ServiceLocator serviceLocator) {
		super(configuration, serviceLocator);
	}

	@Get
	public List<State> list() {
		List<State> returnValue = null;
		RestletContainer<JobsResource> jrRc = RestletUtil.wrap(JobsResource.class,
				this.serviceLocator.getServiceURI(ServiceType.JOB.name()), "");
		JobsResource jr = (JobsResource) jrRc.getWrapped();
		try {
			returnValue = jr.list();
		} catch (Throwable e) {
			throw new RuntimeException(e);
		} finally {
			RestletUtil.release(jrRc);
		}
		return returnValue;
	}

	@Post
	public State send(Properties props) {
		State returnValue = null;
		RestletContainer<JobsResource> jrRc = RestletUtil.wrap(JobsResource.class,
				this.serviceLocator.getServiceURI(ServiceType.JOB.name()), "");
		JobsResource jr = (JobsResource) jrRc.getWrapped();
		try {
			returnValue = jr.send(props);
		} catch (Throwable e) {
			throw new RuntimeException(e);
		} finally {
			RestletUtil.release(jrRc);
		}
		return returnValue;
	}
}
