package gaia.scheduler;

import gaia.common.params.SchedulingParams;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.lang.math.NumberUtils;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.apache.solr.common.params.SolrParams;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobListener;
import org.quartz.NthIncludedDayTrigger;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SimpleTrigger;
import org.quartz.Trigger;
import org.quartz.TriggerListener;
import org.quartz.impl.DirectSchedulerFactory;
import org.quartz.simpl.RAMJobStore;
import org.quartz.simpl.SimpleThreadPool;
import org.quartz.spi.JobStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public final class DefaultSolrScheduler implements SolrScheduler {
	private static final int DELETE_TIMEOUT = 30000;
	private static transient Logger LOG = LoggerFactory.getLogger(DefaultSolrScheduler.class);
	private static final int DEFAULT_THREAD_COUNT = 10;
	private Scheduler scheduler;
	private Map<String, JobListener> jobListeners = Collections.synchronizedMap(new HashMap<String, JobListener>());
	private Map<String, TriggerListener> triggerListeners = Collections
			.synchronizedMap(new HashMap<String, TriggerListener>());
	private StatusTriggerListener statusListener = new StatusTriggerListener();

	private static AtomicLong triggerNumber = new AtomicLong(-9223372036854775808L);

	private int threadCount = DEFAULT_THREAD_COUNT;

	@Inject
	public DefaultSolrScheduler(SolrQuartzJobFactory jobFactory) throws SchedulerException {
		LOG.info("Initializing DefaultSolrScheduler");

		triggerListeners.put(statusListener.getName(), statusListener);

		DirectSchedulerFactory schedFactory = DirectSchedulerFactory.getInstance();

		String name = "scheduler_" + System.currentTimeMillis();
		if (threadCount <= 0) {
			threadCount = DEFAULT_THREAD_COUNT;
		}
		SimpleThreadPool threadPool = new SimpleThreadPool(threadCount, 5);

		threadPool.setMakeThreadsDaemons(true);
		threadPool.initialize();
		JobStore store = new RAMJobStore();

		schedFactory.createScheduler(name, name, threadPool, store);
		scheduler = schedFactory.getScheduler(name);
		for (JobListener listener : jobListeners.values()) {
			scheduler.addGlobalJobListener(listener);
		}
		for (TriggerListener listener : triggerListeners.values()) {
			scheduler.addGlobalTriggerListener(listener);
		}
		scheduler.setJobFactory(jobFactory);
		scheduler.start();
	}

	public void addTriggerListener(String name, TriggerListener listener) throws SchedulerException {
		triggerListeners.put(name, listener);
		scheduler.addGlobalTriggerListener(listener);
	}

	public void addJobListener(String name, JobListener listener) throws SchedulerException {
		jobListeners.put(name, listener);
		scheduler.addGlobalJobListener(listener);
	}

	public boolean doesJobExist(String jobName) throws SchedulerException {
		return null != scheduler.getJobDetail(jobName, "DEFAULT");
	}

	public ScheduledJob schedule(String jobName, SolrParams params) throws SchedulerException, ParseException {
		return schedule(jobName, params, SolrJob.class);
	}

	public ScheduledJob schedule(String jobName, SolrParams params, Class<?> jobClass) throws SchedulerException,
			ParseException {
		LOG.info("scheduleJob " + jobName);
		JobDetail jobDetail = scheduler.getJobDetail(jobName, "DEFAULT");

		boolean jobExists = jobDetail != null;
		if (!jobExists) {
			jobDetail = new JobDetail(jobName, "DEFAULT", jobClass);
		}

		JobDataMap dataMap = jobDetail.getJobDataMap();
		dataMap.put("jobId", jobName);

		Iterator<String> paramNamesIter = params.getParameterNamesIterator();
		while (paramNamesIter.hasNext()) {
			String pName = paramNamesIter.next();
			String[] options = params.getParams(pName);
			dataMap.put(pName, options);
		}

		dataMap.put("jobHandler", params.get("jobHandler"));

		Trigger trigger = createTrigger(params, jobName);
		Date scheduleDate;
		if (!jobExists) {
			scheduleDate = scheduler.scheduleJob(jobDetail, trigger);
		} else {
			LOG.info("Job Exists, adding Trigger");
			scheduleDate = scheduler.scheduleJob(trigger);
		}

		return new ScheduledJob(jobDetail.getKey().getName(), scheduleDate.toString(), trigger.getName(), trigger.getKey()
				.getName());
	}

	public Map<String, Object> getStatus(String keyName) {
		Map<String, Object> status = new HashMap<String, Object>();
		status.put("running", Boolean.valueOf(statusListener.isRunning(keyName)));
		return status;
	}

	public void stop(String jobName) throws SchedulerException {
		scheduler.interrupt(jobName, "DEFAULT");
		List<JobExecutionContext> executingJobs = scheduler.getCurrentlyExecutingJobs();

		long time = System.currentTimeMillis();

		List<String> names = new ArrayList<String>();
		for (JobExecutionContext jec : executingJobs) {
			names.add(jec.getJobDetail().getName());
		}
		if (LOG.isInfoEnabled()) {
			LOG.info("Stopping: " + jobName);
		}
		while ((time < DELETE_TIMEOUT) && (names.contains(jobName))) {
			executingJobs = scheduler.getCurrentlyExecutingJobs();
			names = new ArrayList<String>();
			for (JobExecutionContext jec : executingJobs) {
				names.add(jec.getJobDetail().getName());
			}
			for (JobExecutionContext context : executingJobs) {
				scheduler.interrupt(context.getJobDetail().getName(), "DEFAULT");
			}

			time = System.currentTimeMillis();
		}

		executingJobs = scheduler.getCurrentlyExecutingJobs();
		names.clear();
		for (JobExecutionContext jec : executingJobs) {
			names.add(jec.getJobDetail().getName());
		}
		if (names.contains(jobName))
			LOG.warn("Could not interrupt job:" + jobName);
	}

	public void delete(String jobName) throws SchedulerException {
		boolean result = scheduler.deleteJob(jobName, "DEFAULT");
	}

	public void stopAllJobs() throws SchedulerException {
		try {
			scheduler.standby();
			String[] jobNames = scheduler.getJobNames("DEFAULT");
			boolean jobInterrupted;
			for (int i = 0; i < jobNames.length; i++) {
				String name = jobNames[i];
				if (LOG.isInfoEnabled()) {
					LOG.info("Stopping: " + name);
				}

				jobInterrupted = scheduler.interrupt(name, "DEFAULT");
			}

			List<JobExecutionContext> executingJobs = scheduler.getCurrentlyExecutingJobs();
			long time = System.currentTimeMillis();
			long timeout = time + DELETE_TIMEOUT;
			while ((time < timeout) && (executingJobs.size() > 0)) {
				executingJobs = scheduler.getCurrentlyExecutingJobs();
				for (JobExecutionContext context : executingJobs) {
					scheduler.interrupt(context.getJobDetail().getName(), "DEFAULT");

					scheduler.deleteJob(context.getJobDetail().getName(), "DEFAULT");
				}

				time = System.currentTimeMillis();
			}
			if (scheduler.getCurrentlyExecutingJobs().size() > 0)
				LOG.warn("Not all jobStates stopped in time");
		} finally {
			if (scheduler.isInStandbyMode())
				scheduler.start();
		}
	}

	public void deleteAllJobs() throws SchedulerException {
		try {
			scheduler.standby();
			String[] jobNames = scheduler.getJobNames("DEFAULT");
			boolean result;
			for (int i = 0; i < jobNames.length; i++) {
				String name = jobNames[i];
				if (LOG.isInfoEnabled()) {
					LOG.info("Deleting: " + name);
				}

				result = scheduler.deleteJob(name, "DEFAULT");
			}
		} finally {
			if (scheduler.isInStandbyMode())
				scheduler.start();
		}
	}

	protected Trigger createTrigger(SolrParams params, String jobName) throws ParseException {
		Trigger trigger = null;

		/*
		 * 2013.10.08
		 * updator : ho kyung.lee
		 * desc : jobRepeat값이 true false로 넘어오지 않는 경우 발생. 1과 0으로 넘어오는 경우 있음.
		 *        이에 1과 0으로 넘어올 경우 1일 경우 true로 치환 0일 경우 false로 치환함.
		 */
//		String tempParamVal = params.get("jobRepeat");
//		boolean temp = false;
//		try {
//			int tempInt = Integer.parseInt(tempParamVal);
//			if (tempInt == 1) {
//				temp = true;
//			}
//		}catch(Exception e) {
//			temp = params.getBool("jobRepeat", false);
//		}
		//int repeatInt = params.getInt("jobRepeat");
		
		//LOG.info("createTrigger112 repeatInt " + repeatInt);
		boolean repeat = params.getBool("jobRepeat", false);
		
//		boolean repeat = temp;
		if (repeat) {
			int repeatCount = params.getInt("jobRepeatCount", -1);

			String repeatIntStr = params.get("jobRepeatInterval");
			if (repeatIntStr != null) {
				String repeatUnitsStr = params.get("jobRepeatUnits");
				SchedulingParams.JobRepeatUnits units = SchedulingParams.JobRepeatUnits.get(repeatUnitsStr);

				if (units == null) {
					units = SchedulingParams.JobRepeatUnits.MILLISECOND;
				}
				long interval = NumberUtils.toLong(params.get("jobRepeatInterval"), -1L);

				if (interval != -1L) {
					// MILLISECOND, SECOND, MINUTE, HOUR, DAY, WEEK, MONTH;
					switch (units) {
					case MILLISECOND:
//						trigger = createRepeatTrigger(repeatCount, interval * 86400000L);
						trigger = createRepeatTrigger(repeatCount, interval);
						break;
					case SECOND:
						trigger = createRepeatTrigger(repeatCount, interval * 1000L);
						break;
					case MINUTE:
						trigger = createRepeatTrigger(repeatCount, interval * 60000L);
						break;
					case HOUR:
						trigger = createRepeatTrigger(repeatCount, interval * 3600000L);
						break;
					case DAY:
						trigger = createNthTrigger((int) interval, 1);
						break;
					case WEEK:
						trigger = createNthTrigger((int) interval, 3);
						break;
					default:
						LOG.debug("No Repeat units specified, assuming milliseconds");
						trigger = createRepeatTrigger(repeatCount, interval);
						break;
					}
				} else
					trigger = new SimpleTrigger();
			} else {
				trigger = new SimpleTrigger();
			}
		} else {
			trigger = new SimpleTrigger();
		}

		String startDateStr = params.get("jobStartDate");
		if (startDateStr != null) {
			Date startDate = ClientUtils.getThreadLocalDateFormat().parse(startDateStr);

			trigger.setStartTime(startDate);
		} else {
			trigger.setStartTime(new Date());
		}
		String endDateStr = params.get("jobEndDate");
		if (endDateStr != null) {
			Date endDate = ClientUtils.getThreadLocalDateFormat().parse(endDateStr);
			Date startTime = trigger.getStartTime();
			if (endDate.after(startTime))
				trigger.setEndTime(endDate);
			else {
				LOG.warn("Job's start time is after it's end time");
			}
		}

		trigger.setName(jobName + "_Trigger" + "_" + trigger.getStartTime().getTime() + "_"
				+ triggerNumber.getAndIncrement());

		trigger.setJobName(jobName);
		int jobPri = params.getInt("jobPriority", 5);
		trigger.setMisfireInstruction(0);
		trigger.setPriority(jobPri);
		return trigger;
	}

	private Trigger createRepeatTrigger(int repeatCount, long repeatTime) {
		SimpleTrigger result = new SimpleTrigger();
		result.setRepeatCount(repeatCount);
		result.setRepeatInterval(repeatTime);
		result.setMisfireInstruction(0);
		return result;
	}

	private Trigger createNthTrigger(int n, int intervalType) {
		NthIncludedDayTrigger result = new NthIncludedDayTrigger();
		result.setN(n);
		result.setIntervalType(intervalType);
		result.setMisfireInstruction(0);
		return result;
	}

	public JobListener getJobListener(String name) {
		return (JobListener) jobListeners.get(name);
	}

	public TriggerListener getTriggerListener(String name) {
		return (TriggerListener) triggerListeners.get(name);
	}

	public void shutdown() {
		try {
			if (scheduler != null) {
				LOG.info("Shutting down scheduler");
				scheduler.shutdown();
				LOG.info("Scheduler shutdown");
			}
		} catch (SchedulerException e) {
			LOG.info("Couldn't shut down the scheduler", e);
		}
	}

	static {
		System.setProperty("org.terracotta.quartz.skipUpdateCheck", "true");
	}
}
