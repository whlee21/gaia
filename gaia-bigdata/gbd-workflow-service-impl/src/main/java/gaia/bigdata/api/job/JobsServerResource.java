package gaia.bigdata.api.job;

import gaia.bigdata.api.State;

import java.util.List;
import java.util.Properties;

import org.restlet.resource.ResourceException;
import org.restlet.resource.ServerResource;

import com.google.inject.Inject;

public class JobsServerResource extends ServerResource implements JobsResource {
	private final JobService jobClient;

	@Inject
	public JobsServerResource(JobService jobClient) {
		this.jobClient = jobClient;
	}

	public List<State> list() {
		String status = getQuery().getFirstValue("status");
		try {
			if (status == null)
				return jobClient.listJobs();
			gaia.bigdata.api.Status jobStatus;
			try {
				jobStatus = gaia.bigdata.api.Status.valueOf(status.toUpperCase());
			} catch (IllegalArgumentException e) {
				throw new ResourceException(org.restlet.data.Status.CLIENT_ERROR_BAD_REQUEST, "Unknown job status: " + status);
			}
			return jobClient.listJobsWithStatus(jobStatus);
		} catch (Exception e) {
			throw new ResourceException(org.restlet.data.Status.SERVER_ERROR_INTERNAL, e);
		}
	}

	public State send(Properties props) {
		try {
			return jobClient.submitWorkflow(props);
		} catch (Exception e) {
			throw new ResourceException(org.restlet.data.Status.SERVER_ERROR_INTERNAL, e);
		}
	}
}
