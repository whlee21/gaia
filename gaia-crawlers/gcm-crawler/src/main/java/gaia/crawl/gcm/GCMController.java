package gaia.crawl.gcm;

import com.google.inject.Inject;
import gaia.crawl.CrawlException;
import gaia.crawl.CrawlId;
import gaia.crawl.CrawlProcessor;
import gaia.crawl.CrawlState;
import gaia.crawl.CrawlStateManager;
import gaia.crawl.CrawlStatus;
import gaia.crawl.CrawlStatus.JobState;
import gaia.crawl.CrawlerController;
import gaia.crawl.CrawlerUtils;
import gaia.crawl.DataSourceFactory;
import gaia.crawl.DataSourceRegistry;
import gaia.crawl.TikaCrawlProcessor;
import gaia.crawl.UpdateController;
import gaia.crawl.batch.BatchCrawlState;
import gaia.crawl.batch.BatchManager;
import gaia.crawl.datasource.DataSource;
import gaia.crawl.datasource.DataSourceId;
import gaia.crawl.gcm.api.CMResponse;
import gaia.crawl.gcm.api.ConfigureResponse;
import gaia.crawl.gcm.api.ConnectorStatus;
import gaia.crawl.gcm.api.RemoteGCMServer;
import gaia.crawl.gcm.api.Schedule;
import gaia.crawl.security.AclProcessor;
import gaia.crawl.security.BasicAclProcessor;
import gaia.crawl.security.Principal;
import gaia.crawl.security.SecurityFilter;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GCMController extends CrawlerController {
	private static final String DEFAULT_TIME_INTERVALS = "0-24";
	private static final Long DEFAULT_RETRY_DELAY = Long.valueOf(-1L);
	private static final Integer DEFAULT_LOAD = Integer.valueOf(6000);

	private static final Schedule DEFAULT_SCHEDULE = new Schedule(Boolean.valueOf(false), DEFAULT_LOAD,
			DEFAULT_RETRY_DELAY, "0-24");

	private static final Schedule STOP_SCHEDULE = new Schedule(Boolean.valueOf(false), Integer.valueOf(0),
			DEFAULT_RETRY_DELAY, "0-24");

	private static final Logger LOG = LoggerFactory.getLogger(GCMController.class);
	public static RemoteGCMServer client;
	private DataSourceFactory dsFactory = null;
	private StatusPoller poller;
	private EmbeddedJettyRunner runner;
	private Thread pollerThread;

	@Inject
	public GCMController() {
		try {
			LOG.info("Starting embedded jetty");
			this.runner = new EmbeddedJettyRunner(this);
			this.runner.start();
		} catch (Throwable t) {
			LOG.error("GCMCrawling disabled, error starting embedded jetty: " + t, t);
			return;
		}

		this.dsFactory = new GCMDataSourceFactory(this);
		this.batchMgr = BatchManager.create("gaia.gcm", getClass().getClassLoader());
		client = this.runner.getGCMServer();

		this.poller = new StatusPoller(this, client, this.jobStateMgr);
		this.pollerThread = new Thread(this.poller);
		this.pollerThread.setDaemon(true);
		this.pollerThread.start();
	}

	private void initJobInternal(DataSource datasource) {
		try {
			String id = datasource.getDataSourceId().toString();
			LOG.debug("Checking schedule for:" + id);
			ConnectorStatus status = client.getConnectorStatus(id);
			if (status.getStatusId() == 0) {
				LOG.debug("status code OK!");
			} else if (status.getStatusId() == 5303) {
				return;
			}

			LOG.debug("Schedule: " + status.getSchedule());
			if (!status.getSchedule().isDisabled()) {
				CrawlId cid = new CrawlId(datasource.getDataSourceId());
				CrawlStatus crawlStatus = getStatus(cid);
				if (crawlStatus == null) {
					UpdateController updateController = UpdateController.create(this, datasource);

					defineJob(datasource, new TikaCrawlProcessor(updateController));
				}
				LOG.info("Initializing active job for DS: " + id);
				startJob(new CrawlId(id));
			}
			LOG.info("Job was not active for DS: " + id);
		} catch (Throwable t) {
			LOG.error("Could not init internal job: " + datasource, t);
		}
	}

	public DataSourceFactory getDataSourceFactory() {
		return this.dsFactory;
	}

	public boolean removeJob(CrawlId id) throws CrawlException {
		LOG.debug("Remove job: + id");
		try {
			CMResponse response = client.removeConnector(id.toString());
			if (response.getStatusId() != 0) {
				LOG.warn("Could not remove crawl configuration from GCM, job id=" + id + ", code:" + response.getStatusId());

				return false;
			}
			int n = 10;

			for (int i = 0; i < n; i++) {
				try {
					ConnectorStatus status = client.getConnectorStatus(id.toString());
					if (5411 == status.getStatus()) {
						super.removeJob(id);
						return true;
					}
				} catch (IOException ioe) {
					LOG.warn("Could not read crawl status from GCM, job id=" + id + ", GCM webapp not available?: "
							+ ioe.getClass() + ":" + ioe.getMessage());

					return false;
				}
				try {
					Thread.sleep(1000L);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
			}
			return false;
		} catch (IOException ioe) {
			LOG.warn("Could not remove crawl configuration from GMC, job id=" + id + ", GCM webapp not available?: "
					+ ioe.getClass() + ":" + ioe.getMessage());
		}
		return false;
	}

	public void startJob(CrawlId id) throws Exception {
		LOG.debug("Start job:" + id);

		GCMCrawlState state = (GCMCrawlState) this.jobStateMgr.get(id);
		if (state == null) {
			throw new Exception("Unknown job id: " + id);
		}
		assureJobNotRunning(state);
		assureNotClosing(state.getDataSource().getCollection());

		state.getStatus().starting();

		refreshDatasource(state);

		state.getProcessor().start();

		Map params = LWEGCMAdaptor.getAdaptor(state.getDataSource().getType()).getGCMProperties(
				state.getDataSource().getProperties());

		boolean existing = false;
		for (ConnectorStatus instance : client.getConnectorStatuses()) {
			if (instance.getName().equals(state.getId().toString())) {
				existing = true;
				break;
			}
		}
		String connectorType = (String) params.get("connectorType");
		try {
			try {
				ConfigureResponse response = client.setConnectorConfig(state.getId().toString(), connectorType, params,
						"en_EN", existing);

				if (response.getStatusId() != 0) {
					failJob(
							"Could not create Connector configuration (code:" + response.getStatusId() + "): "
									+ response.getMessage(), state, null);

					return;
				}
			} catch (SocketTimeoutException e) {
				LOG.warn("Socket timeout exception while setting gcm connector config", e);
			}
			try {
				CMResponse cmResponse = client.setSchedule(state.getId().toString(), DEFAULT_SCHEDULE);

				if (cmResponse.getStatusId() != 0) {
					failJob("Could not create Schedule for connector: (code:" + cmResponse.getStatusId() + ")", state, null);

					return;
				}
			} catch (SocketTimeoutException e) {
				LOG.warn("Socket timeout exception while setting gcm connector schedule", e);
			}
		} catch (Throwable t) {
			failJob("Unexpected error occured when starting the crawl:" + t.getMessage(), state, t);

			return;
		}
		state.getStatus().running();
	}

	public SecurityFilter buildSecurityFilter(DataSource ds, String user) {
		boolean enabled = LWEGCMAdaptor.getAdaptor(ds.getType()).useAuthnSpiForDocumentsSecurity(ds.getProperties());
		if (!enabled) {
			return null;
		}
		GcmEntitlementCollector collector = new GcmEntitlementCollector(client, ds.getDataSourceId().toString());

		List entitlements = null;
		if (user != null) {
			Principal principal = new Principal(user, GcmEntitlementCollector.PrincipalType.USER.toString());
			entitlements = collector.getEntitlementsForPrincipal(principal);
		}

		AclProcessor aclProcessor = new BasicAclProcessor("acl", "GCM");
		return aclProcessor.buildSearchFilter(entitlements);
	}

	private void failJob(String msg, GCMCrawlState state, Throwable throwable) {
		LOG.error(CrawlerUtils.msgDocFailed(state.getDataSource().getCollection(), state.getDataSource(), msg,
				throwable != null ? throwable.getMessage() : null));

		state.getStatus().setMessage(msg);
		state.getStatus().failed(throwable);
	}

	public void stopJob(CrawlId id) throws Exception {
		LOG.debug("Stop job:" + id);

		CrawlState job = this.jobStateMgr.get(id);
		if (job == null) {
			throw new Exception("Could not find job with id " + id);
		}
		if ((job instanceof GCMCrawlState)) {
			if ((job.getStatus().getState() == CrawlStatus.JobState.STARTING)
					|| (job.getStatus().getState() == CrawlStatus.JobState.RUNNING)) {
				LOG.info("Actually making call");
				try {
					CMResponse response = client.stopTraversal(id.toString());
					if (response.getStatusId() == 0) {
						LOG.info("Succesfully set status of job " + id + " to STOPPING.");
						job.getStatus().setState(CrawlStatus.JobState.STOPPING);
						this.poller.addStoppedJob(id.toString());
					} else {
						LOG.warn("Could not stop GMC crawl, job id=" + id + ", error code: " + response.getStatusId());

						job.getStatus().setState(CrawlStatus.JobState.UNKNOWN);
					}
				} catch (IOException ioe) {
					LOG.warn("Could not stop GMC crawl, job id=" + id + ", GCM webapp not available?: " + ioe.getClass() + ":"
							+ ioe.getMessage());
				}
			}
		} else if ((job instanceof BatchCrawlState))
			((BatchCrawlState) job).stop();
	}

	public void abortJob(CrawlId id) throws Exception {
		stopJob(id);
	}

	public CrawlId defineJob(DataSource ds, CrawlProcessor processor) throws Exception {
		assureNotClosing(ds.getCollection());
		GCMCrawlState state = new GCMCrawlState();
		state.init(ds, processor, this.historyRecorder);
		this.jobStateMgr.add(state);
		return state.getId();
	}

	public void reset(String collection, DataSourceId dsId) {
		try {
			String id = dsId.toString();

			CMResponse response = client.stopTraversal(id.toString());
			if (response.getStatusId() == 0) {
				response = client.removeConnector(id);
				if (response.getStatusId() == 0)
					LOG.info("Persistent data for datasource " + dsId + " removed.");
			} else {
				LOG.warn("Cannot remove persistent data, because stopping failed with status code: " + response.getStatusId());
			}
		} catch (Throwable e) {
			LOG.warn("Cannot remove persistent data because: " + e.getMessage(), e);
		}
	}

	public void resetAll(String collection) {
		List dsList = this.dsRegistry.getDataSources(collection);
		for (DataSource datasource : dsList)
			if ("gaia.gcm".equals(datasource.getCrawlerType()))
				reset(collection, datasource.getDataSourceId());
	}

	private void restoreInternal() {
		List dsList = this.dsRegistry.getDataSources(null);
		for (DataSource datasource : dsList)
			if ("gaia.gcm".equals(datasource.getCrawlerType()))
				try {
					initJobInternal(datasource);
				} catch (Exception e) {
					LOG.error("Cannot initialize internal job state for datasource:" + datasource, e);
				}
	}

	public void close() throws Exception {
		try {
			if (client != null) {
				super.close();
				LOG.info("Closing gcm client.");
				client.close();
				client = null;
			}
			if (this.poller != null) {
				LOG.info("Closing poller");
				this.poller.close();
				this.poller = null;
			}
			if (this.pollerThread != null) {
				LOG.info("Interrupting poller thread");
				this.pollerThread.interrupt();
				this.pollerThread = null;
			}
		} finally {
			if (this.runner != null) {
				LOG.info("Stoppping embedded jetty");
				this.runner.stop();
				this.runner = null;
			}
		}
	}

	private static class StatusPoller implements Runnable {
		private final CrawlStateManager mgr;
		private final RemoteGCMServer gcm;
		private final GCMController controller;
		private volatile boolean keepRunning = true;
		private Set<String> stoppedJobs = new HashSet();

		private StatusPoller(GCMController controller, RemoteGCMServer gcm, CrawlStateManager mgr) {
			this.mgr = mgr;
			this.gcm = gcm;
			this.controller = controller;
		}

		public void addStoppedJob(String id) {
			this.stoppedJobs.add(id);
		}

		public void run() {
			this.controller.restoreInternal();
			GCMController.LOG.info("Internal state restored.");

			while (this.keepRunning) {
				try {
					Thread.sleep(5000L);
				} catch (InterruptedException e) {
					GCMController.LOG.info("Sleep interrupted, exiting");
					return;
				}
				for (CrawlState state : this.mgr.getJobStates())
					if ((state.getStatus().getState() == CrawlStatus.JobState.RUNNING)
							|| (state.getStatus().getState() == CrawlStatus.JobState.STOPPING))
						try {
							String id = state.getId().toString();
							ConnectorStatus status = this.gcm.getConnectorStatus(id);
							if ((status.getSchedule() != null) && (status.getSchedule().isDisabled())) {
								GCMController.LOG.info("Crawl " + id + " is done.");
								if (this.stoppedJobs.contains(id)) {
									state.getStatus().end(CrawlStatus.JobState.STOPPED);
									this.stoppedJobs.remove(id);
								} else {
									state.getStatus().end(CrawlStatus.JobState.FINISHED);
								}
							}
						} catch (Throwable e) {
							GCMController.LOG.warn("Cannot retrieve status for datasource", e);
						}
			}
		}

		public void close() {
			this.keepRunning = false;
		}
	}
}
