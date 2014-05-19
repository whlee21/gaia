package gaia.admin.collection;

import gaia.Constants;
import gaia.common.params.SchedulingParams;
import gaia.crawl.ConnectorManager;
import gaia.crawl.datasource.DataSource;
import gaia.crawl.datasource.DataSourceId;
import gaia.scheduler.ScheduledJob;
import gaia.scheduler.SolrScheduler;

import java.text.ParseException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.quartz.SchedulerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public final class AdminScheduler {
	private static transient Logger LOG = LoggerFactory.getLogger(AdminScheduler.class);
	private CollectionManager cm;
	private SolrScheduler solrScheduler;
	private ConnectorManager ccm;

	@Inject
	public AdminScheduler(CollectionManager cm, SolrScheduler solrScheduler, ConnectorManager ccm) {
		this.solrScheduler = solrScheduler;
		this.cm = cm;
		this.ccm = ccm;

		initLogsCrawlerScheduleIfNeeded();
	}

	private void initLogsCrawlerScheduleIfNeeded() {
		if (null == cm.getCollection(Constants.LOGS_COLLECTION)) {
			return;
		}

		List dss = null;
		try {
			dss = ccm.listDataSources(Constants.LOGS_COLLECTION);
		} catch (Exception e2) {
			LOG.warn("Unable to obtain a list of datasources", e2);
		}
		DataSourceId dsId = null;
		if ((dss != null) && (dss.size() > 0)) {
			dsId = (DataSourceId) dss.get(0);
		} else {
			Map<String, Object> map = new HashMap<String, Object>();
			map.put("type", "gaiasearchlogs");
			map.put("crawler", "gaia.logs");
			map.put("name", "gaiasearchlogs");
			map.put("collection", Constants.LOGS_COLLECTION);
			try {
				DataSource ds = ccm.createDataSource(map);
				if (ccm.addDataSource(ds))
					dsId = ds.getDataSourceId();
				else
					throw new Exception("addDataSource returned false");
			} catch (Exception e) {
				LOG.warn("Unable to initialize log indexing: " + e.toString());
			}
		}
		if (dsId != null) {
			ScheduledSolrCommand cmd = cm.getScheduledSolrCommand(Constants.LOGS_COLLECTION, dsId.toString());
			Schedule s = cmd != null ? cmd.getSchedule() : null;
			if (s == null) {
				s = new Schedule();
				s.setActive(true);
				s.setRepeatUnit(SchedulingParams.JobRepeatUnits.SECOND);
				s.setInterval(60);
				s.setStartTime(new Date(System.currentTimeMillis() + 30000L));
			}
			if (cmd == null) {
				cmd = SchedParamUtil.createCrawlCommand(s, Constants.LOGS_COLLECTION, dsId.toString());
				cm.addScheduledSolrCommand(Constants.LOGS_COLLECTION, cmd, dsId.toString());
			} else {
				if (cmd.getSchedule() == null) {
					cmd.setSchedule(s);
				}
				SchedParamUtil.fillCrawlCommand(cmd, Constants.LOGS_COLLECTION, dsId.toString());
				cm.updateScheduledSolrCommand(Constants.LOGS_COLLECTION, cmd, dsId.toString());
			}
		}
	}

	public static String getJobNameKey(DataSourceId dsId, Schedule schedule) {
		String key = dsId.toString();
		return key;
	}

	public void deleteJob(String jobName) throws SchedulerException {
		solrScheduler.delete(jobName);
	}

	public void stopJob(String jobName) throws SchedulerException {
		solrScheduler.stop(jobName);
	}

	public Map<String, Object> getStatus(ScheduledSolrCommand cmd) throws SolrServerException, SchedulerException,
			ParseException {
		String triggerName = cmd.getSchedule().getTriggerName();
		boolean running = false;
		if (triggerName != null) {
			Map<String, Object> jobStatus = solrScheduler.getStatus(triggerName);
			running = ((Boolean) jobStatus.get("running")).booleanValue();
		}

		if ((!running) || (triggerName == null)) {
			Map<String, Object> resp = new HashMap<String, Object>(2);
			resp.put("id", cmd.getId());
			resp.put("type", cmd.getName());
			resp.put("running", Boolean.valueOf(false));
			return resp;
		}

		Map<String, Object> map = new HashMap<String, Object>();
		map.put("id", cmd.getId());
		map.put("type", cmd.getName());
		map.put("running", Boolean.valueOf(running));
		return map;
	}

	public void schedule(String collection, ScheduledSolrCommand cmd, boolean fuzzyStartTime) {
		if (!canSchedule(cmd.getSchedule(), new Date(), fuzzyStartTime)) {
			return;
		}
		schedule(collection, cmd, cmd.getSchedule());
	}

	protected void schedule(String collection, ScheduledSolrCommand cmd, Schedule adjusted) {
		if (LOG.isInfoEnabled()) {
			LOG.info("Scheduling: '" + cmd.getName() + "' cmd_id: " + cmd.getId() + " at " + adjusted.getStartTime()
					+ " with repeat: " + adjusted.getInterval() + " units: " + adjusted.getRepeatUnit() + " for activity: "
					+ adjusted.getActivity());
		}

		if (adjusted.isActive()) {
			if (cmd.params == null) {
				cmd.setParams(new GaiaSolrParams());
			}
			SchedParamUtil.addScheduleParams(adjusted, cmd.params);

			if (cmd.getParams().toNamedList().size() == 0) {
				throw new IllegalArgumentException("No params specified in cmd");
			}
			GaiaSolrParams params = new GaiaSolrParams(cmd.getParams());
			try {
				ScheduledJob job = solrScheduler.schedule(cmd.getId(), params);
				String triggerName = job.getTriggerKey();
				Schedule schedule = cmd.getSchedule();
				schedule.setTriggerName(triggerName);

				cm.updateScheduledSolrCommand(collection, cmd, cmd.getId());
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
	}

	public void scheduleAllCommands(String collection) throws SchedulerException {
		for (ScheduledSolrCommand cmd : cm.getAllScheduledSolrCommands(collection)) {
			Schedule schedule = cmd.getSchedule();
			if (!schedule.activityIsCrawl()) {
				stop(collection, cmd);

				schedule = advanceStartTimeModInterval(cmd);
				if ((schedule.isActive()) && (null != schedule.getStartTime()) && (schedule.getStartTime().after(new Date()))) {
					GaiaSolrParams uParams = cmd.getParams();
					if (uParams == null) {
						try {
							uParams = SchedParamUtil.addParams(cmd, collection, cm);
							cm.updateScheduledSolrCommand(collection, cmd, cmd.getId());
						} catch (IllegalStateException ise) {
							LOG.warn("skipping scheduled command: " + cmd + ", exception: " + ise.toString());
						}
					}
					uParams.set("jobStartDate",
							new String[] { ClientUtils.getThreadLocalDateFormat().format(schedule.getStartTime()) });
					cmd.setParams(uParams);
					schedule(collection, cmd, schedule);
				}
			}
		}
	}

	public void scheduleAllDataSources(String collection) throws SchedulerException {
		List<DataSourceId> dataSources = null;
		try {
			dataSources = ccm.listDataSources(collection);
		} catch (Exception e) {
			LOG.warn("Unable to obtain a list of datasources", e);
		}
		if ((dataSources != null) && (!dataSources.isEmpty())) {
			LOG.info("Scheduling persistent Data Sources for collection: " + collection);

			for (DataSourceId dataSource : dataSources) {
				ScheduledSolrCommand cmd = cm.getScheduledSolrCommand(collection, dataSource.toString());
				if (cmd != null) {
					if ((cmd.getParams() == null) || (cmd.getParams().getVals().isEmpty())) {
						SchedParamUtil.fillCrawlCommand(cmd, collection, dataSource.toString());
						cm.updateScheduledSolrCommand(collection, cmd, dataSource.toString());
					}
					stop(collection, cmd);
				} else {
					Schedule s = new Schedule();
					s.setActive(true);
					s.setStartTime(new Date());
					cmd = SchedParamUtil.createCrawlCommand(s, collection, dataSource.toString());
					cm.addScheduledSolrCommand(collection, cmd, dataSource.toString());
				}

				Schedule adjusted = advanceStartTimeModInterval(cmd);
				if ((adjusted.isActive()) && (null != adjusted.getStartTime()) && (adjusted.getStartTime().after(new Date()))) {
					schedule(collection, cmd, adjusted);
				}
			}
			LOG.info("Done scheduling persistent Data Sources for collection: " + collection);
		}
	}

	public void scheduleDataSource(String collection, DataSourceId dataSource, Schedule schedule, boolean fuzzyStartTime)
			throws SchedulerException {
		if ((!schedule.isActive()) || (!canSchedule(schedule, new Date(), fuzzyStartTime))) {
			return;
		}

		stop(collection, dataSource, schedule);
		ScheduledSolrCommand cmd = SchedParamUtil.createCrawlCommand(schedule, collection, dataSource.toString());
		schedule(collection, cmd, schedule);
	}

	public void startAllSchedules() throws SchedulerException {
		for (String collection : cm.getCollectionNames())
			startAllSchedules(collection);
	}

	public void startAllSchedules(String collection) throws SchedulerException {
		scheduleAllDataSources(collection);
		scheduleAllCommands(collection);
	}

	public void stop(String collection, DataSourceId dataSource, Schedule schedule) throws SchedulerException {
		String jobNameKey = getJobNameKey(dataSource, schedule);
		stopJob(jobNameKey);
		deleteJob(jobNameKey);
	}

	public void stop(String collection, ScheduledSolrCommand cmd) throws SchedulerException {
		String name = cmd.getId();
		if (name != null) {
			stopJob(name);
			deleteJob(name);
		}
	}

	public void stopAllDataSources() throws SchedulerException {
		java.util.Collection<Collection> collections = cm.getCollections();
		if ((collections != null) && (!collections.isEmpty())) {
			LOG.info("Stopping persistent Data Sources");
			for (Collection collection : collections) {
				List<DataSourceId> dataSources = null;
				try {
					dataSources = ccm.listDataSources(collection.getName());
				} catch (Exception e) {
					LOG.warn("Unable to obtain a list of datasources for collection " + collection.getName(), e);
				}

				for (DataSourceId dataSource : dataSources)
					stop(collection.getName(), dataSource, null);
			}
			LOG.info("Done stopping all Data Sources");
		}
	}

	public void stopAllDataSources(Collection collection) throws SchedulerException {
		LOG.info("Stopping all data sources for collection: " + collection);
		List<DataSourceId> dataSources = null;
		try {
			dataSources = ccm.listDataSources(collection.getName());
		} catch (Exception e) {
			LOG.warn("Unable to obtain a list of datasources for collection " + collection.getName(), e);
		}

		for (DataSourceId dataSource : dataSources) {
			stop(collection.getName(), dataSource, null);
		}
		LOG.info("Completed Stopping all Data sources");
	}

	public void stopAllScheduledSolrCmds(Collection collection) throws SchedulerException {
		java.util.Collection<ScheduledSolrCommand> cmds = cm.getAllScheduledSolrCommands(collection.getName());
		LOG.info("Stopping all scheduled solr cmds for collection: " + collection);
		for (ScheduledSolrCommand cmd : cmds) {
			stop(collection.getName(), cmd);
		}
		LOG.info("Completed stopping all scheduled solr cmds");
	}

	public void stopAndRemoveAllSchedules() {
		try {
			solrScheduler.stopAllJobs();
			solrScheduler.deleteAllJobs();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public void stopAndRemoveAllSchedules(String collection) throws SchedulerException {
		LOG.debug("whlee21 before stopAllDataSources");
		stopAllDataSources(cm.getCollection(collection));
		LOG.debug("whlee21 after stopAllDataSources");
		stopAllScheduledSolrCmds(cm.getCollection(collection));
	}

	private Schedule advanceStartTimeModInterval(ScheduledSolrCommand cmd) {
		Schedule s = cmd.getSchedule();
		if (s == null) {
			return null;
		}
		Date now = new Date();
		Date start = s.getStartTime();
		long intervalInMillis = s.getInterval() * 1000L;

		if ((!s.isActive()) || (null == start) || (now.before(start)) || (0L == intervalInMillis)) {
			return s;
		}
		long diffInMillis = now.getTime() - start.getTime();

		Date newStart = new Date(start.getTime() + intervalInMillis * (1L + diffInMillis / intervalInMillis));

		LOG.info("Advancing internal schedule start (interval=" + s.getInterval() + ") from " + start + " to " + newStart);

		Schedule adjusted = new Schedule(s);
		adjusted.setStartTime(newStart);
		return adjusted;
	}

	public static boolean canSchedule(Schedule schedule, Date time, boolean fuzzyStartTime) {
		boolean result = false;

		Date startTime = schedule.getStartTime();
		Date endTime = schedule.getEndTime();
		if ((schedule.getInterval() > 0) && ((endTime == null) || (time.before(endTime)))) {
			result = true;
		} else if ((schedule.getInterval() == 0) && (startTime != null)) {
			if (fuzzyStartTime) {
				Date fuzzyTime = new Date(time.getTime());
				fuzzyTime.setMinutes(time.getMinutes() - 5);
				if (fuzzyTime.before(startTime))
					result = true;
			} else if ((!fuzzyStartTime) && (time.before(startTime))) {
				result = true;
			}
		}
		LOG.debug("Check if can schedule:" + schedule + " time:" + time + " result:" + result + " fuzzy:" + fuzzyStartTime);

		return result;
	}
}
