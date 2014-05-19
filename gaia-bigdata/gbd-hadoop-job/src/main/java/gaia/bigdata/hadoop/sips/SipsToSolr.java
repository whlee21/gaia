package gaia.bigdata.hadoop.sips;

import gaia.bigdata.hadoop.GaiaCounters;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.mapreduce.Counters;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.input.SequenceFileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.util.ToolRunner;
import org.apache.mahout.common.AbstractJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SipsToSolr extends AbstractJob {
	private static final Logger log = LoggerFactory.getLogger(SipsToSolr.class);

	public static void main(String[] args) throws Exception {
		System.exit(ToolRunner.run(new Configuration(), new SipsToSolr(), args));
	}

	public int run(String[] args) throws Exception {
		addInputOption();
		addOutputOption();
		addOption("solrUrl", "u", "Solr URL");
		addOption("solrZkHost", "z", "Solr Cloud ZooKeeper host:port");

		addOption("solrZkCollection", "c", "Solr Cloud ZooKeeper collection name");

		addOption("solrFailedThresholdPercent", "t",
				"If the number of failed to index docs is equal to or greater than this threshold, wf fails.");

		if (parseArguments(args) == null) {
			return 1;
		}

		Configuration conf = getConf();
		Job job = new Job(conf);
		job.setJobName("Sips Indexer");
		job.setJarByClass(getClass());

		job.setInputFormatClass(SequenceFileInputFormat.class);
		SequenceFileInputFormat.setInputPaths(job, new Path[] { getInputPath() });
		FileOutputFormat.setOutputPath(job, getOutputPath());

		if (getOption("solrUrl") != null) {
			if (getOption("solrZkHost") != null) {
				log.warn("Both solrUrl and solrZkHost specified, using solrUrl");
			}
			conf.set("solr.server.url", getOption("solrUrl"));

			log.info("Indexing documents to {}", getOption("solrUrl"));
		} else if ((getOption("solrZkHost") != null) && (getOption("solrZkCollection") != null)) {
			conf.set("solr.zkhost", getOption("solrZkHost"));

			conf.set("solr.zk.collection", getOption("solrZkCollection"));

			log.info("Indexing documents to {}/{}", getOption("solrZkHost"), getOption("solrZkCollection"));
		} else {
			log.error("Must either specify solrUrl or solrZkHost+solrZkCollection");
			return 1;
		}

		job.setMapperClass(SipsToSolrMapper.class);
		job.setMapOutputKeyClass(NullWritable.class);
		job.setMapOutputValueClass(NullWritable.class);
		job.setNumReduceTasks(0);
		job.setMapSpeculativeExecution(false);

		boolean success = job.waitForCompletion(true);

		if (!success) {
			log.error("No job running for SipsToSolr: " + job.getJobID());
			return 1;
		}

		if (!job.isComplete()) {
			log.error("Job hasn't completed for SipsToSolr: " + job.getJobID());
			return 1;
		}

		Counters counters = job.getCounters();
		int threshold = new Integer(getOption("solrFailedThresholdPercent")).intValue();
		long failed = counters.findCounter(GaiaCounters.SIPS_TO_SOLR_FAILED).getValue();
		long indexed = counters.findCounter(GaiaCounters.SIPS_TO_SOLR_INDEXED).getValue();
		long total = counters.findCounter("org.apache.hadoop.mapred.Task$Counter", "MAP_INPUT_RECORDS").getValue();
		log.info("Total number of docs submitted for indexing sips: " + total + ". Total number of indexed: " + indexed
				+ ". Total failed: " + failed + ".");

		if (failed / total * 100L > threshold) {
			log.error("Job failed for SipsToSolr because of the threshold didn't pass.");
			return 1;
		}
		return 0;
	}
}
