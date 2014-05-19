package gaia.bigdata.hadoop.simdoc;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.SequenceFile;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.input.SequenceFileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.MapFileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.SequenceFileOutputFormat;
import org.apache.hadoop.util.ToolRunner;
import org.apache.mahout.common.AbstractJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SequenceFileToMapFileJob extends AbstractJob {
	private static final Logger log = LoggerFactory.getLogger(SequenceFileToMapFileJob.class);

	public int run(String[] args) throws IOException, ClassNotFoundException, InterruptedException {
		addInputOption();
		addOutputOption();

		Map<String, List<String>> parsedArgs = parseArguments(args);

		if (parsedArgs == null) {
			return -1;
		}

		Job job = new Job(getConf());
		job.setJobName("SequenceFileToMapFile Size");
		job.setJarByClass(getClass());
		job.getConfiguration().set("mapreduce.fileoutputcommitter.marksuccessfuljobs", "false");

		job.setInputFormatClass(SequenceFileInputFormat.class);
		SequenceFileInputFormat.setInputPaths(job, new Path[] { getInputPath() });

		FileOutputFormat.setOutputPath(job, getOutputPath());
		job.setOutputKeyClass(IntWritable.class);
		job.setOutputFormatClass(MapFileOutputFormat.class);
		SequenceFileOutputFormat.setCompressOutput(job, true);
		SequenceFileOutputFormat.setOutputCompressionType(job, SequenceFile.CompressionType.BLOCK);
		return job.waitForCompletion(true) ? 0 : 1;
	}

	public static void main(String[] args) throws Exception {
		ToolRunner.run(new SequenceFileToMapFileJob(), args);
	}
}
