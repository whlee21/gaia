package gaia.bigdata.hadoop.util;

import java.io.IOException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.BytesWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;
import org.apache.hadoop.mapreduce.lib.output.SequenceFileOutputFormat;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;

public class TextToBytesWritable extends Configured implements Tool {
	public static void main(String[] args) throws Exception {
		ToolRunner.run(new TextToBytesWritable(), args);
	}

	public int run(String[] args) throws Exception {
		Path input = new Path(args[0]);
		Path output = new Path(args[1]);

		Configuration conf = getConf();
		Job job = new Job(conf);
		job.setJarByClass(TextToBytesWritable.class);
		job.setJobName(TextToBytesWritable.class.getName());

		job.setInputFormatClass(TextInputFormat.class);
		TextInputFormat.setInputPaths(job, new Path[] { input });

		job.setOutputFormatClass(SequenceFileOutputFormat.class);
		SequenceFileOutputFormat.setOutputPath(job, output);

		job.setMapperClass(TextToBytesMapper.class);
		job.setMapOutputKeyClass(NullWritable.class);
		job.setMapOutputValueClass(BytesWritable.class);
		job.setOutputKeyClass(NullWritable.class);
		job.setOutputValueClass(BytesWritable.class);

		job.setNumReduceTasks(0);

		return job.waitForCompletion(true) ? 0 : 1;
	}

	public static class TextToBytesMapper extends Mapper<LongWritable, Text, NullWritable, BytesWritable> {
		private static final BytesWritable reuse = new BytesWritable();

		@Override
		protected void setup(Context context) {
		}

		@Override
		protected void cleanup(Context context) throws IOException {
		}

		@Override
		protected void map(LongWritable _, Text line, Context context) throws IOException, InterruptedException {
			byte[] ba = line.toString().getBytes();
			reuse.set(ba, 0, ba.length);
			context.write(NullWritable.get(), reuse);
		}
	}
}
