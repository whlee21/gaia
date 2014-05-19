package com.lucid.crawl.behemoth;

import com.digitalpebble.behemoth.BehemothDocument;
import com.digitalpebble.behemoth.solr.LucidWorksOutputFormat;
import com.digitalpebble.behemoth.tika.DirectAccessTikaMapper;
import com.digitalpebble.behemoth.tika.TikaMapper;
import com.digitalpebble.behemoth.tika.TikaProcessor;
import com.digitalpebble.behemoth.util.CorpusGenerator;
import com.lucid.crawl.CrawlProcessor;
import com.lucid.crawl.CrawlStatus;
import com.lucid.crawl.CrawlStatus.JobState;
import com.lucid.crawl.UpdateController;
import com.lucid.crawl.datasource.DataSource;
import com.lucid.crawl.datasource.DataSourceId;
import com.lucid.utils.MasterConfUtil;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.UUID;
import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.FileInputFormat;
import org.apache.hadoop.mapred.FileOutputFormat;
import org.apache.hadoop.mapred.JobClient;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.OutputFormat;
import org.apache.hadoop.mapred.RunningJob;
import org.apache.hadoop.mapred.SequenceFileInputFormat;
import org.apache.hadoop.mapred.SequenceFileOutputFormat;
import org.apache.hadoop.mapred.lib.IdentityMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BehemothCrawler implements Runnable {
	static transient Logger log = LoggerFactory.getLogger(BehemothCrawler.class);
	protected BehemothCrawlState crawlState;
	protected DataSource dataSource;
	protected volatile boolean stopped = false;
	protected volatile boolean failed = false;
	protected Throwable failureThrowable = null;
	protected boolean directAccess;
	protected DirectAccessCorpusGenerator generator = null;

	public BehemothCrawler(BehemothCrawlState cs) {
		this.crawlState = cs;
		this.dataSource = this.crawlState.getDataSource();
	}

	public void run() {
		this.crawlState.getStatus().starting();
		try {
			this.crawlState.getProcessor().start();

			submitJob();
			if (this.failed) {
				if (this.failureThrowable != null) {
					throw this.failureThrowable;
				}
				throw new Exception("check connector and Hadoop logs");
			}
		} catch (Throwable t) {
			boolean commit;
			log.warn("Exception in Behemoth Crawler", t);
			this.crawlState.getStatus().failed(t);
			this.failed = true;
		} finally {
			boolean commit;
			boolean commit = this.dataSource.getBoolean("commit_on_finish", true);
			try {
				this.crawlState.getProcessor().finish();

				if (this.crawlState.getProcessor().getUpdateController().isStarted())
					this.crawlState.getProcessor().getUpdateController().finish(commit);
			} catch (Exception e) {
				e.printStackTrace();
				this.crawlState.getStatus().end(CrawlStatus.JobState.ABORTED);
			}
			if (!this.failed)
				if (this.stopped)
					this.crawlState.getStatus().end(CrawlStatus.JobState.STOPPED);
				else
					this.crawlState.getStatus().end(CrawlStatus.JobState.FINISHED);
		}
	}

	protected void submitJob() throws Exception {
		String pathLoc = this.dataSource.getString("path");
		Path input = new Path(pathLoc);
		this.directAccess = this.dataSource.getBoolean("direct_access", true);
		Path output = new Path(this.dataSource.getString("work_path", "/tmp"));
		Configuration conf = this.crawlState.createConfig(this);
		Thread.currentThread().setContextClassLoader(BehemothCrawler.class.getClassLoader());
		Path conversionDir;
		if (!this.stopped) {
			this.crawlState.getStatus().running();
			conversionDir = convertToBehemoth(conf, input, output, this.directAccess);
		} else {
			return;
		}
		Path conversionDir;
		if ((this.failed) || (this.stopped)) {
			return;
		}
		if (conversionDir == null) {
			this.failureThrowable = new Exception("phase 1 output doesn't exist: " + conversionDir.toUri());
			this.failed = true;
			return;
		}

		FileSystem outFS = conversionDir.getFileSystem(conf);
		boolean proceed = (outFS.getFileStatus(conversionDir).isDir()) || (outFS.exists(conversionDir));
		if (!proceed) {
			this.failureThrowable = new Exception("phase 1 output doesn't exist: " + conversionDir.toUri());
			this.failed = true;
			return;
		}
		Path tikaDir;
		if (!this.stopped) {
			tikaDir = tikaProcess(conf, conversionDir, output, this.directAccess);
		} else
			return;
		Path tikaDir;
		if ((this.failed) || (this.stopped)) {
			return;
		}

		proceed = (outFS.getFileStatus(tikaDir).isDir()) || (outFS.exists(tikaDir));

		if (!proceed) {
			this.failureThrowable = new Exception("phase 2 output doesn't exist: " + tikaDir.toUri());
			this.failed = true;
			return;
		}
		proceed = (this.crawlState.currentJob != null) && (this.crawlState.currentJob.isSuccessful());
		if (!proceed) {
			this.failureThrowable = new Exception("phase 2 didn't complete successfully, Hadoop job "
					+ this.crawlState.currentJob);
			this.failed = true;
			return;
		}

		if (!this.stopped) {
			conf = this.crawlState.createConfig(this);
			indexToLW(conf, tikaDir, output);
		} else
			;
	}

	protected Path convertToBehemoth(Configuration conf, Path input, Path parentOut, boolean direct)
			throws ClassNotFoundException, IOException, InterruptedException {
		if (direct)
			log.info("Preparing a list of paths from " + input + " at " + parentOut);
		else {
			log.info("Converting " + input + " to behemoth at " + parentOut);
		}
		FileSystem fs = input.getFileSystem(conf);
		if (!fs.exists(input)) {
			this.failed = true;
			this.failureThrowable = new Exception("Input path doesn't exist: " + input.makeQualified(fs).toUri());
			return null;
		}
		Path output = new Path(parentOut, createPathName("inputToBehemoth_"));
		HadoopUtils.delete(conf, new Path[] { output });
		Configuration behemothConf = new Configuration(conf);
		behemothConf.setClassLoader(getClass().getClassLoader());
		behemothConf.addResource("behemoth-default.xml");
		behemothConf.addResource("behemoth-site.xml");

		this.crawlState.currentTask = BehemothCrawlState.TASK.SEQ_FILE;

		boolean recurse = this.dataSource.getBoolean("recurse", true);
		long start = System.currentTimeMillis();
		long count = 0L;
		if (direct) {
			this.generator = new DirectAccessCorpusGenerator(input, output, this.crawlState);
			this.generator.setConf(behemothConf);
			count = this.generator.generate(recurse);
			if (this.generator.isStopped())
				return null;
		} else {
			CorpusGenerator cgenerator = new CorpusGenerator(input, output, this.crawlState);
			cgenerator.setConf(behemothConf);
			count = cgenerator.generate(recurse);
		}
		long finish = System.currentTimeMillis();

		log.info("Converted: " + count + " docs in " + (finish - start) + " ms");

		CrawlStatus s = this.crawlState.getStatus();
		log.info("Status: " + s.toString());
		return output;
	}

	protected String createPathName(String prefix) {
		return prefix + this.dataSource.getCollection() + "_" + this.dataSource.getDataSourceId() + "_"
				+ System.currentTimeMillis();
	}

	protected void indexToLW(Configuration conf, Path input, Path parentOut) throws IOException, ClassNotFoundException {
		log.info("Starting indexing process for " + input);
		JobConf job = new JobConf(conf);
		File jobFile = HadoopUtils.getJobFileByName("behemoth-lucidworks");
		if (jobFile != null) {
			job.setJar(jobFile.getAbsolutePath());
		} else {
			job.setJarByClass(TikaMapper.class);
		}

		job.setJobName("Indexing " + input + " into LucidWorks");
		job.setQuietMode(false);
		job.setInputFormat(SequenceFileInputFormat.class);

		String outClassStr = this.dataSource.getString("output_format");
		Class outClass;
		Class outClass;
		if (outClassStr != null)
			outClass = Class.forName(outClassStr).asSubclass(OutputFormat.class);
		else {
			outClass = LucidWorksOutputFormat.class;
		}
		job.setOutputFormat(outClass);

		job.setOutputKeyClass(Text.class);
		job.setOutputValueClass(BehemothDocument.class);

		job.setMapperClass(IdentityMapper.class);

		job.setNumReduceTasks(0);

		FileInputFormat.addInputPath(job, input);

		Path tmp = new Path(parentOut, createPathName("indexOut_"));
		FileSystem fs = tmp.getFileSystem(conf);
		FileOutputFormat.setOutputPath(job, tmp);
		String zkHost = this.dataSource.getString("zookeeper_host");
		if (zkHost == null) {
			zkHost = System.getProperty("zkHost");
		}
		if ((zkHost != null) && (zkHost.trim().length() > 0)) {
			job.set("solr.zkhost", this.dataSource.getString("zookeeper_host"));

			job.set("solr.zk.collection", this.dataSource.getCollection());
		} else {
			String solrServer = this.dataSource.getString("solr_server_url");
			if ((solrServer == null) || (solrServer.trim().length() == 0)) {
				solrServer = this.dataSource.getString("output_args");
				if (solrServer != null) {
					String[] ss = solrServer.split(",");
					solrServer = null;
					for (String s : ss) {
						if (s.trim().matches("^http(s)?://.*")) {
							solrServer = s.trim();
							break;
						}
					}
				}
				if ((solrServer == null) || (solrServer.trim().length() == 0)) {
					try {
						URL u = MasterConfUtil.getSolrAddress(false, this.dataSource.getCollection());
						solrServer = u.toString();
					} catch (Exception e) {
						log.error("Could not obtain Solr address - jobs will likely fail: " + e.toString());
					}
				}
			}
			job.set("solr.server.url", solrServer);
		}

		commonJobConfig(job);
		long start = System.currentTimeMillis();
		CrawlStatus s = null;
		try {
			job.setClassLoader(getClass().getClassLoader());
			JobClient jobC = new JobClient(job);
			RunningJob runningJob = jobC.submitJob(job);
			this.crawlState.currentJob = runningJob;
			this.crawlState.currentTask = BehemothCrawlState.TASK.INDEX;
			runningJob.waitForCompletion();
		} catch (Exception e) {
			log.error("Error in indexing content to LucidWorks", e);
			this.failed = true;
			this.failureThrowable = e;
		} finally {
			s = this.crawlState.getStatus();
			if (outClass.equals(LucidWorksOutputFormat.class)) {
				fs.delete(tmp, true);
			}
			if ((this.crawlState.currentJob != null) && (!this.failed) && (!this.crawlState.currentJob.isSuccessful())
					&& (!this.stopped)) {
				this.failureThrowable = new Exception(this.crawlState.currentJob.getFailureInfo());
			}

		}

		long finish = System.currentTimeMillis();
		log.info("Finished indexing " + input + " in " + (finish - start) + " ms");
		log.info("Status: " + s.toString());
	}

	private void commonJobConfig(JobConf job) {
		job.set("solr.params", "fm.ds=" + this.dataSource.getDataSourceId().toString());

		job.setBoolean("lw.metadata", this.dataSource.getBoolean("add_metadata"));
		job.setBoolean("tika.metadata", this.dataSource.getBoolean("add_metadata"));

		job.setStrings("lw.batch.id", new String[] { UUID.randomUUID().toString() });
		job.setBoolean("lw.annotations", this.dataSource.getBoolean("add_annotations"));
		job.setBoolean("tika.annotations", this.dataSource.getBoolean("add_annotations"));

		String anns = this.dataSource.getString("annotations");
		if (!StringUtils.isBlank(anns))
			job.set("lw.annotations.list", anns.trim());
	}

	protected Path tikaProcess(Configuration conf, Path input, Path outputParent, boolean direct) throws IOException {
		log.info("Starting Tika conversion process for " + input);
		JobConf job = new JobConf(conf);
		Path output = new Path(outputParent, createPathName("tikaOut_"));
		HadoopUtils.delete(conf, new Path[] { output });
		File jobFile = HadoopUtils.getJobFileByName("behemoth-tika");
		if (jobFile != null) {
			job.setJar(jobFile.getAbsolutePath());
		} else if (direct)
			job.setJarByClass(DirectAccessTikaMapper.class);
		else {
			job.setJarByClass(TikaMapper.class);
		}

		commonJobConfig(job);
		String knownMimeType = this.dataSource.getString("mime_type");
		if ((knownMimeType != null) && (!knownMimeType.equals(""))) {
			job.set("tika.mime.type", knownMimeType);
		}
		String processorClass = this.dataSource.getString("tika_content_handler", TikaProcessor.class.getName());
		job.set("tika.processor", processorClass);

		job.setJobName("Processing " + input + " with Tika");

		job.setInputFormat(SequenceFileInputFormat.class);
		job.setOutputFormat(SequenceFileOutputFormat.class);

		job.setMapOutputKeyClass(Text.class);
		job.setMapOutputValueClass(BehemothDocument.class);
		job.setOutputKeyClass(Text.class);
		job.setOutputValueClass(BehemothDocument.class);

		if (direct) {
			job.setMapperClass(DirectAccessTikaMapper.class);

			FileSystem fs = input.getFileSystem(job);
			FileStatus stat = fs.getFileStatus(input);

			float numBlocks = (float) stat.getLen() / (float) stat.getBlockSize();
			int numMaps = Math.round(numBlocks * 30.0F) + 1;
			job.setNumMapTasks(numMaps);
		} else {
			job.setMapperClass(TikaMapper.class);
		}

		job.setNumReduceTasks(0);

		FileInputFormat.addInputPath(job, input);
		FileOutputFormat.setOutputPath(job, output);

		long start = System.currentTimeMillis();
		CrawlStatus s = null;
		try {
			job.setClassLoader(getClass().getClassLoader());

			JobClient jobC = new JobClient(job);
			RunningJob runningJob = jobC.submitJob(job);
			this.crawlState.currentJob = runningJob;
			this.crawlState.currentTask = BehemothCrawlState.TASK.TIKA;
			runningJob.waitForCompletion();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			s = this.crawlState.getStatus();
			if ((this.crawlState.currentJob != null) && (!this.failed) && (!this.crawlState.currentJob.isSuccessful())
					&& (!this.stopped)) {
				this.failureThrowable = new Exception(this.crawlState.currentJob.getFailureInfo());
			}

		}

		long finish = System.currentTimeMillis();

		log.info("Finished Tika conversion of " + input + " to " + output + " in " + (finish - start) + " ms");
		log.info("Status: " + s.toString());
		return output;
	}

	public synchronized void stop() throws IOException {
		this.stopped = true;
		if (this.crawlState.currentJob != null) {
			this.crawlState.currentJob.killJob();
		}
		if (this.generator != null)
			this.generator.stop();
	}
}
