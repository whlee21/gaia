package gaia.solr.click.log;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.FloatWritable;
import org.apache.hadoop.io.GenericWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.Writable;
import org.apache.hadoop.mapreduce.Counter;
import org.apache.hadoop.mapreduce.CounterGroup;
import org.apache.hadoop.mapreduce.Counters;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.SequenceFileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;
import org.apache.hadoop.util.GenericOptionsParser;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gaia.solr.click.Utils;

public class AggregateTool extends Configured implements Tool {
	private static final Logger LOG = LoggerFactory.getLogger(AggregateTool.class);

	public static final String BY_DOC = new StringBuilder().append(AggregateTool.class.getName()).append(".byDoc")
			.toString();

	public static final String TOP_N = new StringBuilder().append(AggregateTool.class.getName()).append(".topN")
			.toString();

	public static final String MIN_WEIGHT = new StringBuilder().append(AggregateTool.class.getName())
			.append(".minWeight").toString();

	public static final String COLLAPSE = new StringBuilder().append(AggregateTool.class.getName()).append(".collapse")
			.toString();
	public static final String CNT_DOCS = "cntDocs";
	public static final String CNT_TERMS = "cntTerms";

	public int run(String[] args) throws Exception {
		if (args.length < 3) {
			System.err
					.println("Usage: AggregateTool (doc | term) [-top NN] [-min FF.F] [-collapse] <outputDir> <prepDir1> [<prepDir2> ...]");
			System.err.println("\tdoc\taggregate by documentID, collecting top NN terms");
			System.err.println("\tterm\taggregate by term, ignoring document IDs");
			System.err.println("\t-top NN\tcollect top NN shingles per document (only for 'doc' aggregation)");
			System.err.println("\t-min FF.F\tcollect only terms with total minimum weight (only for 'term' aggregation)");
			System.err
					.println("\tcollapse\tturns on 'phrase collapsing', i.e. keeps only the longest phrases and accumulates");
			System.err.println("\t\tweights from shorter phrases/terms that completely fit in the longer phrases.");
			System.err.println("\toutputDir\toutput path (must not exist)");
			System.err.println("\tprepDir\tdirectories with output data from PrepareTool");
			return -1;
		}
		boolean byDoc = false;
		int topN = 20;
		float minWeight = 1.0F;
		boolean collapse = false;
		Path output = null;
		FileSystem fs = FileSystem.get(getConf());
		Set<Path> inputs = new HashSet<Path>();
		for (int i = 0; i < args.length; i++) {
			if (args[i].equals("doc")) {
				byDoc = true;
			} else if (args[i].equals("term")) {
				byDoc = false;
			} else if (args[i].equals("-collapse")) {
				collapse = true;
				getConf().setBoolean(COLLAPSE, collapse);
			} else if (args[i].equals("-top")) {
				topN = Integer.parseInt(args[(++i)]);
			} else if (args[i].equals("-min")) {
				minWeight = Float.parseFloat(args[(++i)]);
			} else if (output == null) {
				output = new Path(args[i]);
			} else {
				Path p = new Path(args[i]);
				if (!fs.exists(p)) {
					LOG.warn(new StringBuilder().append(" - input path '").append(p).append("' does not exist - skipping ...")
							.toString());
				} else {
					inputs.add(new Path(args[i]));
				}
			}
		}
		aggregate((Path[]) inputs.toArray(new Path[0]), output, byDoc, minWeight, topN);
		return 0;
	}

	public Stats aggregate(Path[] inputs, Path output, boolean byDoc, float minWeight, int topN) throws Exception {
		if (output == null) {
			throw new Exception("Required outputDir argument missing.");
		}
		if ((inputs == null) || (inputs.length == 0)) {
			throw new Exception("At least one input path is required.");
		}
		getConf().setBoolean(BY_DOC, byDoc);
		getConf().setInt(TOP_N, topN);
		getConf().setFloat(MIN_WEIGHT, minWeight);
		Job job = new Job(getConf());
		job.setJarByClass(AggregateTool.class);
		job.setJobName(new StringBuilder().append("aggregate ").append(byDoc ? "by doc" : "by term").append(" ")
				.append(output).toString());
		for (Path p : inputs) {
			FileInputFormat.addInputPath(job, p);
		}
		job.setInputFormatClass(SequenceFileInputFormat.class);
		job.setMapperClass(AggregateMapper.class);
		job.setCombinerClass(AggregateCombiner.class);
		job.setReducerClass(AggregateReducer.class);
		job.setOutputFormatClass(TextOutputFormat.class);
		job.setOutputKeyClass(Text.class);
		job.setOutputValueClass(AggregateWritable.class);
		FileOutputFormat.setOutputPath(job, output);
		job.waitForCompletion(false);
		Counters counters = job.getCounters();
		Stats res = new Stats();
		CounterGroup cg = counters.getGroup(AggregateTool.class.getName());
		for (Counter c : cg) {
			res.put(c.getName(), Long.valueOf(c.getValue()));
		}
		return res;
	}

	public static void main(String[] args) throws Exception {
		Configuration conf = Utils.createConfiguration();
		GenericOptionsParser gop = new GenericOptionsParser(conf, args);
		int res = ToolRunner.run(gop.getConfiguration(), new AggregateTool(), gop.getRemainingArgs());
		System.exit(res);
	}

