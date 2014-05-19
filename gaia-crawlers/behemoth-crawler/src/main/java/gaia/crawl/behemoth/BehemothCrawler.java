package gaia.crawl.behemoth;

import gaia.crawl.CrawlStatus;
import gaia.crawl.datasource.DataSource;
import gaia.solr.behemoth.GaiaSearchOutputFormat;
import gaia.solr.behemoth.IdentityMapper;
import gaia.utils.MasterConfUtil;

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
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.OutputFormat;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.SequenceFileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.SequenceFileOutputFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.digitalpebble.behemoth.BehemothDocument;
import com.digitalpebble.behemoth.tika.DirectAccessTikaMapper;
import com.digitalpebble.behemoth.tika.TikaMapper;
import com.digitalpebble.behemoth.tika.TikaProcessor;
import com.digitalpebble.behemoth.util.CorpusGenerator;
import com.google.inject.Inject;
import com.google.inject.name.Named;

public class BehemothCrawler implements Runnable {
	static transient Logger LOG = LoggerFactory.getLogger(BehemothCrawler.class);
	protected BehemothCrawlState crawlState;
	protected DataSource dataSource;
	protected volatile boolean stopped = false;
	protected volatile boolean failed = false;
	protected Throwable failureThrowable = null;
	protected boolean directAccess;
	protected DirectAccessCorpusGenerator generator = null;

	public BehemothCrawler(BehemothCrawlState cs) {
		crawlState = cs;
		dataSource = crawlState.getDataSource();
	}

	public void run() {
		crawlState.getStatus().starting();
		try {
			crawlState.getProcessor().start();
			submitJob();
			if (failed) {
				if (failureThrowable != null) {
					throw failureThrowable;
				}
				throw new Exception("check connector and Hadoop logs");
			}
		} catch (Throwable t) {
			LOG.warn("Exception in Behemoth Crawler", t);
			crawlState.getStatus().failed(t);
			failed = true;
		} finally {
			boolean commit = dataSource.getBoolean("commit_on_finish", true);
			try {
				crawlState.getProcessor().finish();

				if (crawlState.getProcessor().getUpdateController().isStarted())
					crawlState.getProcessor().getUpdateController().finish(commit);
			} catch (Exception e) {
				e.printStackTrace();
				crawlState.getStatus().end(CrawlStatus.JobState.ABORTED);
			}
			if (!failed)
				if (stopped)
					crawlState.getStatus().end(CrawlStatus.JobState.STOPPED);
				else
					crawlState.getStatus().end(CrawlStatus.JobState.FINISHED);
		}
	}

	protected void submitJob() throws Exception {
		String pathLoc = dataSource.getString("path");
		Path input = new Path(pathLoc);
		directAccess = dataSource.getBoolean("direct_access", true);
		Path output = new Path(dataSource.getString("work_path", "/tmp"));
		Configuration conf = crawlState.createConfig(this);
		Thread.currentThread().setContextClassLoader(BehemothCrawler.class.getClassLoader());
		Path conversionDir;
		if (!stopped) {
			crawlState.getStatus().running();
			conversionDir = convertToBehemoth(conf, input, output, directAccess);
		} else {
			return;
		}
		if ((failed) || (stopped)) {
			return;
		}
		if (conversionDir == null) {
			// failureThrowable = new Exception("phase 1 output doesn't exist: " +
			// conversionDir.toUri());
			failureThrowable = new Exception("phase 1 output doesn't exist");
			failed = true;
			return;
		}

		FileSystem outFS = conversionDir.getFileSystem(conf);
		boolean proceed = (outFS.getFileStatus(conversionDir).isDirectory()) || (outFS.exists(conversionDir));
		if (!proceed) {
			failureThrowable = new Exception("phase 1 output doesn't exist: " + conversionDir.toUri());
			failed = true;
			return;
		}
		Path tikaDir;
		if (!stopped) {
			tikaDir = tikaProcess(conf, conversionDir, output, directAccess);
		} else
			return;
		if ((failed) || (stopped)) {
			return;
		}

		proceed = (outFS.getFileStatus(tikaDir).isDirectory()) || (outFS.exists(tikaDir));

		if (!proceed) {
			failureThrowable = new Exception("phase 2 output doesn't exist: " + tikaDir.toUri());
			failed = true;
			return;
		}
		proceed = (crawlState.currentJob != null) && (crawlState.currentJob.isSuccessful());
		if (!proceed) {
			failureThrowable = new Exception("phase 2 didn't complete successfully, Hadoop job " + crawlState.currentJob);
			failed = true;
			return;
		}

		if (!stopped) {
			conf = crawlState.createConfig(this);
			indexToGaia(conf, tikaDir, output);
		}
	}

