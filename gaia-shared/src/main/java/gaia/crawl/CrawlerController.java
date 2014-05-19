package gaia.crawl;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import gaia.crawl.batch.BatchCrawlState;
import gaia.crawl.batch.BatchManager;
import gaia.crawl.batch.BatchRunner;
import gaia.crawl.batch.BatchStatus;
import gaia.crawl.datasource.DataSource;
import gaia.crawl.datasource.DataSourceId;
import gaia.crawl.resource.ResourceManager;
import gaia.crawl.security.SecurityFilter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public abstract class CrawlerController {
	private static transient Logger LOG = LoggerFactory.getLogger(CrawlerController.class);

	@Inject
	protected HistoryRecorder historyRecorder;
	protected CrawlStateManager jobStateMgr;
	protected DataSourceRegistry dsRegistry;
	protected BatchManager batchMgr = null;
	protected ResourceManager resourceMgr = null;
	protected Map<String, Boolean> closingCollection = Collections.synchronizedMap(new HashMap<String, Boolean>());
	protected volatile boolean closingAll = false;

	protected CrawlerController() {
		jobStateMgr = new CrawlStateManager();
		dsRegistry = new DataSourceRegistry(this);

		dsRegistry.addListener(new CrawlListener() {
			public void processEvent(CrawlEvent event) {
				String msg = getClass().getSimpleName() + " DS registry: " + event.getType() + " "
						+ event.getStatus();
				if (event.getMessage() != null) {
					msg = msg + " msg='" + event.getMessage() + "'";
				}
				if (event.getSource() != null) {
					if ((event.getSource() instanceof DataSource)) {
						DataSource ds = (DataSource) event.getSource();
						msg = msg + " " + ds.getCrawlerType() + "/" + ds.getType() + " id " + ds.getDataSourceId();
					} else {
						msg = msg + " " + event.getSource().toString();
					}
				}
				CrawlerController.LOG.info(msg);
			}
		});
		LOG.info(new StringBuilder().append(" - created DataSourceRegistry for ").append(getClass().getSimpleName())
				.toString());
	}

	public DataSourceRegistry getDataSourceRegistry() {
		return dsRegistry;
	}

	public abstract DataSourceFactory getDataSourceFactory();

	public boolean isClosing(String collection) {
		if (collection == null) {
			return closingAll;
		}
		if ((closingAll)
				|| ((closingCollection.containsKey(collection)) && (((Boolean) closingCollection.get(collection))
						.booleanValue()))) {
			return true;
		}
		return false;
	}

	public void setClosing(String collection, boolean value) {
		if (collection == null) {
			closingAll = value;
			if (!closingAll)
				closingCollection.clear();
		} else {
			closingCollection.put(collection, Boolean.valueOf(value));
		}
	}

	public synchronized void close() throws Exception {
		setClosing(null, true);
		LOG.info(new StringBuilder().append("Shutting down crawler controller ").append(getClass().getCanonicalName())
				.append(" with ").append(jobStateMgr.getJobStates().size()).append(" jobStates").toString());
		close(null, false);
		jobStateMgr.shutdown();
	}

	public synchronized List<CrawlId> close(String collection, boolean dryRun) throws Exception {
		List<CrawlId> runningJobs = new ArrayList<CrawlId>();
		List<CrawlId> ids = new ArrayList<CrawlId>();

		for (CrawlState s : jobStateMgr.getJobStates())
			if ((collection == null) || (collection.equals(s.getDataSource().getCollection()))) {
				if (s.getStatus().isRunning()) {
					runningJobs.add(s.getId());
				}
				ids.add(s.getId());
			}
		if (!runningJobs.isEmpty()) {
			if (!dryRun) {
				for (CrawlId id : ids)
					abortJob(id);
			} else {
				throw new JobStateException(runningJobs);
			}
		}
		if (dryRun) {
			return ids;
		}
		for (CrawlId id : ids) {
			removeJob(id);
		}
		LOG.info(new StringBuilder()
				.append("Closed resources of ")
				.append(getClass().getCanonicalName())
				.append(
						collection != null ? new StringBuilder().append(" for collection ").append(collection).toString()
								: " for all collections").append(", ").append(ids.size()).append(" states.").toString());

		return Collections.emptyList();
	}

	public abstract void reset(String paramString, DataSourceId paramDataSourceId) throws Exception;

	public abstract void resetAll(String paramString) throws Exception;

	public abstract CrawlId defineJob(DataSource paramDataSource, CrawlProcessor paramCrawlProcessor) throws Exception;

	public boolean removeJob(CrawlId id) throws CrawlException {
		CrawlState jobState = jobStateMgr.get(id);
		if (jobState != null) {
			assureJobNotRunning(jobState);
			jobState.close();
		}
		return jobStateMgr.delete(id);
	}

	public List<CrawlStatus> listJobs() throws Exception {
		List<CrawlStatus> res = new ArrayList<CrawlStatus>();
		for (CrawlState job : jobStateMgr.getJobStates()) {
			if (!(job instanceof BatchCrawlState)) {
				res.add(job.getStatus());
			}
		}
		return res;
	}

	public List<CrawlStatus> listBatchJobs() throws Exception {
		List<CrawlStatus> res = new ArrayList<CrawlStatus>();
		for (CrawlState job : jobStateMgr.getJobStates()) {
			if ((job instanceof BatchCrawlState)) {
				res.add(job.getStatus());
			}
		}
		return res;
	}

	public abstract void startJob(CrawlId paramCrawlId) throws Exception;

	public abstract void stopJob(CrawlId paramCrawlId) throws Exception;

	public abstract void abortJob(CrawlId paramCrawlId) throws Exception;

	public CrawlStatus getStatus(CrawlId id) throws Exception {
		CrawlState state = jobStateMgr.get(id);
		if (state == null) {
			return null;
		}
		return state.getStatus();
	}

	public boolean jobExists(CrawlId id) {
		return jobStateMgr.get(id) != null;
	}

	public boolean jobIsRunning(CrawlId id) {
		CrawlState state = jobStateMgr.get(id);
		if (state == null) {
			return false;
		}
		return state.getStatus().getState() == CrawlStatus.JobState.RUNNING;
	}

	public boolean jobIsActive(CrawlId id) {
		CrawlState state = jobStateMgr.get(id);
		if (state == null) {
			return false;
		}
		return state.getStatus().isRunning();
	}

	public CrawlState getCrawlState(CrawlId id) {
		CrawlState state = jobStateMgr.get(id);
		if (state == null) {
			return null;
		}
		return state;
	}

	protected boolean jobHasStarted(CrawlId id) throws Exception {
		CrawlState state = jobStateMgr.get(id);
		if (state == null) {
			return false;
		}
		return state.getStatus().getState() != CrawlStatus.JobState.IDLE;
	}

	public ResourceManager getResourceManager() {
		return resourceMgr;
	}

	public BatchManager getBatchManager() {
		return batchMgr;
	}

	public CrawlId startBatchJob(BatchStatus batch, DataSource template, CrawlProcessor processor, String newCollection,
			boolean parse, boolean index) throws Exception {
		BatchManager mgr = getBatchManager();
		if (mgr == null) {
			throw new Exception("This crawl controller does not support batch processing.");
		}
		if (newCollection != null) {
			batch.collection = newCollection;
		}
		assureNotClosing(batch.collection);
		if (template == null) {
			template = mgr.newDataSourceTemplate(this, batch.collection, batch.dsId);
		} else {
			DataSource newTemplate = new DataSource(template);
			template = newTemplate;
			if (newCollection != null) {
				template.setCollection(newCollection);
			}
		}
		template.setProperty("indexing", Boolean.valueOf(index));
		template.setProperty("parsing", Boolean.valueOf(parse));
		if (!parse) {
			template.setProperty("caching", Boolean.valueOf(false));
		}
		CrawlId cid = new CrawlId(batch.batchId);
		BatchCrawlState state = null;
		CrawlState st = jobStateMgr.get(cid);
		if (st != null) {
			if ((st instanceof BatchCrawlState)) {
				state = (BatchCrawlState) st;
				if (state.getStatus().isRunning()) {
					throw new Exception("Already running batch processing for this batch.");
				}
				jobStateMgr.delete(cid);
			} else {
				throw new Exception(new StringBuilder().append("Wrong type of CrawlState, expected BatchCrawlState, got ")
						.append(st.getClass().getName()).toString());
			}
		}
		state = new BatchCrawlState();
		state.setId(new CrawlId(batch.batchId));
		if (processor == null) {
			UpdateController update = UpdateController.create(this, template);
			processor = new TikaCrawlProcessor(update);
		}
		state.init(template, processor, historyRecorder);
		jobStateMgr.add(state);
		state.getStatus().starting();
		BatchRunner br = new BatchRunner(this, batch, processor, parse, index, state);
		Thread t = new Thread(br);
		t.start();
		return cid;
	}

	public SecurityFilter buildSecurityFilter(DataSource ds, String user) {
		return null;
	}

	protected void waitJob(CrawlId id) throws Exception {
		waitJob(id, -1, 500);
	}

	protected void waitJob(CrawlId id, int timeLimit) throws Exception {
		waitJob(id, timeLimit, 500);
	}

	protected void waitJob(CrawlId id, int timeLimit, int sleepInterval) throws Exception {
		long startTimeLimit = 5000L;
		waitJobStarted(id, startTimeLimit);
		CrawlStatus status = getStatus(id);
		CrawlStatus.JobState jobState = status.getState();
		if (!status.isRunning()) {
			LOG.debug(new StringBuilder().append("Job already finished without waiting: ").append(id).append(" state: ")
					.append(jobState).toString());
			if (jobState == CrawlStatus.JobState.EXCEPTION) {
				String message = new StringBuilder().append("Job aborted with exception: ").append(status.getException())
						.toString();
				LOG.error(message);

				return;
			}
			return;
		}
		long startTime = System.currentTimeMillis();
		long currentTime = System.currentTimeMillis();
		long endTime = startTime + timeLimit;

		while ((timeLimit <= 0) || (currentTime < endTime)) {
			jobState = status.getState();
			if (status.isRunning()) {
				LOG.debug(new StringBuilder().append("Job ").append(id).append(" state: ").append(jobState.toString())
						.toString());
			} else {
				LOG.debug(new StringBuilder().append("Job ").append(id).append(" completed with status ")
						.append(jobState.toString()).toString());
				if (jobState == CrawlStatus.JobState.EXCEPTION) {
					String message = new StringBuilder().append("Job aborted with exception: ").append(status.getException())
							.toString();
					LOG.error(message);

					return;
				}
				return;
			}
			LOG.debug(new StringBuilder().append("Sleeping for ").append(sleepInterval).append(" ms waiting for job ")
					.append(id).append(" to complete").toString());
			Thread.sleep(sleepInterval);
			currentTime = System.currentTimeMillis();
		}
		String message = new StringBuilder().append("Job ").append(id).append(" has timed out after ").append(timeLimit)
				.append(" ms").toString();
		LOG.error(message);
		throw new Exception(message);
	}

	protected void waitJobStarted(CrawlId id) throws Exception {
		waitJobStarted(id, 10000L);
	}

	protected void waitJobStarted(CrawlId id, long timeLimit) throws Exception {
		long startTime = System.currentTimeMillis();
		long currentTime = System.currentTimeMillis();
		long endTime = startTime + timeLimit;
		long sleepInterval = 100L;
		while ((timeLimit <= 0L) || (currentTime < endTime)) {
			if (jobHasStarted(id))
				return;
			LOG.debug(new StringBuilder().append("Sleeping for ").append(sleepInterval).append(" ms waiting for job ")
					.append(id).append(" to start").toString());
			Thread.sleep(sleepInterval);
			currentTime = System.currentTimeMillis();
		}
		String message = new StringBuilder().append("Job ").append(id).append(" did not start within ").append(timeLimit)
				.append(" ms").toString();
		LOG.error(message);
		throw new Exception(message);
	}

	protected final HistoryRecorder getHistoryRecorder() {
		return historyRecorder;
	}

	protected final void assureJobNotRunning(CrawlState state) throws JobStateException {
		if (state == null) {
			return;
		}
		// IDLE, STARTING, RUNNING, FINISHING, FINISHED, STOPPING, STOPPED,
		// ABORTING, ABORTED, EXCEPTION, UNKNOWN;
		switch (state.getStatus().getState()) { // FIXME: by whlee21
//		case IDLE:
//		case STARTING:
//		case FINISHED:
//		case STOPPING:
//		case STOPPED:
			case RUNNING:
				throw new JobStateException(state.getStatus().getState().toString(), Collections.singletonList(state.getId()));
		}
	}

	protected final void assureNotClosing(String collection) throws Exception {
		if (isClosing(collection))
			throw new Exception(new StringBuilder()
					.append(getClass().getSimpleName())
					.append(" is closing ")
					.append(
							collection != null ? new StringBuilder().append("collection '").append(collection).append("'.")
									.toString() : "all collections.").toString());
	}

	protected final void refreshDatasource(CrawlState state) throws Exception {
		DataSource ds = dsRegistry.getDataSource(state.getDataSource().getDataSourceId());

		if (ds == null) {
			throw new RuntimeException(new StringBuilder().append("Could not find DataSource: ").append(state.getId())
					.append(" ").append(dsRegistry.getDataSources(state.getDataSource().getCollection())).toString());
		}

		state.setDataSource(ds);
	}
}
