package gaia.crawl.gaialogs;

import gaia.Defaults;
import gaia.crawl.CrawlStatus;
import gaia.crawl.UpdateController;
import gaia.crawl.datasource.DataSource;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class LogCrawlerRunner implements Runnable {
	private static transient Logger log = LoggerFactory.getLogger(LogCrawlerRunner.class);
	public static final String CORE_LOG_NAME_PATTERN = "core\\.[\\d_]+\\.log";
	public static final String CON_LOG_NAME_PATTERN = "connectors\\.[\\d_]+\\.log";
	private static final String[] CRAWLER_PATTERNS = { "core\\.[\\d_]+\\.log", "connectors\\.[\\d_]+\\.log" };
	private final String nodeName;
	private final CrawlStatus status;
	private final File dir;
	private final UpdateController solr;
	private final List<LogCrawler> fileCrawlers;
	private boolean runUntilStopped = false;
	private boolean stopRequested = false;
	private DataSource ds;

	public void setDataSource(DataSource ds) {
		this.ds = ds;
	}

	public boolean getRunUntilStopped() {
		return runUntilStopped;
	}

	public void setRunUntilStopped(boolean runUntilStopped) {
		this.runUntilStopped = runUntilStopped;
		for (LogCrawler fc : fileCrawlers)
			fc.setRunUntilStopped(runUntilStopped);
	}

	public void requestStop() {
		stopRequested = true;
		for (LogCrawler fc : fileCrawlers)
			fc.requestStop();
	}

	public LogCrawlerRunner(String nodeName, CrawlStatus status, File logDir, UpdateController solr) {
		this.nodeName = nodeName;
		this.status = status;
		this.solr = solr;
		this.dir = logDir;

		LogCrawlerCounter counter = new LogCrawlerCounter(status);
		fileCrawlers = new ArrayList<LogCrawler>(CRAWLER_PATTERNS.length);
		for (String pat : CRAWLER_PATTERNS)
			fileCrawlers.add(new LogCrawler(nodeName, counter, logDir, pat, solr));
	}

	public String toString() {
		return getClass().getSimpleName() + " [" + status.toString() + "]";
	}

	public synchronized void run() {
		if (null == ds) {
			Exception e = new IllegalStateException("No datasource set on crawler for run");
			status.failed(e);
			log.error("GaiaSearchLogs crawler state failure", e);
			return;
		}
		boolean commit;
		try {
			status.running();
			solr.start();
			log.debug("Starting Crawl: " + toString());
			try {
				if (ds.getLong("delete_after", -1L) > 0L) {
					long ms = ds.getLong("delete_after");
					if (ms > Integer.MAX_VALUE)
						solr.deleteByQuery("lastModified:[* TO NOW-" + ms / 1000L + "SECONDS]");
					else
						solr.deleteByQuery("lastModified:[* TO NOW-" + ms + "MILLIS]");
				}
			} catch (Exception e) {
				log.error("Unable to delete old log docs", e);
				throw new RuntimeException("Unable to delete old log docs", e);
			}

			List<Callable<Object>> actions = new ArrayList<Callable<Object>>(fileCrawlers.size());

			for (LogCrawler fc : fileCrawlers) {
				actions.add(Executors.callable(fc));
			}
			ExecutorService service = Executors.newFixedThreadPool(actions.size());

			service.invokeAll(actions);
			service.shutdown();

			status.end(stopRequested ? CrawlStatus.JobState.STOPPED : CrawlStatus.JobState.FINISHED);
		} catch (Exception e) {
			status.failed(e);
			log.error("Failed to run LogCrawlerRunner", e);
		} finally {
			log.debug("Ending Crawl: " + toString());
			commit = ds.getBoolean("commit_on_finish",
					Defaults.INSTANCE.getBoolean(Defaults.Group.datasource, "commit_on_finish"));
			try {
				solr.finish(commit);
			} catch (Exception e) {
				log.error("Failed to finish LogCrawlerRunner", e);
			}
		}
	}
}
