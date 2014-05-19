package gaia.bigdata.api;

import gaia.bigdata.api.job.JobServerResource;
import gaia.bigdata.api.job.JobsServerResource;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import gaia.commons.api.API;
import gaia.commons.api.ResourceFinder;
import gaia.bigdata.services.ServiceType;

@Singleton
public class JobAPI extends API {
	@Inject
	public JobAPI(ResourceFinder finder) {
		super(finder);
	}

	protected void initAttachments() {
		attach("", JobsServerResource.class);
		attach("/{job}", JobServerResource.class);
	}

	public String getAPIRoot() {
		return "/job";
	}

	public String getAPIName() {
		return ServiceType.JOB.name();
	}
}
