package gaia.scheduler;

import gaia.admin.collection.CollectionManager;
import gaia.admin.collection.GaiaSolrParams;
import gaia.admin.collection.ScheduledSolrCommand;
import gaia.admin.collection.SolrCmdHistory;
import gaia.crawl.ConnectorManager;
import gaia.crawl.JobStateException;
import gaia.crawl.datasource.DataSource;
import gaia.crawl.datasource.DataSourceId;
import gaia.utils.StringUtils;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.common.util.SimpleOrderedMap;
import org.apache.solr.core.CoreContainer;
import org.apache.solr.core.SolrCore;
import org.apache.solr.request.LocalSolrQueryRequest;
import org.apache.solr.request.SolrRequestHandler;
import org.apache.solr.response.SolrQueryResponse;
import org.quartz.InterruptableJob;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.SchedulerException;
import org.quartz.StatefulJob;
import org.quartz.UnableToInterruptJobException;
import org.restlet.data.Status;
import org.restlet.resource.ResourceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;

public class SolrJob implements StatefulJob, InterruptableJob {
	private static transient Logger LOG = LoggerFactory.getLogger(SolrJob.class);
	private static final String NUM_FAILED_KEY = "solr_job_num_failed";
	private static final String FAILED_MSG_KEY = "solr_job_failed_message";
	protected volatile SolrJobInterruptable interruptable = new SolrJobInterruptable();
	private CoreContainer cores;
	private SolrCmdHistory cmdHistory;
	private ConnectorManager crawlerManager;
	private CollectionManager cm;
	private SolrScheduler scheduler;

	@Inject
	public SolrJob(CoreContainer cores, CollectionManager cm, ConnectorManager crawlerManager, SolrScheduler scheduler,
			SolrCmdHistory cmdHistory) {
		this.cores = cores;
		this.cmdHistory = cmdHistory;
		this.crawlerManager = crawlerManager;
		this.scheduler = scheduler;
		this.cm = cm;
	}

	public void interrupt() throws UnableToInterruptJobException {
		if (interruptable != null) {
			interruptable.setInterrupted(true);
		}
	}

	public void execute(JobExecutionContext context) throws JobExecutionException {
		try {
			doExecute(context);
		} catch (Exception e) {
			LOG.error("", e);
		}
	}

	private void failWithDeactivateCheck(JobDataMap jobData, Exception e, String message, boolean disableNow)
			throws JobExecutionException {
		Object Failed = jobData.get(NUM_FAILED_KEY);
		int numFailed = 0;
		if ((Failed != null) && ((Failed instanceof Number))) {
			numFailed = ((Number) Failed).intValue();
		}
		numFailed++;
		jobData.put(NUM_FAILED_KEY, numFailed);
		if (message != null) {
			jobData.put(FAILED_MSG_KEY, message);
		}
		if ((numFailed > 2) || (disableNow)) {
			Object collectionO = jobData.get("coreName");
			Object idO = jobData.get("jobId");
			String collection = null;
			String id = null;
			ScheduledSolrCommand cmd = null;
			if ((collectionO != null) && (idO != null)) {
				if ((collectionO instanceof Object[]))
					collection = String.valueOf(((Object[]) (Object[]) collectionO)[0]);
				else {
					collection = String.valueOf(collectionO);
				}
				if ((idO instanceof Object[]))
					id = String.valueOf(((Object[]) (Object[]) idO)[0]);
				else
					id = String.valueOf(idO);
				try {
					cmd = cm.getScheduledSolrCommand(collection, id);
				} catch (Exception ex) {
					LOG.warn(new StringBuilder().append("Unable to retrieve scheduled command id ").append(id).append(": ")
							.append(ex).toString());
				}
			}
			if (id != null) {
				try {
					scheduler.delete(id);
				} catch (SchedulerException e1) {
					LOG.warn(new StringBuilder().append("Unable to delete the already scheduled job ").append(id).append(": ")
							.append(e1).toString());
				}
			}
			if (cmd != null) {
				LOG.warn(new StringBuilder().append("Disabling scheduled command ").append(id).toString());
				cmd.getSchedule().setActive(false);
				GaiaSolrParams params = cmd.getParams();
				params.set(NUM_FAILED_KEY, new String[] { String.valueOf(numFailed) });
				if (message != null) {
					params.set(FAILED_MSG_KEY, new String[] { message });
				}
				cmd.setParams(params);
				cm.updateScheduledSolrCommand(collection, cmd, id);
			} else {
				LOG.warn(new StringBuilder().append("Can't find scheduled command to disable schedule for ")
						.append(toString(jobData)).toString());
			}
		}
		if (e != null) {
			throw new JobExecutionException(message, e);
		}
		throw new JobExecutionException(message);
	}

