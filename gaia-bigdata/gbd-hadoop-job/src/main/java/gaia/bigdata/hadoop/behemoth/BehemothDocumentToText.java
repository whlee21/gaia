package gaia.bigdata.hadoop.behemoth;

import gaia.bigdata.hadoop.GaiaCounters;

import java.io.IOException;

import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.lib.input.SequenceFileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.SequenceFileOutputFormat;
import org.apache.hadoop.util.ToolRunner;
import org.apache.mahout.common.AbstractJob;

import com.digitalpebble.behemoth.BehemothDocument;

public class BehemothDocumentToText extends AbstractJob {
	public int run(String[] args) throws Exception {
		addInputOption();
		addOutputOption();

		if (parseArguments(args) == null) {
			return -1;
		}

		Path input = getInputPath();
		Path output = getOutputPath();

		Job job = new Job(getConf());
		job.setJobName(getClass().getName());
		job.setJarByClass(BehemothDocumentToText.class);

		job.setInputFormatClass(SequenceFileInputFormat.class);
		SequenceFileInputFormat.setInputPaths(job, new Path[] { input });

		job.setOutputFormatClass(SequenceFileOutputFormat.class);
		SequenceFileOutputFormat.setOutputPath(job, output);

		job.setMapperClass(Map.class);
		job.setOutputKeyClass(Text.class);
		job.setOutputValueClass(Text.class);

		job.setNumReduceTasks(0);

		// JobClient.runJob(conf);
		return job.waitForCompletion(true) ? 0 : 1;
	}

	public static void main(String[] args) throws Exception {
		ToolRunner.run(new BehemothDocumentToText(), args);
	}

	public static class Map extends Mapper<Text, BehemothDocument, Text, Text> {
		private Text textReuse = new Text();

		@Override
		protected void setup(Context context) {
		}

		@Override
		protected void cleanup(Context context) throws IOException {
		}

		@Override
		protected void map(Text uri, BehemothDocument doc, Context context) throws IOException, InterruptedException {
			String text = doc.getText();
			if (text == null) {
				context.getCounter(GaiaCounters.BEHEMOTH_NO_TEXT).increment(1L);
				return;
			}
			textReuse.set(text);
			context.write(uri, textReuse);
		}
	}
}
