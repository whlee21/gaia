package gaia.solr.click.log;

import gaia.solr.click.Utils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Counter;
import org.apache.hadoop.mapreduce.CounterGroup;
import org.apache.hadoop.mapreduce.Counters;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;
import org.apache.hadoop.util.ReflectionUtils;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BoostTool extends Configured implements Tool {
	private static final Logger LOG = LoggerFactory.getLogger(BoostTool.class);

	static int DEFAULT_MAX_TOP_TERMS = 100;
	public static final String CNT_NEW = "cntNew";
	public static final String CNT_OLD = "cntOld";
	public static final String CNT_DOCS = "cntDocs";
	public static final String TOTAL_POS_BOOST1M = "totalPosBoost1M";
	public static final String TOTAL_TIME_BOOST1M = "totalTimeBoost1M";
	public static final String BOOST_PROCESSOR_CLASS = "boost.processor.class";

	public int run(String[] args) throws Exception {
		if (args.length == 0) {
			System.err
					.println("Usage: BoostTool <outputPath> [-time <ms>] [-period <ms>] [-history <nn>] [-processor <className] [-flatten] <inputDir1> ...");
			System.err.println("\toutputPath\tdestination folder for output data, or destination file if -flatten");
			System.err.println("\ttime <ms>\tset the time of boost update to this time since epoch");
			System.err
					.println("\tprocessor <class>\tfully-qualified class name of BoostProcessor (default is HalfLifeBoostProcessor)");
			System.err.println("\tperiod <ms>\tperiod of half-life decay in milliseconds (default is 30 days)");
			System.err.println("\thistory <nn>\tlength of history per boost record (smoothing), default is 10");
			System.err.println("\tflatten\tif present, output will be merged into a single file,");
			System.err.println("\t\totherwise it will consist of a series of files in output folder");
			return -1;
		}
		long updateTime = 0L;
		boolean flatten = false;
		Path out = new Path(args[0]);
		String processor = null;
		long period = 0L;
		int history = 0;
		ArrayList<Path> inputs = new ArrayList<Path>();
		for (int i = 1; i < args.length; i++) {
			if (args[i].equals("-time")) {
				updateTime = Long.parseLong(args[(++i)]);
			} else if (args[i].equals("-period")) {
				period = Long.parseLong(args[(++i)]);
			} else if (args[i].equals("-processor")) {
				processor = args[(++i)];
			} else if (args[i].equals("-history")) {
				history = Integer.parseInt(args[(++i)]);
			} else if (args[i].equals("-flatten")) {
				flatten = true;
			} else
				inputs.add(new Path(args[i]));
		}
		if (updateTime == 0L) {
			updateTime = System.currentTimeMillis();
		}
		boost((Path[]) inputs.toArray(new Path[0]), out, updateTime, flatten, processor, period, history);
		return 0;
	}

	public Stats boost(Path[] inputs, Path output, long updateTime, boolean flatten, String processor, long period,
			int history) throws Exception {
		Job job = new Job(getConf());
		job.setJarByClass(BoostTool.class);
		job.getConfiguration().setLong("update.time", updateTime);
		FileSystem fs = FileSystem.get(getConf());
		for (Path p : inputs) {
			if (fs.isFile(p)) {
				FileInputFormat.addInputPath(job, p);
			} else { // FIXME: by whlee21
				// Path[] files = FileUtil.stat2Paths(fs.listStatus(p, new
				// Utils.OutputFileUtils.OutputFilesFilter()));

				// if (files != null) {
				// for (Path f : files) {
				// FileInputFormat.addInputPath(job, f);
				// }
				// }
			}
		}
		if (processor != null) {
			job.getConfiguration().set("boost.processor.class", processor);
		}
		if (period > 0L) {
			job.getConfiguration().setLong("boost.processor.halflife.period", period);
		}
		if (history > 0) {
			job.getConfiguration().setInt("boost.processor.halflife.history.length", history);
		}
		job.setInputFormatClass(TextInputFormat.class);
		job.setMapperClass(BoostMapper.class);
		job.setCombinerClass(BoostCombiner.class);
		job.setReducerClass(BoostReducer.class);
		job.setOutputFormatClass(TextOutputFormat.class);
		job.setOutputKeyClass(Text.class);
		job.setOutputValueClass(BoostWritable.class);
		Path tmpOut = null;
		if (flatten) {
			job.setNumReduceTasks(1);
			tmpOut = new Path(getConf().get("hadoop.tmp.dir"), "boost-" + System.currentTimeMillis());

			FileOutputFormat.setOutputPath(job, tmpOut);
		} else {
			FileOutputFormat.setOutputPath(job, output);
		}
		job.waitForCompletion(false);
		if (flatten) {
			if (fs.exists(output))
				fs.delete(output, true);
			Path part = new Path(tmpOut, "part-r-00000");
			if (!fs.exists(part)) {
				throw new Exception("Missing output " + part);
			}
			fs.rename(part, output);
			fs.delete(tmpOut, true);
		}
		Counters counters = job.getCounters();
		Stats res = new Stats();
		CounterGroup cg = counters.getGroup(BoostTool.class.getName());
		for (Counter c : cg) {
			res.put(c.getName(), Long.valueOf(c.getValue()));
		}
		return res;
	}

	public static void main(String[] args) throws Exception {
		Configuration conf = Utils.createConfiguration();
		int res = ToolRunner.run(conf, new BoostTool(), args);
		System.exit(res);
	}

	public static class BoostReducer extends Reducer<Text, BoostWritable, Text, BoostWritable> {
		private BoostProcessor processor;
		private long updateTime;
		private HashMap<String, TermFreq> terms = new HashMap<String, TermFreq>();
		private int maxTopTerms;

		@Override
		protected void setup(Reducer<Text, BoostWritable, Text, BoostWritable>.Context ctx) {
			Configuration conf = ctx.getConfiguration();
			String boostClass = conf.get("boost.processor.class", HalfLifeBoostProcessor.class.getName());
			try {
				Class clazz = Class.forName(boostClass);
				processor = ((BoostProcessor) ReflectionUtils.newInstance(clazz, conf));
			} catch (ClassNotFoundException e) {
				BoostTool.LOG.warn(e.toString());
				processor = null;
			}
			updateTime = conf.getLong("update.time", System.currentTimeMillis());
			maxTopTerms = conf.getInt("boost.max.top.terms", BoostTool.DEFAULT_MAX_TOP_TERMS);
		}

		@Override
		protected void reduce(Text key, Iterable<BoostWritable> values,
				Reducer<Text, BoostWritable, Text, BoostWritable>.Context ctx) throws IOException, InterruptedException {
			BoostWritable res = new BoostWritable();
			BoostWritable previous = new BoostWritable();
			boolean hasPrevious = false;
			boolean hasCurrent = false;
			terms.clear();
			for (BoostWritable b : values)
				if (b.posWeightLog.length > 0) {
					previous.set(b);
					hasPrevious = true;
				} else {
					hasCurrent = true;
					res.currentPosBoost += b.currentPosBoost;
					res.currentTimeBoost += b.currentTimeBoost;

					for (TermFreq tf : b.topTerms) {
						TermFreq rtf = (TermFreq) terms.get(tf.term);
						if (rtf == null) {
							terms.put(tf.term, tf);
						} else {
							rtf.posWeight += tf.posWeight;
							rtf.timeWeight += tf.timeWeight;
						}
					}
				}
			List<TermFreq> list = new ArrayList<TermFreq>(terms.values());
			if ((!hasCurrent) && (hasPrevious)) {
				list.addAll(Arrays.asList(previous.topTerms));
			}
			Collections.sort(list, TermFreq.COMPARATOR);
			if (list.size() > maxTopTerms) {
				list = list.subList(0, maxTopTerms);
			}
			res.topTerms = ((TermFreq[]) list.toArray(new TermFreq[0]));
			if (processor != null) {
				res = processor.processHistory(key, res, hasPrevious ? previous : null, updateTime);
			}
			if (hasPrevious) {
				ctx.getCounter(BoostTool.class.getName(), "cntOld").increment(1L);
			}
			if (hasCurrent) {
				ctx.getCounter(BoostTool.class.getName(), "cntNew").increment(1L);
			}
			ctx.getCounter(BoostTool.class.getName(), "cntDocs").increment(1L);
			ctx.getCounter(BoostTool.class.getName(), "totalPosBoost1M").increment((long) (res.currentPosBoost * 1000000.0F));

			ctx.getCounter(BoostTool.class.getName(), "totalTimeBoost1M").increment(
					(long) (res.currentTimeBoost * 1000000.0F));

			ctx.write(key, res);
		}
	}

	public static class BoostCombiner extends Reducer<Text, BoostWritable, Text, BoostWritable> {
		private HashMap<String, TermFreq> terms = new HashMap<String, TermFreq>();

		@Override
		protected void reduce(Text key, Iterable<BoostWritable> values,
				Reducer<Text, BoostWritable, Text, BoostWritable>.Context ctx) throws IOException, InterruptedException {
			BoostWritable res = new BoostWritable();
			terms.clear();
			boolean hasNewValues = false;
			for (BoostWritable b : values)
				if ((b.posWeightLog != null) && (b.posWeightLog.length > 0)) {
					ctx.write(key, b);
				} else {
					hasNewValues = true;
					res.currentPosBoost += b.currentPosBoost;
					res.currentTimeBoost += b.currentTimeBoost;

					for (TermFreq tf : b.topTerms) {
						TermFreq rtf = (TermFreq) terms.get(tf.term);
						if (rtf == null) {
							terms.put(tf.term, tf);
						} else {
							rtf.posWeight += tf.posWeight;
							rtf.timeWeight += tf.timeWeight;
						}
					}
				}
			if (!hasNewValues) {
				return;
			}
			res.topTerms = ((TermFreq[]) terms.values().toArray(new TermFreq[0]));

			ctx.write(key, res);
		}
	}

	public static class BoostMapper extends Mapper<LongWritable, Text, Text, BoostWritable> {
		private BoostProcessor processor;
		private Text k = new Text();

		@Override
		public void setup(Mapper<LongWritable, Text, Text, BoostWritable>.Context ctx) {
			Configuration conf = ctx.getConfiguration();
			String boostClass = conf.get("boost.processor.class", HalfLifeBoostProcessor.class.getName());
			try {
				Class clazz = Class.forName(boostClass);
				processor = ((BoostProcessor) ReflectionUtils.newInstance(clazz, conf));
			} catch (Exception e) {
				e.printStackTrace();
				BoostTool.LOG.warn(e.toString());
				processor = null;
			}
		}

		@Override
		protected void map(LongWritable key, Text value, Mapper<LongWritable, Text, Text, BoostWritable>.Context ctx)
				throws IOException, InterruptedException {
			String line = value.toString();
			if ((line.startsWith("#")) || (line.trim().length() == 0)) {
				return;
			}

			int pos = line.indexOf('\t');
			if (pos == -1) {
				BoostTool.LOG.warn("Invalid URL/BoostWritable string: '" + line + "'");
				return;
			}
			String id = line.substring(0, pos);
			String v = line.substring(pos + 1);
			BoostWritable b = BoostWritable.fromString(v);
			if (b == null) {
				BoostTool.LOG.warn("Invalid BoostWritable string: '" + v + "'");
				return;
			}
			k.set(id);
			if ((b.posWeightLog.length == 0) && (processor != null)) {
				b = processor.processCurrent(k, b);
			}
			ctx.write(k, b);
		}
	}

	public static class BoostEntry {
		private BoostWritable boost = new BoostWritable();
		private Text docId = new Text();

		public BoostWritable getBoost() {
			return boost;
		}

		public String getDocId() {
			return docId.toString();
		}
	}
}