	private void resetDeactivateCheck(JobDataMap jobData) {
		jobData.remove(NUM_FAILED_KEY);
		jobData.remove(FAILED_MSG_KEY);
	}

	private boolean isDisabled(JobDataMap jobData) {
		Object Failed = jobData.get(NUM_FAILED_KEY);
		if (Failed == null) {
			return false;
		}
		if ((Failed != null) && ((Failed instanceof Number))) {
			int numFailed = ((Number) Failed).intValue();
			if (numFailed > 2) {
				return true;
			}
		}
		return false;
	}

	private void doExecute(JobExecutionContext context) throws JobExecutionException {
		String instName = context.getJobDetail().getName();
		String instGroup = context.getJobDetail().getGroup();
		if (LOG.isInfoEnabled()) {
			LOG.info(new StringBuilder().append("Executing job: ").append(instName).append(" Group: ").append(instGroup)
					.toString());
		}

		JobDataMap dataMapOriginal = context.getJobDetail().getJobDataMap();
		JobDataMap dataMap = (JobDataMap) dataMapOriginal.clone();
		if (isDisabled(dataMap)) {
			LOG.info(new StringBuilder().append("Skipping due to previous failures: ").append(toString(dataMap)).toString());
			return;
		}

		String[] coreNames = (String[]) dataMap.get("coreName");
		if (coreNames == null) {
			failWithDeactivateCheck(dataMapOriginal, null, "coreName cannot be null", true);
		}

		String coreName = coreNames[0];

		String handlerName = (String) dataMap.get("jobHandler");

		if ("--crawl--".equals(handlerName)) {
			doCrawl(coreName, dataMapOriginal);
			return;
		}

		LocalSolrQueryRequest request = null;

		SolrCore core = cores.getCore(coreName);
		if (core == null) {
			failWithDeactivateCheck(dataMapOriginal, null, new StringBuilder().append("SolrJob could not find SolrCore:")
					.append(coreName).toString(), true);
		}

		long start = System.currentTimeMillis();
		long finish = start;
		ModifiableSolrParams params = new ModifiableSolrParams(dataMap);
		String jobId = dataMap.getString("jobId");
		SolrRequestHandler handler = core.getRequestHandler(handlerName);
		if (handler == null) {
			core.close();
			failWithDeactivateCheck(dataMapOriginal, null, new StringBuilder().append("Couldn't find request handler:")
					.append(handlerName).toString(), true);
		}
		try {
			dataMap.remove("jobHandler");
			dataMap.remove("jobId");

			request = new LocalSolrQueryRequest(core, params);

			dataMap.remove("streams");
			SolrQueryResponse response = new SolrQueryResponse();
			response.add("responseHeader", new SimpleOrderedMap());
			if (!interruptable.isInterrupted()) {
				handler.handleRequest(request, response);
				finish = System.currentTimeMillis();
				if (LOG.isInfoEnabled()) {
					LOG.info(new StringBuilder().append("Scheduled Job for handler: ").append(handlerName).append(" instance: ")
							.append(instName).append(" took ").append(finish - start).append(" ms").toString());
				}
			}

			context.setResult(response);
		} catch (Exception e) {
			failWithDeactivateCheck(dataMapOriginal, e, e.getMessage(), false);
		} finally {
			if (request != null) {
				request.close();
			}
			core.close();
		}
		resetDeactivateCheck(dataMapOriginal);
		if (LOG.isInfoEnabled()) {
			LOG.info(new StringBuilder().append("Done executing job: ").append(instName).append(" Group: ").append(instGroup)
					.toString());
		}

		String trackHistory = params.get("trackHistory");
		if ((!interruptable.isInterrupted()) && (jobId != null)
				&& ((trackHistory == null) || (trackHistory.equals("true")))) {
			HashMap<String, Object> historyMap = new HashMap<String, Object>();
			historyMap.put("activity_started", StringUtils.formatDate(new Date(start)));

			historyMap.put("activity_finished", StringUtils.formatDate(new Date(finish)));

			cmdHistory.addHistory(jobId, historyMap);
			cmdHistory.save();
		}
	}

