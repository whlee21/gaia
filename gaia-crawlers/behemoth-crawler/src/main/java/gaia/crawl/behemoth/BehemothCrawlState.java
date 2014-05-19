package gaia.crawl.behemoth;

import gaia.crawl.CrawlState;
import gaia.crawl.CrawlStatus;
import gaia.crawl.datasource.DataSource;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;

import org.apache.hadoop.conf.Configuration;
//import org.apache.hadoop.mapred.Task;
import org.apache.hadoop.mapreduce.Counters;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.TaskCounter;
//import org.apache.hadoop.mapred.Counters.Counter;
//import org.apache.hadoop.mapred.InputSplit;
//import org.apache.hadoop.mapred.Reporter;
//import org.apache.hadoop.mapred.RunningJob;
//import org.apache.hadoop.mapred.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
//import org.apache.hadoop.mapred.Reporter;
//import org.apache.hadoop.mapred.RunningJob;
//import org.apache.hadoop.mapred.Task;

import com.digitalpebble.behemoth.util.CorpusGenerator;

public class BehemothCrawlState extends CrawlState {
	private static final Logger LOG = LoggerFactory.getLogger(BehemothCrawlState.class);

	BehemothCrawler crawler = null;
	Job currentJob = null;
	protected String reporterStatus;
	long seqFileRecords;
	long tikaRecords;
	long indexRecords;
	boolean directAccess;
	TASK currentTask = TASK.NONE;
	Thread t = null;

	public synchronized void start() throws Exception {
		if ((t != null) && (t.isAlive())) {
			throw new Exception("already running");
		}
		crawler = new BehemothCrawler(this);
		status.reset();
		// counters = new Counters();
		currentJob = null;
		currentTask = TASK.NONE;
		t = new Thread(crawler);
		t.setDaemon(true);
		t.start();
	}

	public synchronized void setDataSource(DataSource ds) throws Exception {
		if ((t != null) && (t.isAlive())) {
			throw new Exception("job is running");
		}
		t = null;
		crawler = new BehemothCrawler(this);
//		counters = new Counters();
		status.reset();
		directAccess = ds.getBoolean("direct_access");
		super.setDataSource(ds);
	}

	public synchronized void stop() throws Exception {
		if ((t == null) || (!t.isAlive()) || (crawler == null)) {
			throw new Exception("job is not running");
		}
		crawler.stop();
	}

	public CrawlStatus getStatus() {
		try {
			if (currentJob != null) {
				seqFileRecords = currentJob.getCounters().findCounter(CorpusGenerator.Counters.DOC_COUNT).getValue();
			} else {
				seqFileRecords = 0;
			}
		} catch (Exception e) {
			if (currentJob != null) {
				LOG.warn("Unable to obtain counters from the running job " + currentJob.getTrackingURL(), e);
				status.setMessage("Unable to obtain counters from the running job Hadoop ID: " + currentJob.getJobID());
			}
			return status;
		}
		switch (currentTask) {
		case SEQ_FILE:
			status.setCounter(CrawlStatus.Counter.New, seqFileRecords);
			if (directAccess)
				status.setMessage("Preparing a list of files (" + seqFileRecords + " docs)");
			else {
				status.setMessage("Converting to sequence files (" + seqFileRecords + " docs)");
			}
			break;
		case TIKA:
			if (currentJob != null) {
				status.setMessage("Extracting content.  Hadoop ID: " + currentJob.getJobID());
				try {
					Counters currentCounters = currentJob.getCounters();
					long newCnt = currentCounters.findCounter("TIKA", "DOC-PARSED").getValue();
					newCnt += currentCounters.findCounter("TIKA", "TEXT ALREADY AVAILABLE").getValue();
					long failedCnt = currentCounters.findCounter("TIKA", "PARSING_ERROR").getValue();
					failedCnt += currentCounters.findCounter("TIKA", "DOC-NO_DATA").getValue();
					long filteredCnt = currentCounters.findCounter("TIKA", "FILTERED-CONTENT-LENGTH").getValue();
					filteredCnt += currentCounters.findCounter("TIKA", "FILTERED-CONTENT-TYPE").getValue();
					status.setCounter(CrawlStatus.Counter.New, newCnt);
					status.setCounter(CrawlStatus.Counter.Failed, failedCnt);
					status.setCounter(CrawlStatus.Counter.Filter_Denied, filteredCnt);
				} catch (Exception e) {
					LOG.warn("Unable to obtain counters from the running job " + currentJob.getTrackingURL(), e);
				}
			}
			break;
		case INDEX:
			if (currentJob != null) {
				long eligible = status.getCounter(CrawlStatus.Counter.New) + status.getCounter(CrawlStatus.Counter.Failed);
				try {
					Counters cnt = currentJob.getCounters();
					long newCnt = cnt.findCounter(TaskCounter.MAP_OUTPUT_RECORDS).getValue();
					status.setMessage("Indexing to GaiaSearch (" + newCnt + " of " + eligible + ").  Hadoop ID: "
							+ currentJob.getJobID());
				} catch (Exception e) {
					LOG.warn("Unable to obtain counters from the running job " + currentJob.getTrackingURL(), e);
					status.setMessage("Indexing to GaiaSearch (? of " + eligible + ").  Hadoop ID: " + currentJob.getJobID());
				}
			}
			break;
		case NONE:
		}

		return status;
	}

	public Configuration createConfig(BehemothCrawler behemothCrawler) throws Exception {
		ClassLoader ctxCl = Thread.currentThread().getContextClassLoader();
		try {
			Thread.currentThread().setContextClassLoader(behemothCrawler.getClass().getClassLoader());

			Configuration res = new Configuration();
			res.setClassLoader(behemothCrawler.getClass().getClassLoader());
			String hadoopConfDir = behemothCrawler.dataSource.getString("hadoop_conf");
			String jobTracker;
			if (hadoopConfDir != null) {
				File confDir = new File(hadoopConfDir);
				if ((confDir.exists()) && (confDir.isDirectory())) {
					File[] files = confDir.listFiles(new FilenameFilter() {
						public boolean accept(File file, String name) {
							return (name.equals("core-site.xml")) || (name.equals("mapred-site.xml"))
									|| (name.equals("hdfs-site.xml"));
						}
					});
					for (int i = 0; i < files.length; i++) {
						File file = files[i];
						LOG.info("Adding Hadoop resource: " + file.getAbsolutePath());
						res.addResource(file.toURI().toURL());
					}
				} else {
					throw new IOException("Couldn't find Hadoop configuration directory");
				}
			} else {
				jobTracker = behemothCrawler.dataSource.getString("job_tracker");
				if (jobTracker != null) {
					res.set("mapred.job.tracker", jobTracker);
				}
				String fsName = behemothCrawler.dataSource.getString("fs_default_name");
				if (fsName != null) {
					res.set("fs.default.name", fsName);
				}
			}
			res.set("fs.hdfs.impl", "org.apache.hadoop.hdfs.DistributedFileSystem");
			return res;
		} finally {
			Thread.currentThread().setContextClassLoader(ctxCl);
		}
	}

	public static enum TASK {
		NONE, SEQ_FILE, TIKA, INDEX;
	}
}
