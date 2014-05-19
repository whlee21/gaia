package gaia.solr.click.log;

import java.io.IOException;
import java.io.StringReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
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
import org.apache.hadoop.mapreduce.lib.output.SequenceFileOutputFormat;
import org.apache.hadoop.util.GenericOptionsParser;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.core.StopAnalyzer;
import org.apache.lucene.analysis.shingle.ShingleFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.TypeAttribute;
import org.apache.lucene.util.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gaia.solr.click.Utils;

public class PrepareTool extends Configured implements Tool {
	private static final Logger LOG = LoggerFactory.getLogger(PrepareTool.class);

	public static final String SHINGLE_SIZE = PrepareTool.class.getName() + ".shingleSize";
	public static final String CNT_QUERIES = "queries";
	public static final String CNT_CLICKS = "clicks";

	public int run(String[] args) throws Exception {
		if (args.length < 2) {
			System.err.println("Usage: PrepareTool [-shingles NN] [-aol] <outputDir> <inputDir1> [<inputDir2> ...]");
			System.err.println("\tshingles NN\tproduce shingles up to NN terms long (default is 2)");
			System.err.println("\taol\tlogs are in the AOL format");
			return -1;
		}
		Path out = null;
		int shingleSize = 2;
		boolean aol = false;
		ArrayList<Path> inputs = new ArrayList<Path>();
		for (int i = 0; i < args.length; i++) {
			if (args[i].equals("-shingles"))
				shingleSize = Integer.parseInt(args[(++i)]);
			else if (args[i].equals("-aol")) {
				aol = true;
			} else if (out == null) {
				out = new Path(args[i]);
			} else {
				inputs.add(new Path(args[i]));
			}
		}
		prepare((Path[]) inputs.toArray(new Path[0]), out, shingleSize, aol);
		return 0;
	}

	public Stats prepare(Path[] inputs, Path out, int shingleSize, boolean formatAol) throws Exception {
		Job job = new Job(getConf());
		job.setJobName("prepare " + out);
		job.setJarByClass(PrepareTool.class);
		for (Path in : inputs) {
			FileInputFormat.addInputPath(job, in);
		}
		job.getConfiguration().setInt(SHINGLE_SIZE, shingleSize);
		job.setInputFormatClass(TextInputFormat.class);
		if (formatAol)
			job.setMapperClass(PrepareAOLMapper.class);
		else {
			job.setMapperClass(PrepareMapper.class);
		}
		job.setReducerClass(PrepareReducer.class);
		job.setMapOutputKeyClass(CompoundKey.class);
		job.setMapOutputValueClass(Click.class);
		job.setPartitionerClass(CompoundKey.CompoundKeyPartitioner.class);
		job.setOutputKeyClass(Text.class);
		job.setOutputValueClass(TermFreqs.class);
		job.setGroupingComparatorClass(CompoundKey.CompoundGroupingComparator.class);
		job.setSortComparatorClass(CompoundKey.CompoundSortComparator.class);
		FileOutputFormat.setOutputPath(job, out);
		job.setOutputFormatClass(SequenceFileOutputFormat.class);
		job.waitForCompletion(true);
		Counters counters = job.getCounters();
		Stats res = new Stats();
		CounterGroup cg = counters.getGroup(PrepareTool.class.getName());
		for (Counter c : cg) {
			res.put(c.getName(), Long.valueOf(c.getValue()));
		}
		return res;
	}

	public static void main(String[] args) throws Exception {
		Configuration conf = Utils.createConfiguration();
		GenericOptionsParser gop = new GenericOptionsParser(conf, args);
		int res = ToolRunner.run(gop.getConfiguration(), new PrepareTool(), gop.getRemainingArgs());
		System.exit(res);
	}

	public static class PrepareReducer extends Reducer<CompoundKey, Click, Text, TermFreqs> {
		private final Set<String> STOP_WORDS = new HashSet<String>();
		int shingleSize;
		Text outKey = new Text();
		int batchSize = 1000;
		HashMap<String, Map<String, TermFreq>> docMap = new HashMap<String, Map<String, TermFreq>>(batchSize);

		@Override
		protected void setup(Reducer<CompoundKey, Click, Text, TermFreqs>.Context context) throws IOException,
				InterruptedException {
			super.setup(context);
			shingleSize = context.getConfiguration().getInt(PrepareTool.SHINGLE_SIZE, 2);
			for (Object o : StopAnalyzer.ENGLISH_STOP_WORDS_SET) {
				STOP_WORDS.add(new String((char[]) o));
			}

			STOP_WORDS.add("he");
			STOP_WORDS.add("how");
			STOP_WORDS.add("she");
			STOP_WORDS.add("i");
			STOP_WORDS.add("s");
			STOP_WORDS.add("what");
			STOP_WORDS.add("my");
			STOP_WORDS.add("me");
			STOP_WORDS.add("new");
			STOP_WORDS.add("you");
			STOP_WORDS.add("do");
			STOP_WORDS.add("up");
			STOP_WORDS.add("from");
			STOP_WORDS.add("www");
			STOP_WORDS.add("com");
			STOP_WORDS.add("aol");
			STOP_WORDS.add("google");
			STOP_WORDS.add("yahoo");
			STOP_WORDS.add("msn");
			STOP_WORDS.add("http");
			STOP_WORDS.add("ebay");
			STOP_WORDS.add("free");
			STOP_WORDS.add("click");
			STOP_WORDS.add("like");
		}

