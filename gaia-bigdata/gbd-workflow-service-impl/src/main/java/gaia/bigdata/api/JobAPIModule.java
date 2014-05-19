package gaia.bigdata.api;

import gaia.bigdata.api.job.JobService;
import gaia.bigdata.api.job.OozieJobService;
import gaia.commons.api.APIModule;

public class JobAPIModule extends APIModule {
	protected void defineBindings() {
		bind(JobService.class).to(OozieJobService.class);
	}
}
