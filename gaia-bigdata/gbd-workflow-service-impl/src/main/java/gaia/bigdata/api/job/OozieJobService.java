package gaia.bigdata.api.job;

import gaia.bigdata.api.State;
import gaia.bigdata.api.Status;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.oozie.client.OozieClient;
import org.apache.oozie.client.OozieClientException;
import org.apache.oozie.client.WorkflowJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.BiMap;
import com.google.common.collect.EnumBiMap;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import gaia.commons.api.Configuration;
import gaia.commons.services.BaseService;
import gaia.commons.services.ServiceLocator;
import gaia.bigdata.services.ServiceType;

@Singleton
public class OozieJobService extends BaseService implements JobService {
	static final BiMap<Status, WorkflowJob.Status> statusMap = EnumBiMap.create(Status.class, WorkflowJob.Status.class);

	private static transient Logger log = LoggerFactory.getLogger(OozieJobService.class);
	static final String OOZIE_URL = "oozie.url";
	static final String OOZIE_USER = "oozie.user";
	private final OozieClient oozieClient;
	private final String oozieUser;

	static Status fromOozieStatus(WorkflowJob.Status status) {
		Status mappedStatus = (Status) statusMap.inverse().get(status);
		if (mappedStatus == null) {
			return Status.UNKNOWN;
		}
		return mappedStatus;
	}

	static WorkflowJob.Status toOozieStatus(Status status) {
		return (WorkflowJob.Status) statusMap.get(status);
	}

	static State translateJob(WorkflowJob oozieJob) {
		Status jobStatus = fromOozieStatus(oozieJob.getStatus());
		return new State(oozieJob.getId(), oozieJob.getParentId(), oozieJob.getAppName(), (String) null, jobStatus,
				oozieJob.getCreatedTime());
	}

	@Inject
	public OozieJobService(Configuration config, ServiceLocator locator) {
		super(config, locator);
		if ((config.getProperties().containsKey(OOZIE_USER)) && (config.getProperties().containsKey(OOZIE_URL))) {
			oozieClient = new OozieClient(config.getProperties().getProperty(OOZIE_URL));
			oozieUser = config.getProperties().getProperty(OOZIE_USER);
		} else {
			throw new IllegalArgumentException("Missing one or more required config values: oozie.url, oozie.user");
		}
	}

	public String getType() {
		return ServiceType.JOB.name();
	}

	public State submitWorkflow(Properties properties) throws Exception {
		properties.setProperty("user.name", oozieUser);
		log.info("Workflow properties: {}", properties);
		try {
			String jobId = oozieClient.run(properties);
			WorkflowJob job = oozieClient.getJobInfo(jobId);
			return translateJob(job);
		} catch (OozieClientException e) {
			log.error("Error submitting Oozie workflow: {}", e.getMessage());
			throw unwrapOozieException(e);
		}
	}

	public State getJobById(String id) throws Exception {
		try {
			WorkflowJob job = oozieClient.getJobInfo(id);
			return translateJob(job);
		} catch (OozieClientException e) {
			throw unwrapOozieException(e);
		}
	}

	public State cancelJob(String jobId) throws Exception {
		try {
			oozieClient.kill(jobId);

			WorkflowJob job = oozieClient.getJobInfo(jobId);
			return translateJob(job);
		} catch (OozieClientException e) {
			throw unwrapOozieException(e);
		}
	}

	public List<State> listJobs() throws Exception {
		try {
			List<State> out = new ArrayList<State>();
			for (WorkflowJob oozieJob : oozieClient.getJobsInfo("")) {
				out.add(translateJob(oozieJob));
			}
			return out;
		} catch (OozieClientException e) {
			throw unwrapOozieException(e);
		}
	}

	public List<State> listJobsWithStatus(Status status) throws Exception {
		try {
			List<State> out = new ArrayList<State>();
			WorkflowJob.Status oozieStatus = toOozieStatus(status);
			for (WorkflowJob oozieJob : oozieClient.getJobsInfo("status=" + oozieStatus + "")) {
				out.add(translateJob(oozieJob));
			}
			return out;
		} catch (OozieClientException e) {
			throw unwrapOozieException(e);
		}
	}

	private Exception unwrapOozieException(OozieClientException e) {
		if (e.getErrorCode().equals("IO_ERROR")) {
			return (IOException) e.getCause();
		}
		log.error(e.getMessage());
		return new RuntimeException(e.getCause());
	}

	static {
		statusMap.put(Status.RUNNING, WorkflowJob.Status.RUNNING);
		statusMap.put(Status.SUCCEEDED, WorkflowJob.Status.SUCCEEDED);
		statusMap.put(Status.FAILED, WorkflowJob.Status.FAILED);
		statusMap.put(Status.KILLED, WorkflowJob.Status.KILLED);
		statusMap.put(Status.SUBMITTED, WorkflowJob.Status.PREP);
	}
}