	protected Path convertToBehemoth(Configuration conf, Path input, Path parentOut, boolean direct)
			throws ClassNotFoundException, IOException, InterruptedException {
		if (direct)
			LOG.info("Preparing a list of paths from " + input + " at " + parentOut);
		else {
			LOG.info("Converting " + input + " to behemoth at " + parentOut);
		}
		Job job = new Job(conf);
		FileSystem fs = input.getFileSystem(conf);
		if (!fs.exists(input)) {
			failed = true;
			failureThrowable = new Exception("Input path doesn't exist: "
					+ input.makeQualified(fs.getUri(), fs.getWorkingDirectory()).toUri());
			return null;
		}
		Path output = new Path(parentOut, createPathName("inputToBehemoth_"));
		HadoopUtils.delete(conf, new Path[] { output });
		Configuration behemothConf = new Configuration(conf);
		behemothConf.setClassLoader(getClass().getClassLoader());
		behemothConf.addResource("behemoth-default.xml");
		behemothConf.addResource("behemoth-site.xml");

		crawlState.currentTask = BehemothCrawlState.TASK.SEQ_FILE;

		boolean recurse = dataSource.getBoolean("recurse", true);
		long start = System.currentTimeMillis();
		long count = 0L;
		if (direct) {
			generator = new DirectAccessCorpusGenerator(input, output, crawlState);
			generator.setConf(behemothConf);
			count = generator.generate(recurse);
			if (generator.isStopped())
				return null;
		} else {
			CorpusGenerator cgenerator = new CorpusGenerator(input, output, job.getCounters());
			cgenerator.setConf(behemothConf);
			count = cgenerator.generate(recurse);
		}
		long finish = System.currentTimeMillis();

		LOG.info("Converted: " + count + " docs in " + (finish - start) + " ms");

		CrawlStatus s = crawlState.getStatus();
		LOG.info("Status: " + s.toString());
		return output;
	}

	protected String createPathName(String prefix) {
		return prefix + dataSource.getCollection() + "_" + dataSource.getDataSourceId() + "_" + System.currentTimeMillis();
	}

