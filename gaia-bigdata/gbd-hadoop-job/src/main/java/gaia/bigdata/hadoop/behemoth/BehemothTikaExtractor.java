package gaia.bigdata.hadoop.behemoth;

import gaia.bigdata.hadoop.GaiaCounters;

import java.io.IOException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.lib.input.SequenceFileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.SequenceFileOutputFormat;
import org.apache.hadoop.util.ToolRunner;
import org.apache.mahout.common.AbstractJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.digitalpebble.behemoth.BehemothDocument;
import com.digitalpebble.behemoth.tika.TikaProcessor;

public class BehemothTikaExtractor extends AbstractJob {
	private static transient Logger log = LoggerFactory.getLogger(BehemothTikaExtractor.class);

	public static void main(String[] args) throws Exception {
		ToolRunner.run(new BehemothTikaExtractor(), args);
	}

	public int run(String[] args) throws Exception {
		System.setProperty("java.awt.headless", "true");
		addInputOption();
		addOutputOption();

		if (parseArguments(args) == null) {
			return -1;
		}

		Path input = getInputPath();
		Path output = getOutputPath();

		Configuration conf = getConf();
		conf.setBoolean("tika.metadata", true);
		conf.setBoolean("tika.annotations", false);
		conf.setInt("mapreduce.job.counters.limit", 255);
		conf.setInt("mapred.max.map.failures.percent", 5);

		Job job = new Job(getConf());
		job.setJobName(getClass().getName());
		job.setJarByClass(BehemothTikaExtractor.class);
		job.setInputFormatClass(SequenceFileInputFormat.class);
		SequenceFileInputFormat.setInputPaths(job, new Path[] { input });

		job.setOutputFormatClass(SequenceFileOutputFormat.class);
		SequenceFileOutputFormat.setOutputPath(job, output);
		SequenceFileOutputFormat.setCompressOutput(job, false);

		job.setMapperClass(Map.class);
		job.setOutputKeyClass(Text.class);
		job.setOutputValueClass(BehemothDocument.class);

		job.setNumReduceTasks(0);

		// JobClient.runJob(conf);
		return job.waitForCompletion(true) ? 0 : 1;
	}

	public static class Map extends Mapper<Text, BehemothDocument, Text, BehemothDocument> {
		protected TikaProcessor processor;

		@Override
		protected void setup(Context context) {
			processor = new TikaProcessor();
			processor.setConf(context.getConfiguration());
			System.setProperty("java.awt.headless", "true");
		}

		@Override
		protected void cleanup(Context context) throws IOException {
		}

		@Override
		protected void map(Text url, BehemothDocument doc, Context context) throws IOException {
			try {
				for (BehemothDocument outDoc : processor.process(doc, context))
					context.write(url, outDoc);
			} catch (OutOfMemoryError e) {
				System.gc();
				context.getCounter(GaiaCounters.TIKA_FORCED_GC).increment(1L);
				context.getCounter(GaiaCounters.TIKA_EXTRACT_FAILED).increment(1L);
				BehemothTikaExtractor.log.warn("Error processing file: " + url + " due to an OOM, forcing a GC", e);
			} catch (Throwable e) {
				context.getCounter(GaiaCounters.TIKA_EXTRACT_FAILED).increment(1L);
				BehemothTikaExtractor.log.warn("Error processing file: " + url, e);
			}
		}
	}
}
