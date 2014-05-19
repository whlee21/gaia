package gaia.bigdata.hadoop.logs;

import gaia.bigdata.hadoop.GaiaCounters;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.BytesWritable;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.WritableComparable;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.SequenceFileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.LazyOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.MultipleOutputs;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;
import org.apache.hadoop.util.ToolRunner;
import org.apache.mahout.common.AbstractJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProcessClickLogs extends AbstractJob {
	private static final Logger log = LoggerFactory.getLogger(ProcessClickLogs.class);
	private static final String JOB_TAG = "gaia.bigdata.hadoop.tag";
	private static final String FILE_OUTPUT = "gaia.bigdata.hadoop.output";
	private static final String INPUT_FIELD_SEP = "gaia.bigdata.hadoop.ifs";
	private static final String MOS = "logs";
	private static final char FIELD_SEP = '\037';

	public int run(String[] args) throws Exception {
		addInputOption();
		addOutputOption();
		addOption("delimiter", "d", "input delimiter");

		if (parseArguments(args) == null) {
			return -1;
		}

		Path input = getInputPath();
		Path output = getOutputPath();
		String delimHexString = getOption("delimiter");

		Configuration conf = getConf();
		Job job = new Job();
		job.setJarByClass(ProcessClickLogs.class);
		job.setJobName(ProcessClickLogs.class.getName());
		conf.set(JOB_TAG, Long.toString(new Date().getTime()));
		conf.set(FILE_OUTPUT, output.toString());
		conf.set(INPUT_FIELD_SEP, delimHexString);

		Path tmpPath = new Path(getTempPath(), Long.toString(System.currentTimeMillis()));

		job.setInputFormatClass(SequenceFileInputFormat.class);

		for (FileStatus fstat : input.getFileSystem(conf).globStatus(input)) {
			assert (fstat.getPath().toString().matches("^.*id=\\w+.*$"));
		}
		SequenceFileInputFormat.setInputPaths(job, new Path[] { input });

		MultipleOutputs.addNamedOutput(job, MOS, TextOutputFormat.class, NullWritable.class, LogWritable.class);

		MultipleOutputs.setCountersEnabled(job, true);
		TextOutputFormat.setOutputPath(job, tmpPath);
		TextOutputFormat.setCompressOutput(job, false);
		LazyOutputFormat.setOutputFormatClass(job, TextOutputFormat.class);

		job.setMapperClass(ClickLogMapper.class);
		job.setMapOutputKeyClass(Text.class);
		job.setMapOutputValueClass(Text.class);
		job.setReducerClass(ClickLogReducer.class);

		return job.waitForCompletion(true) ? 0 : 1;
	}

	public static void main(String[] args) throws Exception {
		ToolRunner.run(new ProcessClickLogs(), args);
	}

	public static class LogWritable implements WritableComparable<LogWritable> {
		private Text log = new Text();
		private Text collection = new Text();
		private Text type = new Text();
		private Text partition = new Text();

		public LogWritable() {
		}

		public LogWritable(String type, String collection, String log, String partition) {
			this.type.set(type);
			this.collection.set(collection);
			this.log.set(log);
			this.partition.set(partition);
		}

		public LogWritable setType(String b) {
			this.type.set(b);
			return this;
		}

		public LogWritable setLog(String log) {
			this.log.set(log);
			return this;
		}

		public LogWritable setCollection(String collection) {
			this.collection.set(collection);
			return this;
		}

		public LogWritable setPartition(String partition) {
			this.partition.set(partition);
			return this;
		}

		public String toString() {
			return log.toString();
		}

		public void readFields(DataInput in) throws IOException {
			collection.readFields(in);
			type.readFields(in);
			log.readFields(in);
			partition.readFields(in);
		}

		public void write(DataOutput out) throws IOException {
			collection.write(out);
			type.write(out);
			log.write(out);
			partition.write(out);
		}

		public int compareTo(LogWritable other) {
			if (other.equals(this)) {
				return 0;
			}
			int cmp = other.collection.compareTo(collection);
			if (cmp == 0) {
				cmp = other.type.compareTo(type);
			}
			if (cmp == 0) {
				cmp = other.log.compareTo(log);
			}
			if (cmp == 0) {
				cmp = other.partition.compareTo(partition);
			}
			return cmp;
		}
	}

	public static class ClickLogReducer extends Reducer<Text, Text, NullWritable, NullWritable> {
		MultipleOutputs<NullWritable, LogWritable> mos;
		LogWritable reuse = new LogWritable();

		@Override
		protected void setup(Context context) {
			mos = new MultipleOutputs(context);
		}

		@Override
		protected void cleanup(Context context) throws IOException, InterruptedException {
			mos.close();
		}

		@Override
		protected void reduce(Text key, Iterable<Text> values, Context context) throws IOException, InterruptedException {
			for (Text value : values) {
				String[] cols = value.toString().split(String.valueOf(FIELD_SEP));
				String type;
				if (cols[0].equalsIgnoreCase("C")) {
					type = "click";
				} else {
					if (cols[0].equalsIgnoreCase("Q")) {
						type = "query";
					} else {
						ProcessClickLogs.log.warn("Unknown log type {}", cols[0]);
						context.getCounter(GaiaCounters.UNKNOWN_LOG_TYPE).increment(1L);
						continue;
					}
				}
				reuse.setCollection(cols[1]).setType(type).setLog(value.toString()).setPartition(key.toString());
				mos.write(NullWritable.get(), reuse, generateFileName(context.getConfiguration(), reuse, MOS));
			}
		}

		private String generateFileName(Configuration conf, LogWritable value, String name) {
			String fileName = "/collections/" + value + "/logs/" + value + "/" + value + "/" + name;
			String finalFileName = conf.get(FILE_OUTPUT) + fileName + conf.get(JOB_TAG);
			return finalFileName;
		}
	}

	public static class ClickLogMapper extends Mapper<NullWritable, BytesWritable, Text, Text> {
		DateFormat format;
		Text reuseKey = new Text();
		Text reuseValue = new Text();
		Calendar cal = Calendar.getInstance();
		Path inputPath;
		String parentName;
		String delim;

		@Override
		protected void setup(Context context) {
			Configuration conf = context.getConfiguration();
			format = new SimpleDateFormat("yyyy-MM-dd");
			format.setTimeZone(TimeZone.getTimeZone("UTC"));
			cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
			inputPath = new Path(conf.get("map.input.file"));
			parentName = inputPath.getParent().getName();
			assert (parentName.startsWith("id=") == true);
			delim = new String(Character.toChars(Integer.parseInt(conf.get(INPUT_FIELD_SEP), 16)));
		}

		@Override
		protected void cleanup(Context context) throws IOException {
		}

		@Override
		protected void map(NullWritable _, BytesWritable bytes, Context context) throws IOException, InterruptedException {
			byte[] ba = new byte[bytes.getLength()];
			System.arraycopy(bytes.getBytes(), 0, ba, 0, ba.length);
			String message = new String(ba, "UTF-8").trim();
			String[] parts = message.split(delim);

			String type = parts[1];
			String collection = parts[0];
			parts[0] = type;
			parts[1] = collection;
			long ts = Long.parseLong(parts[2]);
			cal.setTimeInMillis(ts);
			int hour = cal.get(10);

			String hourString = String.format("%02d", new Object[] { Integer.valueOf(hour) });
			String dateString = format.format(cal.getTime());

			reuseKey.set(parentName + "/" + dateString + "/" + hourString);
			reuseValue.set(StringUtils.join(parts, FIELD_SEP));
			context.write(reuseKey, reuseValue);
		}
	}
}
