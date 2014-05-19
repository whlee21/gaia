package gaia.scheduler;

import java.text.ParseException;
import java.util.Map;
import org.apache.solr.common.params.SolrParams;
import org.quartz.JobListener;
import org.quartz.SchedulerException;
import org.quartz.TriggerListener;

public interface SolrScheduler {
	public void addTriggerListener(String name, TriggerListener listener) throws SchedulerException;

	public void addJobListener(String name, JobListener listener) throws SchedulerException;

	public boolean doesJobExist(String jobName) throws SchedulerException;

	public ScheduledJob schedule(String jobName, SolrParams params) throws SchedulerException, ParseException;

	public ScheduledJob schedule(String jobName, SolrParams params, Class<?> jobClass) throws SchedulerException,
			ParseException;

	public Map<String, Object> getStatus(String keyName);

	public void stop(String jobName) throws SchedulerException;

	public void delete(String jobName) throws SchedulerException;

	public void stopAllJobs() throws SchedulerException;

	public void deleteAllJobs() throws SchedulerException;

	public JobListener getJobListener(String name);

	public TriggerListener getTriggerListener(String name);

	public void shutdown();
}
