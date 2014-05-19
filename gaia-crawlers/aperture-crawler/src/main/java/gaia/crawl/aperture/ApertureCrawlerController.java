package gaia.crawl.aperture;

import gaia.crawl.CrawlId;
import gaia.crawl.CrawlProcessor;
import gaia.crawl.CrawlState;
import gaia.crawl.CrawlStatus;
import gaia.crawl.CrawlerController;
import gaia.crawl.DataSourceFactory;
import gaia.crawl.HistoryRecorder;
import gaia.crawl.JobStateException;
import gaia.crawl.batch.BatchCrawlState;
import gaia.crawl.batch.BatchManager;
import gaia.crawl.datasource.DataSource;
import gaia.crawl.datasource.DataSourceId;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Injector;

public class ApertureCrawlerController extends CrawlerController {
	private static transient Logger LOG = LoggerFactory.getLogger(ApertureCrawlerController.class);

	@Inject
	private Injector injector;
	protected DataSourceFactory dsFactory = new ApertureDataSourceFactory(this);
	private ApertureRepos apertureRepos;

	@Inject
	public ApertureCrawlerController(ApertureRepos repos, HistoryRecorder historyRecorder) {
		this.apertureRepos = repos;
		this.historyRecorder = historyRecorder;
		this.batchMgr = BatchManager.create("gaia.aperture", getClass().getClassLoader());
	}

	List<CrawlState> getJobStates() {
		return jobStateMgr.getJobStates();
	}

	public DataSourceFactory getDataSourceFactory() {
		return dsFactory;
	}

	public void reset(String collection, DataSourceId dsId) throws Exception {
		DataSource ds = dsRegistry.getDataSource(dsId);

		if (ds == null) {
			LOG.error("Could not find DataSource: " + dsId);
			throw new RuntimeException("Could not find DataSource: " + dsId);
		}

		apertureRepos.clearRepositoryForUrl(ds);
	}

	public void resetAll(String collection) throws Exception {
		setClosing(collection, true);
		try {
			List<CrawlId> runningIds = new ArrayList<CrawlId>();
			for (CrawlState job : jobStateMgr.getJobStates()) {
				if ((job.getDataSource().getCollection().equals(collection)) && (jobIsRunning(job.getId()))) {
					runningIds.add(job.getId());
				}
			}

			if (!runningIds.isEmpty()) {
				throw new JobStateException("Can't reset collection '" + collection + "' with jobs running", runningIds);
			}

			for (CrawlState job : jobStateMgr.getJobStates()) {
				if (job.getDataSource().getCollection().equals(collection)) {
					job.close();
				}
			}
			apertureRepos.clearRepository(collection);
		} finally {
			setClosing(collection, false);
		}
	}

	public CrawlId defineJob(DataSource ds, CrawlProcessor processor) throws Exception {
		assureNotClosing(ds.getCollection());
		CrawlState crawlState = jobStateMgr.get(new CrawlId(ds.getDataSourceId()));
		if (crawlState == null) {
			crawlState = (CrawlState) injector.getInstance(ApertureCrawlState.class);
		}
		crawlState.init(ds, processor, historyRecorder);
		jobStateMgr.add(crawlState);

		return crawlState.getId();
	}

	public void startJob(CrawlId crawlJobId) throws Exception {
		final CrawlState state = jobStateMgr.get(crawlJobId);
		if (state == null) {
			throw new Exception("Unknown job id: " + crawlJobId);
		}
		assureJobNotRunning(state);
		assureNotClosing(state.getDataSource().getCollection());

		state.getStatus().reset();

		refreshDatasource(state);

		Runnable runJob = new Runnable() {
			public void run() {
				LOG.debug("run job " + state.getId());
				try {
					startJob(state);
				} catch (Exception e) {
					LOG.error("exeception on job startup: " + e.getMessage());
					state.getStatus().failed(e);
				}
				LOG.debug("return from run job " + state.getId());
			}
		};
		Thread thread = new Thread(runJob);
		thread.start();

		waitJobStarted(crawlJobId);
	}

	private void startJob(CrawlState state) {
		CrawlStatus crawlStatus = state.getStatus();

		crawlStatus.starting();
		try {
			CrawlProcessor processor = state.getProcessor();
			if (processor != null) {
				processor.start();
			}
			ApertureCrawler apertureDriver = new ApertureCrawler(this, (ApertureCrawlState) state, dsFactory
					.getRestrictedTypes().contains(state.getDataSource().getType()));

			apertureDriver.crawl();
		} catch (Throwable e) {
			crawlStatus.failed(e);
		}
	}

	public void stopJob(CrawlId jobId) throws Exception {
		CrawlState state = jobStateMgr.get(jobId);
		if (state == null) {
			throw new Exception("Unknown job id: " + jobId);
		}

		if (state.getStatus().getState() != CrawlStatus.JobState.RUNNING) {
			return;
		}
		if ((state instanceof ApertureCrawlState))
			((ApertureCrawlState) state).stop();
		else if ((state instanceof BatchCrawlState))
			((BatchCrawlState) state).stop();
	}

	public void abortJob(CrawlId jobId) throws Exception {
		CrawlState state = jobStateMgr.get(jobId);
		if (state == null) {
			throw new Exception("Unknown job id: " + jobId);
		}

		if (state.getStatus().getState() != CrawlStatus.JobState.RUNNING) {
			return;
		}
		if ((state instanceof ApertureCrawlState))
			((ApertureCrawlState) state).abort();
		else if ((state instanceof BatchCrawlState))
			((BatchCrawlState) state).stop();
	}
}
