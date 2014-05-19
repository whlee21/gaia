package gaia.crawl.dih;

import gaia.crawl.CrawlState;
import gaia.crawl.CrawlStatus;

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DIHCrawlState extends CrawlState {
	private static transient Logger LOG = LoggerFactory.getLogger(DIHCrawlState.class);
	private DIHImporter importer;

	public void crawl() {
		status.starting();
		try {
			processor.start();

			String dir = DIHCrawlerController.DS_DIR + File.separator + ds.getDataSourceId().getId();
			importer = new DIHImporter(dir);
			importer.init();
			importer.copyJdbcJars(ds.getCollection());
			importer.generateDihConfigs(ds);
			importer.startEmbeddedSolrServer();

			Thread t = new Thread("dihCrawl " + ds.getDisplayName() + " " + ds.getDataSourceId().getId()) {
				public void run() {
					try {
						status.running();

						DIHStatus res = importer.runImport(ds, null);
						DIHCrawlState.LOG.info(res.toString());
						// FIXME: by whlee21
						// IDLE, STARTING, RUNNING, FINISHING, FINISHED, STOPPING, STOPPED,
						// ABORTING, ABORTED, EXCEPTION, UNKNOWN
						switch (status.getState()) {
						case STARTING:
							status.end(CrawlStatus.JobState.STOPPED);
							break;
						case RUNNING:
							status.end(CrawlStatus.JobState.ABORTED);
							break;
						default:
							if (res.getStatus() == DIHStatus.Status.FAILED)
								status.failed(new RuntimeException("Data import handler returned failed status: " + res.getError()));
							else
								status.end(CrawlStatus.JobState.FINISHED);
							break;
						}
					} catch (Exception e) {
						status.failed(e);
					} finally {
						try {
							processor.finish();
						} catch (Exception e) {
						}
						importer.shutdown();
					}
				}
			};
			t.start();
		} catch (Throwable e) {
			status.failed(e);
		}
	}

	public void stop() throws Exception {
		if (!status.isRunning()) {
			return;
		}

		if (importer != null) {
			status.setState(CrawlStatus.JobState.STOPPING);
			importer.stopImport(ds);
		}
	}
}