	protected void indexToGaia(Configuration conf, Path input, Path parentOut) throws IOException, ClassNotFoundException {
		LOG.info("Starting indexing process for " + input);
		Job job = new Job(conf);
		File jobFile = HadoopUtils.getJobFileByName("behemoth-gaiasearch");
		if (jobFile != null) {
			job.getConfiguration().set("mapred.jar", jobFile.getAbsolutePath());
		} else {
			job.setJarByClass(TikaMapper.class);
		}

		// jobFile = HadoopUtils.getJobFileByName("behemoth-core");
		// if (jobFile != null) {
		// job.getConfiguration().set("mapred.jar", jobFile.getAbsolutePath());
		// }

		job.setJobName("Indexing " + input + " into GaiaSearch");
		job.getConfiguration().setQuietMode(false);
		// job.
		job.setInputFormatClass(SequenceFileInputFormat.class);

		String outClassStr = dataSource.getString("output_format");
		Class outClass;
		if (outClassStr != null)
			outClass = Class.forName(outClassStr).asSubclass(OutputFormat.class);
		else {
			outClass = GaiaSearchOutputFormat.class;
		}
		try {
			job.setOutputFormatClass(outClass);
		} catch (Exception e) {
			e.printStackTrace();
		}

		job.setOutputKeyClass(Text.class);
		job.setOutputValueClass(BehemothDocument.class);

		// job.setMapperClass(IdentityMapper.class);
		// job.setMapperClass(IdentityMapper.class);
		job.setMapperClass(IdentityMapper.class);

		job.setNumReduceTasks(0);

		FileInputFormat.addInputPath(job, input);

		Path tmp = new Path(parentOut, createPathName("indexOut_"));
		FileSystem fs = tmp.getFileSystem(conf);
		FileOutputFormat.setOutputPath(job, tmp);
		String zkHost = dataSource.getString("zookeeper_host");
		if (zkHost == null) {
			zkHost = System.getProperty("zkHost");
		}
		if ((zkHost != null) && (zkHost.trim().length() > 0)) {
			job.getConfiguration().set("solr.zkhost", dataSource.getString("zookeeper_host"));

			job.getConfiguration().set("solr.zk.collection", dataSource.getCollection());
		} else {
			String solrServer = dataSource.getString("solr_server_url");
			if ((solrServer == null) || (solrServer.trim().length() == 0)) {
				solrServer = dataSource.getString("output_args");
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
						URL u = MasterConfUtil.getSolrAddress(false, dataSource.getCollection());
						solrServer = u.toString();
					} catch (Exception e) {
						LOG.error("Could not obtain Solr address - jobs will likely fail: " + e.toString());
					}
				}
			}
			job.getConfiguration().set("solr.server.url", solrServer);
		}

		commonJobConfig(job.getConfiguration());
		long start = System.currentTimeMillis();
		CrawlStatus s = null;
		try {
			job.getConfiguration().setClassLoader(getClass().getClassLoader());
			// JobClient jobC = new JobClient(job);
			// RunningJob runningJob = jobC.submitJob(job);
			job.submit();
			crawlState.currentJob = job;
			crawlState.currentTask = BehemothCrawlState.TASK.INDEX;
			boolean success = job.waitForCompletion(true);
		} catch (Exception e) {
			LOG.error("Error in indexing content to GaiaSearch", e);
			failed = true;
			failureThrowable = e;
		} finally {
			s = crawlState.getStatus();
			if (outClass.equals(GaiaSearchOutputFormat.class)) {
				fs.delete(tmp, true);
			}
			if ((crawlState.currentJob != null) && (!failed) && (!crawlState.currentJob.isSuccessful()) && (!stopped)) {
				// FIXME: by whlee21
				// failureThrowable = new
				// Exception(crawlState.currentJob.getFailureInfo());
				failureThrowable = new Exception(crawlState.currentJob.toString());
			}

		}

		long finish = System.currentTimeMillis();
		LOG.info("Finished indexing " + input + " in " + (finish - start) + " ms");
		LOG.info("Status: " + s.toString());
	}

	private void commonJobConfig(Configuration conf) {
		conf.set("solr.params", "fm.ds=" + dataSource.getDataSourceId().toString());

		conf.setBoolean("gaia.metadata", dataSource.getBoolean("add_metadata"));
		conf.setBoolean("tika.metadata", dataSource.getBoolean("add_metadata"));

		conf.setStrings("gaia.batch.id", new String[] { UUID.randomUUID().toString() });
		conf.setBoolean("gaia.annotations", dataSource.getBoolean("add_annotations"));
		conf.setBoolean("tika.annotations", dataSource.getBoolean("add_annotations"));

		String anns = dataSource.getString("annotations");
		if (!StringUtils.isBlank(anns))
			conf.set("gaia.annotations.list", anns.trim());
	}

	protected Path tikaProcess(Configuration conf, Path input, Path outputParent, boolean direct) throws IOException {
		LOG.info("Starting Tika conversion process for " + input);
		Job job = new Job(conf);
		Path output = new Path(outputParent, createPathName("tikaOut_"));
		HadoopUtils.delete(conf, new Path[] { output });
		File jobFile = HadoopUtils.getJobFileByName("behemoth-tika");
		if (jobFile != null) {
			job.getConfiguration().set("mapred.jar", jobFile.getAbsolutePath());
		} else if (direct)
			job.setJarByClass(DirectAccessTikaMapper.class);
		else {
			job.setJarByClass(TikaMapper.class);
		}

		commonJobConfig(job.getConfiguration());
		String knownMimeType = dataSource.getString("mime_type");
		if ((knownMimeType != null) && (!knownMimeType.equals(""))) {
			job.getConfiguration().set("tika.mime.type", knownMimeType);
		}
		String processorClass = dataSource.getString("tika_content_handler", TikaProcessor.class.getName());
		job.getConfiguration().set("tika.processor", processorClass);

		job.setJobName("Processing " + input + " with Tika");

		job.setInputFormatClass(SequenceFileInputFormat.class);
		job.setOutputFormatClass(SequenceFileOutputFormat.class);

		job.setMapOutputKeyClass(Text.class);
		job.setMapOutputValueClass(BehemothDocument.class);
		job.setOutputKeyClass(Text.class);
		job.setOutputValueClass(BehemothDocument.class);

		if (direct) {
			job.setMapperClass(DirectAccessTikaMapper.class);

			FileSystem fs = input.getFileSystem(job.getConfiguration());
			FileStatus stat = fs.getFileStatus(input);

			float numBlocks = (float) stat.getLen() / (float) stat.getBlockSize();
			int numMaps = Math.round(numBlocks * 30.0F) + 1;
			// job.set .setNumMapTasks(numMaps);
			job.getConfiguration().setInt("mapreduce.job.maps", numMaps);
		} else {
			job.setMapperClass(TikaMapper.class);
		}

		job.setNumReduceTasks(0);

		FileInputFormat.addInputPath(job, input);
		FileOutputFormat.setOutputPath(job, output);

		long start = System.currentTimeMillis();
		CrawlStatus s = null;
		try {
			job.getConfiguration().setClassLoader(getClass().getClassLoader());

			// JobClient jobC = new JobClient(job);
			// RunningJob runningJob = jobC.submitJob(job);
			job.submit();
			crawlState.currentJob = job;
			crawlState.currentTask = BehemothCrawlState.TASK.TIKA;
			boolean success = job.waitForCompletion(true);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			s = crawlState.getStatus();
			if ((crawlState.currentJob != null) && (!failed) && (!crawlState.currentJob.isSuccessful()) && (!stopped)) {
				// FIXME: by whlee21
				// failureThrowable = new Exception(crawlState.currentJob.get);
				failureThrowable = new Exception();
			}

		}

		long finish = System.currentTimeMillis();

		LOG.info("Finished Tika conversion of " + input + " to " + output + " in " + (finish - start) + " ms");
		LOG.info("Status: " + s.toString());
		return output;
	}

	public synchronized void stop() throws IOException {
		stopped = true;
		if (crawlState.currentJob != null) {
			crawlState.currentJob.killJob();
		}
		if (generator != null)
			generator.stop();
	}
}