	private void doCrawl(String coreName, JobDataMap dataMap) throws JobExecutionException {
		String jobId = dataMap.getString("jobId");

		String dsIdString = ((String[]) (String[]) dataMap.get("id"))[0];
		DataSourceId dsId = new DataSourceId(dsIdString);
		DataSource ds = null;
		try {
			ds = crawlerManager.getDataSource(dsId);
		} catch (Exception e1) {
			failWithDeactivateCheck(dataMap, e1, new StringBuilder().append("Exception retrieving data source ").append(dsId)
					.toString(), false);
		}

		if (ds == null) {
			LOG.warn(new StringBuilder().append("Could not find DataSource, ignoring scheduled crawl: ").append(dsId)
					.toString());
			failWithDeactivateCheck(dataMap, null,
					new StringBuilder().append("Could not find DataSource, disabling scheduled crawl: ").append(dsId).toString(),
					true);
		}

		if (!interruptable.isInterrupted()) {
			long start = System.currentTimeMillis();
			try {
				crawlerManager.crawl(dsId);
			} catch (JobStateException ex) {
				LOG.info(new StringBuilder().append("Wrong job state, crawl request ignored (").append(ex.getMessage())
						.append(")").toString());
				return;
			} catch (ResourceException re) {
				Status s = re.getStatus();
				if ((s != null) && (s.getDescription().contains("gaia.crawl.JobStateException:"))) {
					LOG.info(new StringBuilder().append("Wrong job state, crawl request ignored: ").append(s).toString());
					return;
				}
				failWithDeactivateCheck(dataMap, re, new StringBuilder().append("crawl: ").append(dsId).toString(), false);
			} catch (Exception e) {
				failWithDeactivateCheck(dataMap, e, new StringBuilder().append("crawl: ").append(dsId).toString(), false);
			}
			long finish = System.currentTimeMillis();
			resetDeactivateCheck(dataMap);
			if (LOG.isInfoEnabled()) {
				LOG.info(new StringBuilder().append("Scheduled Job for data source: ").append(dsId).append(" took ")
						.append(finish - start).append(" ms").toString());
			}

		}

		if (LOG.isInfoEnabled())
			LOG.info(new StringBuilder().append("Done executing job: ").append(dsId).append("/").append(jobId).toString());
	}

	private static String toString(JobDataMap map) {
		if (map == null) {
			return "(null)";
		}
		StringBuilder sb = new StringBuilder();
		Map<Object, Object> m = map.getWrappedMap();
		for (Map.Entry<Object, Object> e : m.entrySet()) {
			if (sb.length() > 0)
				sb.append(",\n");
			else {
				sb.append("{\n");
			}
			sb.append(new StringBuilder().append("\t\"").append(e.getKey()).append("\" : ").toString());
			Object v = e.getValue();
			if (v == null) {
				sb.append("null");
			} else if ((v instanceof Object[])) {
				Object[] vv = (Object[]) v;
				sb.append("[");
				for (int i = 0; i < vv.length; i++) {
					if (i > 0)
						sb.append(", ");
					sb.append(new StringBuilder().append("\"").append(String.valueOf(vv[i])).append("\"").toString());
				}
				sb.append("]");
			} else {
				sb.append(new StringBuilder().append("\"").append(String.valueOf(v)).append("\"").toString());
			}
		}
		sb.append("\n}");
		return sb.toString();
	}
}
