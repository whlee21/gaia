package gaia.crawl.gaialogs;

import gaia.crawl.CrawlEvent;
import gaia.crawl.CrawlId;
import gaia.crawl.CrawlListener;
import gaia.crawl.CrawlProcessor;
import gaia.crawl.CrawlStatus;
import gaia.crawl.CrawlerController;
import gaia.crawl.DataSourceFactory;
import gaia.crawl.DataSourceRegistry;
import gaia.crawl.TikaCrawlProcessor;
import gaia.crawl.UpdateController;
import gaia.crawl.datasource.DataSource;
import gaia.crawl.datasource.DataSourceId;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;

public class LogCrawlerController extends CrawlerController {
	private static transient Logger log = LoggerFactory.getLogger(LogCrawlerController.class);

	private DataSourceFactory dsFactory = new LogDataSourceFactory(this);
	private String nodeName;

	@Inject
	public LogCrawlerController() {
		dsRegistry = new DataSourceRegistry(this) {
			public synchronized boolean addDataSource(DataSource ds) throws Exception {
				if (getDataSources(null).size() > 0) {
					fireListeners(new CrawlEvent(CrawlEvent.Type.ADD.toString(), CrawlEvent.Status.FAIL.toString(),
							"already got " + getDataSources(null).size() + " ds", ds));

					return false;
				}
				return super.addDataSource(ds);
			}
		};
		dsRegistry.addListener(new CrawlListener() {
			public void processEvent(CrawlEvent event) {
				log.info("DS registry: " + event);
			}
		});
		try {
			nodeName = InetAddress.getLocalHost().getHostName();
		} catch (UnknownHostException e) {
			nodeName = "amnesiac";
		}
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
		LogCrawlState state = new LogCrawlState(nodeName);
		if (processor == null) {
			processor = new TikaCrawlProcessor(UpdateController.create(this, ds));
		}
		state.init(ds, processor, historyRecorder);
		jobStateMgr.add(state);
		return state.getId();
	}

	public void startJob(CrawlId descrId) throws Exception {
		LogCrawlState state = (LogCrawlState) jobStateMgr.get(descrId);
		if (state == null)
			throw new Exception("Unknown job id: " + descrId);
		assureJobNotRunning(state);
		assureNotClosing(state.getDataSource().getCollection());
		state.getStatus().starting();

		LogCrawlerRunner crawler = state.getCrawler();

		refreshDatasource(state);
		crawler.setDataSource(state.getDataSource());

		Thread thread = new Thread(crawler);
		thread.start();

		waitJobStarted(descrId);
	}

	public void stopJob(CrawlId jobId) throws Exception {
		LogCrawlState state = (LogCrawlState) jobStateMgr.get(jobId);
		if (state == null) {
			throw new Exception("Unknown job id: " + jobId);
		}

		if (state.getStatus().getState() != CrawlStatus.JobState.RUNNING) {
			return;
		}

		state.getCrawler().requestStop();
	}

	public void abortJob(CrawlId id) throws Exception {
		stopJob(id);
	}
}
