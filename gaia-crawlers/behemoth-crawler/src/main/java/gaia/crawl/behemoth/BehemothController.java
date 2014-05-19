package gaia.crawl.behemoth;

import gaia.crawl.CrawlId;
import gaia.crawl.CrawlProcessor;
import gaia.crawl.CrawlStatus;
import gaia.crawl.CrawlerController;
import gaia.crawl.DataSourceFactory;
import gaia.crawl.batch.BatchManager;
import gaia.crawl.datasource.DataSource;
import gaia.crawl.datasource.DataSourceId;

public class BehemothController extends CrawlerController {
	protected DataSourceFactory dsFactory;

	public BehemothController() {
		batchMgr = BatchManager.create("gaia.map.reduce.hdfs", getClass().getClassLoader());
		dsFactory = new BehemothDataSourceFactory(this);
	}

	public DataSourceFactory getDataSourceFactory() {
		return dsFactory;
	}

	public void reset(String collection, DataSourceId dsId) throws Exception {
	}

	public void resetAll(String collection) throws Exception {
	}

	public CrawlId defineJob(DataSource ds, CrawlProcessor processor) throws Exception {
		CrawlId result = new CrawlId(ds.getDataSourceId());
		BehemothCrawlState state = new BehemothCrawlState();
		state.init(ds, processor, historyRecorder);
		jobStateMgr.add(state);
		return result;
	}

	public void startJob(CrawlId descrId) throws Exception {
		BehemothCrawlState state = (BehemothCrawlState) jobStateMgr.get(descrId);
		if (state == null) {
			throw new Exception("Unknown job id: " + descrId);
		}
		assureJobNotRunning(state);
		assureNotClosing(state.getDataSource().getCollection());
		refreshDatasource(state);
		state.start();
	}

	public void stopJob(CrawlId jobId) throws Exception {
		BehemothCrawlState state = (BehemothCrawlState) jobStateMgr.get(jobId);
		if (state == null) {
			throw new Exception("Unknown job id: " + jobId);
		}

		if (state.getStatus().getState() != CrawlStatus.JobState.RUNNING) {
			return;
		}

		state.stop();
	}

	public void abortJob(CrawlId id) throws Exception {
		stopJob(id);
	}
}
