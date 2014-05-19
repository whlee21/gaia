package gaia.bigdata.hadoop.logs;

import gaia.bigdata.hadoop.io.LogMessageWritable;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.VIntWritable;
import org.apache.hadoop.io.Writable;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.lib.input.SequenceFileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.LazyOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.MultipleOutputs;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;
import org.apache.hadoop.util.Tool;

public class RawLogProcessJob extends Configured implements Tool {
	private static final String REQUEST_LOG_MOS = "requestlogs";
	private static final String SERVER_LOG_MOS = "serverlogs";
	private static final String JOB_TAG = "gaia.bigdata.hadoop.tag";

	public int run(String[] args) throws Exception {
		Configuration conf = getConf();
		conf.set(JOB_TAG, Long.toString(new Date().getTime()));

		Job job = new Job(conf);
		job.setJarByClass(RawLogProcessJob.class);
		job.setJobName(RawLogProcessJob.class.getName());

		Path inputDir = new Path(args[0]);
		Path baseDir = new Path(args[1]);

		LazyOutputFormat.setOutputFormatClass(job, TextOutputFormat.class);

		job.setInputFormatClass(SequenceFileInputFormat.class);
		SequenceFileInputFormat.setInputPaths(job, new Path[] { inputDir });

		MultipleOutputs.addNamedOutput(job, REQUEST_LOG_MOS, TextOutputFormat.class, NullWritable.class,
				SolrLogWritable.class);

		MultipleOutputs.addNamedOutput(job, SERVER_LOG_MOS, TextOutputFormat.class, NullWritable.class,
				LogMessageWritable.class);

		MultipleOutputs.setCountersEnabled(job, true);
		TextOutputFormat.setOutputPath(job, new Path(baseDir, conf.get(JOB_TAG)));
		TextOutputFormat.setCompressOutput(job, false);
		job.setMapperClass(RawLogProcessMapper.class);
		job.setMapOutputKeyClass(NullWritable.class);
		job.setMapOutputValueClass(NullWritable.class);
		job.setNumReduceTasks(0);

		job.waitForCompletion(true);

		FileSystem fs = inputDir.getFileSystem(conf);
		Path baseDestDir = fs.makeQualified(new Path("/data/raw-logs"));

		for (FileStatus fstat : fs.globStatus(inputDir)) {
			Path srcPath = fstat.getPath();
			String[] pathParts = srcPath.toString().split("/");
			String name = pathParts[(pathParts.length - 1)];
			String hour = pathParts[(pathParts.length - 2)];
			String date = pathParts[(pathParts.length - 3)];

			Path destPath = new Path(baseDestDir, date + "/" + hour + "/" + conf.get(JOB_TAG) + "-" + srcPath.getName());
			fs.mkdirs(destPath.getParent());
			System.err.print("Moving " + srcPath.toUri() + " to " + destPath.toUri());
			boolean b = fs.rename(srcPath, destPath);
			System.err.println(b ? " success" : " failed");
		}
		System.err.println("DONE");
		return 0;
	}

	public static void main(String[] args) throws Exception {
		new RawLogProcessJob().run(args);
	}

	static class SolrLogWritable implements Writable {
		private Text collection = new Text();
		private LongWritable date = new LongWritable();
		private Text webapp = new Text();
		private Text path = new Text();
		private Text params = new Text();
		private VIntWritable status = new VIntWritable();
		private VIntWritable hitcount = new VIntWritable();
		private VIntWritable qtime = new VIntWritable();

		private static final String delim = new Character('\001').toString();

		public long getDate() {
			return date.get();
		}

		public void readFields(DataInput in) throws IOException {
			collection.readFields(in);
			date.readFields(in);
			webapp.readFields(in);
			path.readFields(in);
			params.readFields(in);
			status.readFields(in);
			hitcount.readFields(in);
			qtime.readFields(in);
		}

		public void write(DataOutput out) throws IOException {
			collection.write(out);
			date.write(out);
			webapp.write(out);
			path.write(out);
			params.write(out);
			status.write(out);
			hitcount.write(out);
			qtime.write(out);
		}

		public String toString() {
			return collection + delim + date.get() + delim + webapp + delim + path + delim + params + delim + status + delim
					+ hitcount + delim + qtime;
		}
	}

