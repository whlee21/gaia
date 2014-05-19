package gaia.bigdata.hadoop.behemoth;

import gaia.bigdata.hadoop.GaiaCounters;
import gaia.solr.behemoth.GaiaSearchOutputFormat;

import java.io.IOException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Counters;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.lib.input.SequenceFileInputFormat;
import org.apache.hadoop.util.ToolRunner;
import org.apache.mahout.common.AbstractJob;
import org.apache.solr.common.SolrException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.digitalpebble.behemoth.BehemothDocument;

public class BehemothDocumentToSolr extends AbstractJob {
	private static final Logger log = LoggerFactory.getLogger(BehemothDocumentToSolr.class);
	public static final String SOLR_URL_OPTION = "solrUrl";
	public static final String SOLR_ZK_HOST_OPTION = "solrZkHost";
	public static final String SOLR_ZK_COLLECTION_OPTION = "solrZkCollection";
	public static final String DO_ANNOTATIONS_OPTION = "doAnnotations";

	public int run(String[] args) throws Exception {
		addInputOption();
		addOutputOption();
		addOption("solrUrl", "u", "Solr URL");
		addOption("solrZkHost", "z", "Solr Cloud ZooKeeper host:port");
		addOption("solrZkCollection", "c", "Solr Cloud ZooKeeper collection name");
		addOption("doAnnotations", "a", "Whether annotations were used in the workflow and should be indexed.");
		addOption("solrFailedThresholdPercent", "t",
				"If the number of failed to index docs is equal to or greater than this threshold, wf fails.");

		if (parseArguments(args) == null) {
			return -1;
		}

		Configuration conf = getConf();
		Job job = new Job(conf);
		job.setJobName("Send BehemothDocuments to Solr");
		job.setJarByClass(getClass());

		job.setInputFormatClass(SequenceFileInputFormat.class);
		SequenceFileInputFormat.setInputPaths(job, new Path[] { getInputPath() });

		job.setOutputFormatClass(GaiaSearchOutputFormat.class);
		GaiaSearchOutputFormat.setOutputPath(job, getOutputPath());
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
			return -1;
		}
		conf.setBoolean("tika.metadata", true);
		conf.setBoolean("tika.annotations", false);
		conf.setBoolean("gaia.metadata", true);
		conf.setBoolean("gaia.annotations", false);
		if (getOption("doAnnotations").equals("true")) {
			conf.setBoolean("gaia.annotations", true);
			conf.set("solr.f.person", "Person");
			conf.set("solr.f.organization", "Organization");
			conf.set("solr.f.location", "Location");
		}
		job.setMapperClass(BehemothDocumentToSolrMapper.class);
		job.setOutputKeyClass(Text.class);
		job.setOutputValueClass(BehemothDocument.class);
		job.setNumReduceTasks(0);

		// FIXME: // by whlee21
		// RunningJob job = JobClient.runJob(jobConf);
		boolean success = job.waitForCompletion(true);

		if (!success) {
			log.error("No job running for SipsToSolr: " + job.getJobID());
			return -1;
		}

		if (!job.isComplete()) {
			log.error("Job hasn't completed for SipsToSolr: " + job.getJobID());
			return -1;
		}

		Counters counters = job.getCounters();
		int threshold = new Integer(getOption("solrFailedThresholdPercent")).intValue();
		long failed = counters.findCounter(GaiaCounters.BEHEMOTH_TO_SOLR_FAILED).getValue();
		long indexed = counters.findCounter(GaiaCounters.BEHEMOTH_TO_SOLR_INDEXED).getValue();
		long total = counters.findCounter("org.apache.hadoop.mapred.Task$Counter", "MAP_INPUT_RECORDS").getValue();
		log.info("Total number of docs submitted for indexing docs: " + total + ". Total number of indexed: " + indexed
				+ ". Total failed: " + failed + ".");
		if (failed / total * 100L > threshold) {
			log.error("Job failed for BehemothDocumentToSolr because of the threshold didn't pass.");
			return -1;
		}

		return 0;
	}

	public static void main(String[] args) throws Exception {
		ToolRunner.run(new BehemothDocumentToSolr(), args);
	}

	public static class BehemothDocumentToSolrMapper extends Mapper<Text, BehemothDocument, Text, BehemothDocument> {
		@Override
		protected void setup(Context context) {
		}

		@Override
		protected void cleanup(Context context) {
		}

		@Override
		protected void map(Text key, BehemothDocument value, Context context) throws IOException, InterruptedException {
			try {
				context.write(key, value);
				context.getCounter(GaiaCounters.BEHEMOTH_TO_SOLR_INDEXED).increment(1L);
			} catch (SolrException e) {
				log.warn("Failed to index document. Got SolrException: {}", e.getMessage());
				context.getCounter(GaiaCounters.BEHEMOTH_TO_SOLR_FAILED).increment(1L);
			}
		}
	}
}