		protected void cleanup(Reducer<CompoundKey, Click, Text, TermFreqs>.Context context) throws IOException,
				InterruptedException {
			writeBatch(context);
			super.cleanup(context);
		}

		private void writeBatch(Reducer<CompoundKey, Click, Text, TermFreqs>.Context context) throws IOException,
				InterruptedException {
			for (Map.Entry<String, Map<String, TermFreq>> e : docMap.entrySet()) {
				outKey.set((String) e.getKey());
				TermFreq[] freqs = e.getValue().values().toArray(new TermFreq[0]);
				TermFreqs v = new TermFreqs(freqs);

				context.write(outKey, v);
			}
			docMap.clear();
		}

		protected void reduce(CompoundKey key, Iterable<Click> values,
				Reducer<CompoundKey, Click, Text, TermFreqs>.Context context) throws IOException, InterruptedException {
			Iterator<Click> it = values.iterator();
			Click clk = (Click) it.next();

			if (clk.getTime() >= 0L) {
				PrepareTool.LOG.warn("Missing main query - skipping " + clk + ", recId=" + key);
				while (it.hasNext()) {
					PrepareTool.LOG.warn(" - entry " + it.next() + ", recId=" + key);
				}

				return;
			}
			String query = clk.getId().toString();
			Set<String> shingles = makeGrams(query, shingleSize);
			if ((shingles == null) || (shingles.size() == 0)) {
				return;
			}
			long queryTime = -clk.getTime();
			int numHits = clk.getPosition();
			long readingStart = queryTime;
			int numClicks = 0;
			String lastId = null;
			HashMap<String, Map<String, TermFreq>> curDocMap = new HashMap<String, Map<String, TermFreq>>(batchSize);
			Map<String, TermFreq> tfs;
			while (it.hasNext()) {
				clk = (Click) it.next();

				if (clk.getTime() < 0L) {
					if ((!clk.getId().toString().equals(query)) || (clk.getTime() != -queryTime)) {
						PrepareTool.LOG.warn("Mis-sorted entry: " + clk + ", recId=" + key + ", main query=" + query + ", qtime="
								+ queryTime);
					}
				} else {
					numClicks++;
					assert (clk.getTime() > queryTime);
					long readingTime = clk.getTime() - readingStart;
					readingStart = clk.getTime();

					if (lastId != null) {
						Map<String, Object> tfsPrev = (Map) curDocMap.get(lastId);
						if (tfsPrev != null) {
							for (Map.Entry e : tfsPrev.entrySet()) {
								((TermFreq) e.getValue()).timeWeight += readingTimeWeight(readingTime);
							}
						}
					}
					tfs = (Map) curDocMap.get(clk.getId().toString());
					if (tfs == null) {
						tfs = new HashMap<String, TermFreq>();
						curDocMap.put(clk.getId().toString(), tfs);
					}
					for (String s : shingles) {
						TermFreq tf = (TermFreq) tfs.get(s);
						if (tf == null) {
							tf = new TermFreq(s, 0.0F, 0.0F);
							tfs.put(s, tf);
						}
						tf.posWeight += weight(clk.getPosition());
					}
				}
			}
			float w;
			if (numClicks > 0) {
				w = readingTimeWeight((readingStart - queryTime) / numClicks);
				Map<String, Object> tfsPrev = (Map) curDocMap.get(lastId);
				if (tfsPrev != null) {
					for (Map.Entry e : tfsPrev.entrySet()) {
						((TermFreq) e.getValue()).timeWeight += w;
					}
				}
			}

			tfs = new HashMap<String, TermFreq>();
			curDocMap.put("*", tfs);
			for (String s : shingles) {
				TermFreq tf = new TermFreq(s, weight(0), 0.0F);
				tfs.put(tf.term, tf);
			}
			// <String, Map<String, TermFreq>>
			for (Map.Entry<String, Map<String, TermFreq>> e : curDocMap.entrySet()) {
				Map<String, Object> oldFreqs = (Map) docMap.get(e.getKey());
				if (oldFreqs == null)
					docMap.put(e.getKey(), e.getValue());
				else
					for (Map.Entry<String, TermFreq> tf : e.getValue().entrySet()) {
						TermFreq old = (TermFreq) oldFreqs.get(tf.getKey());
						if (old != null) {
							old.posWeight += ((TermFreq) tf.getValue()).posWeight;
							old.timeWeight += ((TermFreq) tf.getValue()).timeWeight;
						} else {
							oldFreqs.put(tf.getKey(), tf.getValue());
						}
					}
			}
			if (docMap.size() >= batchSize)
				writeBatch(context);
		}

