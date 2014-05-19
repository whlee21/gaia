package gaia.bigdata.api.job;

import gaia.bigdata.api.State;
import gaia.bigdata.api.Status;

import java.util.List;
import java.util.Properties;

public interface JobService {
	public State submitWorkflow(Properties paramProperties) throws Exception;

	public State getJobById(String paramString) throws Exception;

	public State cancelJob(String paramString) throws Exception;

	public List<State> listJobs() throws Exception;

	public List<State> listJobsWithStatus(Status paramStatus) throws Exception;
}