	// static class SolrLogOutputFormat extends
	// MultipleTextOutputFormat<NullWritable, RawLogProcessJob.SolrLogWritable> {
	// final DateFormat format = new SimpleDateFormat("yyyy-MM-dd/HH");
	//
	// protected String generateFileNameForKeyValue(NullWritable key,
	// RawLogProcessJob.SolrLogWritable value, String name) {
	// return "/data/collections/" + value.collection + "/" +
	// format.format(Long.valueOf(value.getDate())) + "/"
	// + name;
	// }
	//
	// protected String getInputFileBasedOutputFileName(JobConf conf, String name)
	// {
	// return name + "-" + conf.get(JOB_TAG);
	// }
	// }
	//
	// static class ServerLogOutputFormat extends
	// MultipleTextOutputFormat<NullWritable, LogMessageWritable> {
	// final DateFormat format = new SimpleDateFormat("yyyy-MM-dd/HH");
	//
	// protected String generateFileNameForKeyValue(NullWritable key,
	// LogMessageWritable value, String name) {
	// return "/data/server-logs/" + format.format(Long.valueOf(value.getDate()))
	// + "/" + name;
	// }
	//
	// protected String getInputFileBasedOutputFileName(JobConf conf, String name)
	// {
	// return name + "-" + conf.get(JOB_TAG);
	// }
	// }

	static class RawLogProcessMapper extends Mapper<NullWritable, LogMessageWritable, NullWritable, LogMessageWritable> {
		MultipleOutputs mos;
		final Pattern requestLogPattern = Pattern.compile("^\\[(\\S+?)\\]\\s(.*)$", 32);
		final Pattern fieldExtractPattern = Pattern.compile("(\\w+)=(\\S+)?\\s?", 32);

		@Override
		protected void setup(Context context) {
			mos = new MultipleOutputs(context);
		}

		@Override
		protected void cleanup(Context context) throws IOException, InterruptedException {
			mos.close();
		}

		@Override
		protected void map(NullWritable _, LogMessageWritable log, Context context) throws IOException, InterruptedException {
			String message = log.getMessage().trim();

			Matcher matcher = requestLogPattern.matcher(message);
			if ((matcher.matches()) && (matcher.groupCount() == 2)) {
				String collection = matcher.group(1);
				String remainder = matcher.group(2);
				Matcher fieldMatcher = fieldExtractPattern.matcher(remainder);
				Map<String, String> fields = new HashMap<String, String>();
				while (fieldMatcher.find()) {
					fields.put(fieldMatcher.group(1), fieldMatcher.group(2));
				}
				if ((fields.size() > 0) && (fields.containsKey("webapp"))) {
					RawLogProcessJob.SolrLogWritable w = new RawLogProcessJob.SolrLogWritable();
					w.date.set(log.getDate());
					w.collection.set(collection);
					w.webapp.set(get(fields, "webapp", ""));
					w.path.set(get(fields, "path", ""));
					w.params.set(get(fields, "params", ""));
					if (fields.containsKey("status")) {
						w.status.set(Integer.parseInt((String) fields.get("status")));
					}
					if (fields.containsKey("hits")) {
						w.hitcount.set(Integer.parseInt((String) fields.get("hits")));
					}
					if (fields.containsKey("QTime")) {
						w.qtime.set(Integer.parseInt((String) fields.get("QTime")));
					}
					// OutputCollector collector = mos.getCollector(REQUEST_LOG_MOS,
					// reporter);
					// collector.collect(NullWritable.get(), w);
					mos.write(NullWritable.get(), w, generateRequestLogFileName(context.getConfiguration(), w, REQUEST_LOG_MOS));
					return;
				}
			}
			// OutputCollector collector = mos.getCollector(SERVER_LOG_MOS,
			// reporter);
			// collector.collect(NullWritable.get(), log);
			mos.write(NullWritable.get(), log, generateServerLogFileName(context.getConfiguration(), log, SERVER_LOG_MOS));
		}

		private String generateRequestLogFileName(Configuration conf, SolrLogWritable value, String name) {
			final DateFormat format = new SimpleDateFormat("yyyy-MM-dd/HH");
			return "/data/collections/" + value.collection + "/" + format.format(Long.valueOf(value.getDate())) + "/" + name
					+ "-" + conf.get(JOB_TAG);
		}

		private String generateServerLogFileName(Configuration conf, LogMessageWritable value, String name) {
			final DateFormat format = new SimpleDateFormat("yyyy-MM-dd/HH");
			return "/data/server-logs/" + format.format(Long.valueOf(value.getDate())) + "/" + name + "-" + conf.get(JOB_TAG);
		}

		private String get(Map<String, String> map, String key, String def) {
			String value = (String) map.get(key);
			if (value == null) {
				return def;
			}
			return value;
		}
	}
}