		private float weight(int position) {
			return (float) (Math.log1p(position) * 0.5D + 0.5D);
		}

		private float readingTimeWeight(long time) {
			return (float) Math.log1p(time / 1000L / 10L);
		}

		private Set<String> makeGrams(String query, int ngramSize) throws IOException {
			TokenStream ts = new StandardTokenizer(Version.LUCENE_45, new StringReader(query));
			ts = new ShingleFilter(ts, ngramSize);
			CharTermAttribute att = (CharTermAttribute) ts.addAttribute(CharTermAttribute.class);
			TypeAttribute type = (TypeAttribute) ts.addAttribute(TypeAttribute.class);
			ts.reset();
			HashSet<String> grams = new HashSet<String>();
			try {
				while (ts.incrementToken()) {
					String term = new String(att.buffer(), 0, att.length());
					if (!type.type().equals("shingle")) {
						if (!STOP_WORDS.contains(term))
							;
					} else {
						String[] terms = term.split(" ");
						boolean nonStop = false;
						for (String s : terms) {
							if (!STOP_WORDS.contains(s)) {
								nonStop = true;
								break;
							}
						}
						if (nonStop) {
							grams.add(term);
						}
					}
				}
				ts.end();
				ts.close();
			} catch (IOException e) {
				e.printStackTrace();
				PrepareTool.LOG.warn("Failed to produce ngrams", e);
				return null;
			}
			return grams;
		}
	}

	public static class PrepareMapper extends Mapper<LongWritable, Text, CompoundKey, Click> {
		CompoundKey recId = new CompoundKey();
		Click clk = new Click();

		protected void map(LongWritable key, Text value, Mapper<LongWritable, Text, CompoundKey, Click>.Context context)
				throws IOException, InterruptedException {
			String v = value.toString();
			Click clk = null;
			try {
				if (v.charAt(0) == 'Q') {
					clk = processQueryRecord(v);
					context.getCounter(PrepareTool.class.getName(), "queries").increment(1L);
				} else if (v.charAt(0) == 'C') {
					clk = processClickRecord(v);
					context.getCounter(PrepareTool.class.getName(), "clicks").increment(1L);
				} else {
					return;
				}
			} catch (Exception e) {
				PrepareTool.LOG.warn("Error parsing record: '" + v + "'", e);
				return;
			}
			if (clk == null) {
				return;
			}
			context.write(recId, clk);
		}

		private Click processQueryRecord(String v) {
			String[] fields = v.split("\t");

			if (fields.length < 5) {
				return null;
			}
			int numHits = Integer.parseInt(fields[4]);
			if (numHits == 0) {
				return null;
			}
			long time = Long.parseLong(fields[1]);
			int idx = fields[3].indexOf('~');
			String uid;
			if (idx == -1)
				uid = "none";
			else {
				uid = fields[3].substring(0, idx);
			}
			recId.userId.set(uid);
			recId.queryId.set(fields[3]);
			recId.val = (-time);
			clk.setId(fields[2]);
			clk.setTime(-time);
			clk.setPosition(numHits);
			return clk;
		}

		private Click processClickRecord(String v) {
			String[] fields = v.split("\t");

			if (fields.length < 5) {
				return null;
			}
			int idx = fields[2].indexOf('~');
			String uid;
			if (idx == -1)
				uid = "none";
			else {
				uid = fields[2].substring(0, idx);
			}
			long time = Long.parseLong(fields[1]);
			recId.queryId.set(fields[2]);
			recId.userId.set(uid);
			recId.val = time;
			clk.setId(fields[3]);
			clk.setTime(time);
			clk.setPosition(Integer.parseInt(fields[4]));
			return clk;
		}
	}

	public static class PrepareAOLMapper extends Mapper<LongWritable, Text, CompoundKey, Click> {
		CompoundKey recId = new CompoundKey();
		Click clk = new Click();

		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

		protected void map(LongWritable key, Text value, Mapper<LongWritable, Text, CompoundKey, Click>.Context context)
				throws IOException, InterruptedException {
			String[] fields = value.toString().split("\t");
			long time = 0L;
			try {
				Date date = sdf.parse(fields[2]);
				time = date.getTime();
			} catch (Exception e) {
				PrepareTool.LOG.warn("Invalid date: " + fields[2] + " - skipping");
				return;
			}
			String rec = Utils.createRequestId(fields[0], time, fields[1]);
			recId.queryId.set(rec);
			recId.userId.set(fields[0]);
			recId.val = (-time);
			clk.setId(fields[1]);
			clk.setTime(-time);
			clk.setPosition(1000);
			context.write(recId, clk);

			if ((fields.length > 3) && (fields[3].length() > 0)) {
				recId.queryId.set(rec);
				recId.val = time;
				clk.setId(fields[4]);
				clk.setTime(time);
				try {
					clk.setPosition(Integer.parseInt(fields[3]));
				} catch (Exception e) {
					PrepareTool.LOG.warn("Invalid position: '" + fields[3] + "' - skipping");
					return;
				}
				context.write(recId, clk);
			}
		}
	}
}