	public static class AggregateReducer extends
			Reducer<Text, AggregateTool.AggregateWritable, Text, AggregateTool.AggregateWritable> {
		private AggregateTool.AggregateWritable v = new AggregateTool.AggregateWritable();
		private boolean byDoc;
		private int topN;
		private float minWeight;
		private boolean collapse;

		protected void reduce(Text key, Iterable<AggregateTool.AggregateWritable> values,
				Reducer<Text, AggregateTool.AggregateWritable, Text, AggregateTool.AggregateWritable>.Context ctx)
				throws IOException, InterruptedException {
			if (byDoc) {
				TermFreq.TermFreqQueue tfq = new TermFreq.TermFreqQueue(topN, collapse);
				float curPosWeight = 0.0F;
				float curTimeWeight = 0.0F;
				for (AggregateTool.AggregateWritable a : values) {
					Writable o = a.get();
					if ((o instanceof TermFreq)) {
						tfq.add((TermFreq) o);
						curPosWeight += ((TermFreq) o).posWeight;
						curTimeWeight += ((TermFreq) o).timeWeight;
					} else if ((o instanceof TermFreqs)) {
						for (TermFreq tf : ((TermFreqs) o).tfs) {
							tfq.add(tf);
							curPosWeight += tf.posWeight;
							curTimeWeight += tf.timeWeight;
						}
					} else {
						AggregateTool.LOG.warn("Unexpected value: " + o.getClass().getName() + ": '" + o + "', skipping");
					}
				}
				TermFreq[] arr = tfq.getElements();
				BoostWritable boost = new BoostWritable(arr, curPosWeight, curTimeWeight);
				v.set(boost);
				ctx.write(key, v);
				ctx.getCounter(AggregateTool.class.getName(), "cntDocs").increment(1L);
				ctx.getCounter(AggregateTool.class.getName(), "cntTerms").increment(arr.length);
			} else {
				FloatWritable res = new FloatWritable();
				for (AggregateTool.AggregateWritable a : values) {
					FloatWritable fw = (FloatWritable) a.get();
					res.set(res.get() + fw.get());
				}
				if (res.get() < minWeight) {
					return;
				}
				v.set(res);
				ctx.write(key, v);
				ctx.getCounter(AggregateTool.class.getName(), "cntTerms").increment(1L);
			}
		}

		@Override
		protected void setup(
				Reducer<Text, AggregateTool.AggregateWritable, Text, AggregateTool.AggregateWritable>.Context context)
				throws IOException, InterruptedException {
			super.setup(context);
			byDoc = context.getConfiguration().getBoolean(AggregateTool.BY_DOC, true);
			topN = context.getConfiguration().getInt(AggregateTool.TOP_N, 20);
			minWeight = context.getConfiguration().getFloat(AggregateTool.MIN_WEIGHT, 1.0F);
			collapse = context.getConfiguration().getBoolean(AggregateTool.COLLAPSE, true);
		}
	}

	public static class AggregateCombiner extends
			Reducer<Text, AggregateTool.AggregateWritable, Text, AggregateTool.AggregateWritable> {
		private boolean byDoc;
		private int topN;
		private boolean collapse;
		private AggregateTool.AggregateWritable v = new AggregateTool.AggregateWritable();

		protected void reduce(Text key, Iterable<AggregateTool.AggregateWritable> values,
				Reducer<Text, AggregateTool.AggregateWritable, Text, AggregateTool.AggregateWritable>.Context ctx)
				throws IOException, InterruptedException {
			if (byDoc) {
				TermFreq.TermFreqQueue tfq = new TermFreq.TermFreqQueue(topN * 100, collapse);
				for (AggregateTool.AggregateWritable a : values) {
					if ((a.get() instanceof TermFreqs)) {
						for (TermFreq tf : ((TermFreqs) a.get()).tfs)
							tfq.add(tf);
					} else {
						TermFreq tf = (TermFreq) a.get();
						tfq.add(tf);
					}
				}
				TermFreqs tfs = new TermFreqs(tfq.getElements());
				v.set(tfs);
				ctx.write(key, v);
			} else {
				FloatWritable res = new FloatWritable();
				for (AggregateTool.AggregateWritable a : values) {
					FloatWritable fl = (FloatWritable) a.get();
					res.set(res.get() + fl.get());
				}
				v.set(res);
				ctx.write(key, v);
			}
		}

		@Override
		protected void setup(
				Reducer<Text, AggregateTool.AggregateWritable, Text, AggregateTool.AggregateWritable>.Context context)
				throws IOException, InterruptedException {
			super.setup(context);
			byDoc = context.getConfiguration().getBoolean(AggregateTool.BY_DOC, true);
			topN = context.getConfiguration().getInt(AggregateTool.TOP_N, 20);
			collapse = context.getConfiguration().getBoolean(AggregateTool.COLLAPSE, true);
		}
	}

	public static class AggregateMapper extends Mapper<Text, TermFreqs, Text, AggregateTool.AggregateWritable> {
		private Text k = new Text();
		private AggregateTool.AggregateWritable v = new AggregateTool.AggregateWritable();
		private boolean byDoc;

		@Override
		protected void setup(Mapper<Text, TermFreqs, Text, AggregateTool.AggregateWritable>.Context context)
				throws IOException, InterruptedException {
			super.setup(context);
			byDoc = context.getConfiguration().getBoolean(AggregateTool.BY_DOC, true);
		}

		protected void map(Text key, TermFreqs value,
				Mapper<Text, TermFreqs, Text, AggregateTool.AggregateWritable>.Context context) throws IOException,
				InterruptedException {
			if (byDoc) {
				v.set(value);
				context.write(key, v);
			} else {
				for (TermFreq tf : value.tfs) {
					k.set(tf.term);
					v.set(new FloatWritable(tf.posWeight));
					context.write(k, v);
				}
			}
		}
	}

	public static class AggregateWritable extends GenericWritable {
		private static final Class[] TYPES = { Text.class, FloatWritable.class, TermFreq.class, TermFreqs.class,
				BoostWritable.class };

		protected Class<? extends Writable>[] getTypes() {
			return TYPES;
		}

		public String toString() {
			return get().toString();
		}
	}
}
