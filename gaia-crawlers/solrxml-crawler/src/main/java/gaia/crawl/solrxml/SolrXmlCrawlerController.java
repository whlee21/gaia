package gaia.crawl.solrxml;

import gaia.crawl.CrawlId;
import gaia.crawl.CrawlProcessor;
import gaia.crawl.CrawlState;
import gaia.crawl.CrawlStatus;
import gaia.crawl.CrawlerController;
import gaia.crawl.DataSourceFactory;
import gaia.crawl.batch.BatchCrawlState;
import gaia.crawl.batch.BatchManager;
import gaia.crawl.datasource.DataSource;
import gaia.crawl.datasource.DataSourceId;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SolrXmlCrawlerController extends CrawlerController {
	private static transient Logger LOG = LoggerFactory.getLogger(SolrXmlCrawlerController.class);

	private DataSourceFactory dsFactory = new SolrXmlDataSourceFactory(this);

	public SolrXmlCrawlerController() {
		batchMgr = BatchManager.create("gaia.solrxml", getClass().getClassLoader());
	}

	public DataSourceFactory getDataSourceFactory() {
		return dsFactory;
	}

	public void reset(String collection, DataSourceId dsId) throws Exception {
	}

	public void resetAll(String collection) throws Exception {
	}

	public CrawlId defineJob(DataSource ds, CrawlProcessor processor) throws Exception {
		assureNotClosing(ds.getCollection());
		SolrXmlCrawlState state = new SolrXmlCrawlState();
		state.init(ds, processor, historyRecorder);
		jobStateMgr.add(state);
		return state.getId();
	}

	public void startJob(CrawlId descrId) throws Exception {
		final SolrXmlCrawlState state = (SolrXmlCrawlState) jobStateMgr.get(descrId);
		if (state == null)
			throw new Exception("Unknown job id: " + descrId);
		assureJobNotRunning(state);
		assureNotClosing(state.getDataSource().getCollection());

		refreshDatasource(state);
		DataSource ds = state.getDataSource();

		state.getStatus().starting();

		SolrXmlCrawler.clearInterruptedStatus(ds);

		final SolrXmlCrawler xmlCrawler = new SolrXmlCrawler(this, ds, state.getStatus(), state.getProcessor());

		Runnable runJob = new Runnable() {
			public void run() {
				state.getStatus().running();
				try {
					xmlCrawler.crawl();
				} catch (Exception e) {
					LOG.error("exeception on job startup", e);
					state.getStatus().failed(e);
				}
				LOG.debug("return from run job " + state.getId());
			}
		};
		Thread thread = new Thread(runJob);
		thread.start();

		waitJobStarted(descrId);
	}

	public void stopJob(CrawlId jobId) throws Exception {
		CrawlState state = jobStateMgr.get(jobId);
		if (state == null) {
			throw new Exception("Unknown job id: " + jobId);
		}

		if (state.getStatus().getState() != CrawlStatus.JobState.RUNNING) {
			return;
		}

		if ((state instanceof SolrXmlCrawlState)) {
			state.getStatus().setState(CrawlStatus.JobState.STOPPING);
			DataSource ds = state.getDataSource();
			SolrXmlCrawler.interrupt(ds);
		} else if ((state instanceof BatchCrawlState)) {
			((BatchCrawlState) state).stop();
		}
	}

	public void abortJob(CrawlId id) throws Exception {
		stopJob(id);
	}
}
