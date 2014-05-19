package gaia.bigdata.api.job;

import gaia.bigdata.api.State;

import org.restlet.data.Status;
import org.restlet.resource.ResourceException;
import org.restlet.resource.ServerResource;

import com.google.inject.Inject;

public class JobServerResource extends ServerResource implements JobResource {
	private String id;
	private final JobService jobClient;

	@Inject
	public JobServerResource(JobService jobClient) {
		this.jobClient = jobClient;
	}

	protected void doInit() throws ResourceException {
		id = ((String) getRequest().getAttributes().get("job"));
	}

	public State retrieve() {
		try {
			return jobClient.getJobById(id);
		} catch (Exception e) {
			throw new ResourceException(Status.SERVER_ERROR_INTERNAL, e);
		}
	}

	public State cancel() {
		try {
			return jobClient.cancelJob(id);
		} catch (Exception e) {
			throw new ResourceException(Status.SERVER_ERROR_INTERNAL, e);
		}
	}
}
