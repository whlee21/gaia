package gaia.crawl.external;

import gaia.crawl.CrawlEvent;
import gaia.crawl.CrawlEvent.Status;
import gaia.crawl.CrawlEvent.Type;
import gaia.crawl.CrawlId;
import gaia.crawl.CrawlListener;
import gaia.crawl.CrawlProcessor;
import gaia.crawl.CrawlState;
import gaia.crawl.CrawlStateManager;
import gaia.crawl.CrawlStatus;
import gaia.crawl.CrawlerController;
import gaia.crawl.DataSourceFactory;
import gaia.crawl.DataSourceRegistry;
import gaia.crawl.batch.BatchCrawlState;
import gaia.crawl.datasource.DataSource;
import gaia.crawl.datasource.DataSourceId;
import java.util.LinkedList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExternalCrawlerController extends CrawlerController {
	private static transient Logger log = LoggerFactory.getLogger(ExternalCrawlerController.class);

	private DataSourceFactory dsFactory = new ExternalDataSourceFactory(this);

	public ExternalCrawlerController() {
		this.dsRegistry.addListener(new CrawlListener() {
			public void processEvent(CrawlEvent event) {
				try {
					if ((CrawlEvent.Type.ADD.toString().equals(event.getType()))
							&& (CrawlEvent.Status.OK.toString().equals(event.getStatus()))) {
						defineJob((DataSource) event.getSource(), null);
					} else if ((CrawlEvent.Type.UPDATE.toString().equals(event.getType()))
							&& (CrawlEvent.Status.OK.toString().equals(event.getStatus()))) {
						startJobInternal(new CrawlId(((DataSource) event.getSource()).getDataSourceId()), false);
					} else if ((CrawlEvent.Type.REMOVE.toString().equals(event.getType()))
							&& (CrawlEvent.Status.OK.toString().equals(event.getStatus()))) {
						stopJob(new CrawlId(((DataSource) event.getSource()).getDataSourceId()));
					} else if ((CrawlEvent.Type.REMOVE_MULTI.toString().equals(event.getType()))
							&& (CrawlEvent.Status.OK.toString().equals(event.getStatus()))) {
						List dss = (List) event.getSource();
						for (DataSource ds : dss)
							stopJob(new CrawlId(ds.getDataSourceId()));
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	public DataSourceFactory getDataSourceFactory() {
		return this.dsFactory;
	}

	public void reset(String collection, DataSourceId dsId) throws Exception {
		CrawlId cid = new CrawlId(dsId);
		ExternalCrawlState state = (ExternalCrawlState) this.jobStateMgr.get(cid);
		if (state == null) {
			return;
		}
		stopJob(cid);
		this.jobStateMgr.delete(cid);
	}

	public synchronized void resetAll(String collection) throws Exception {
		List ids = new LinkedList();
		for (CrawlState state : this.jobStateMgr.getJobStates())
			if ((collection == null) || (collection.equals(state.getDataSource().getCollection()))) {
				if ((state instanceof ExternalCrawlState)) {
					stopJob(state.getId());
					ids.add(state.getId());
				}
			}
		for (CrawlId id : ids)
			this.jobStateMgr.delete(id);
	}

	public CrawlId defineJob(DataSource ds, CrawlProcessor processor) throws Exception {
		assureNotClosing(ds.getCollection());
		ExternalCrawlState state = new ExternalCrawlState();
		state.init(ds, processor, this.historyRecorder);
		this.jobStateMgr.add(state);

		startJobInternal(state.getId(), false);
		return state.getId();
	}

	public void startJob(CrawlId descrId) throws Exception {
		startJobInternal(descrId, true);
	}

	private void startJobInternal(CrawlId descrId, final boolean runCallback) throws Exception {
		final ExternalCrawlState state = (ExternalCrawlState) this.jobStateMgr.get(descrId);
		if (state == null)
			throw new Exception("Unknown job id: " + descrId);
		assureNotClosing(state.getDataSource().getCollection());

		if (state.getStatus().isRunning()) {
			stopJob(descrId);
		}

		refreshDatasource(state);
		final DataSource ds = state.getDataSource();

		Runnable runJob = new Runnable() {
			public void run() {
				state.getStatus().starting();
				ExternalCrawler externalCrawler = new ExternalCrawler(this.this$0, ds, state);
				state.crawler = externalCrawler;
				try {
					externalCrawler.crawl(runCallback);
				} catch (Exception e) {
					log.error("exception on job startup", e);
					state.getStatus().failed(e);
				}
				log.debug("return from run job " + state.getId());
			}
		};
		Thread thread = new Thread(runJob);
		thread.start();

		waitJobStarted(descrId);
	}

	public void stopJob(CrawlId jobId) throws Exception {
		CrawlState state = this.jobStateMgr.get(jobId);
		if (state == null) {
			throw new Exception("Unknown job id: " + jobId);
		}
		if ((state instanceof ExternalCrawlState)) {
			ExternalCrawlState estate = (ExternalCrawlState) state;
			if (estate.crawler != null)
				estate.crawler.stop();
		} else if ((state instanceof BatchCrawlState)) {
			((BatchCrawlState) state).stop();
		}
	}

	public void abortJob(CrawlId id) throws Exception {
		stopJob(id);
	}

	public synchronized List<CrawlId> close(String collection, boolean dryRun) throws Exception {
		for (CrawlState state : this.jobStateMgr.getJobStates()) {
			if ((collection == null) || (collection.equals(state.getDataSource().getCollection()))) {
				if ((state instanceof ExternalCrawlState)) {
					ExternalCrawlState estate = (ExternalCrawlState) state;
					if (estate.crawler != null) {
						estate.crawler.stop();
					}
				} else if (!(state instanceof BatchCrawlState))
					;
			}
		}
		return super.close(collection, dryRun);
	}
}
