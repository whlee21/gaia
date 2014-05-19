package gaia.bigdata.hadoop.simdoc;

import gaia.bigdata.hadoop.GaiaCounters;
import gaia.bigdata.hbase.documents.Document;
import gaia.bigdata.hbase.documents.DocumentKey;
import gaia.bigdata.hbase.documents.DocumentKeySerializer;
import gaia.bigdata.hbase.documents.DocumentTable;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.client.HTableFactory;
import org.apache.hadoop.hbase.client.HTableInterface;
import org.apache.hadoop.hbase.client.HTablePool;
import org.apache.hadoop.hbase.util.Base64;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.MapFile;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.Writable;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Partitioner;
import org.apache.hadoop.mapreduce.lib.input.SequenceFileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.MapFileOutputFormat;
import org.apache.hadoop.mapreduce.lib.partition.HashPartitioner;
import org.apache.hadoop.util.ToolRunner;
import org.apache.mahout.common.AbstractJob;
import org.apache.mahout.math.Vector;
import org.apache.mahout.math.Vector.Element;
import org.apache.mahout.math.VectorWritable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SimdocToHBaseJob extends AbstractJob {
	private static final Logger log = LoggerFactory.getLogger(SimdocToHBaseJob.class);
	public static final String DOC_LOOKUP_DIR = "docLookup";
	public static final String COLLECTION = "collection";
	public static final String ZK = "zk";
	public static final String NUMBER_OF_REGIONS_OPTION = "numberOfRegions";
	public static final String NUMBER_OF_REGIONS = SimdocToHBaseJob.class.getName() + ".numberOfRegions";

	public static void main(String[] args) throws Exception {
		System.exit(ToolRunner.run(new SimdocToHBaseJob(), args));
	}

	public int run(String[] args) throws Exception {
		addInputOption();
		addOutputOption();
		addOption(DOC_LOOKUP_DIR, "d", "The directory containing doc key to doc id mapping", true);
		addOption(COLLECTION, "c", "Collection name", true);
		addOption(ZK, "z", "Zookeeper host and port", true);
		addOption(NUMBER_OF_REGIONS_OPTION, "reg", "Number of HBase regions", false);

		Map<String, List<String>> parsedArgs = parseArguments(args);

		if (parsedArgs == null) {
			return -1;
		}

		Configuration conf = getConf();
		Job job = new Job(conf);
		job.setJobName("SimdocToHBase Size");
		job.setJarByClass(getClass());

		Path docLookupDir = new Path(getOption(DOC_LOOKUP_DIR));
		conf.set(DOC_LOOKUP_DIR, docLookupDir.toString());
		conf.set(COLLECTION, getOption(COLLECTION));
		conf.set(ZK, getOption(ZK));
		conf.set(NUMBER_OF_REGIONS, getOption(NUMBER_OF_REGIONS_OPTION, DocumentKeySerializer.getNumberOfRegions() + ""));

		job.setInputFormatClass(SequenceFileInputFormat.class);

		SequenceFileInputFormat.setInputPaths(job, new Path[] { getInputPath(), docLookupDir });
		FileOutputFormat.setOutputPath(job, getOutputPath());

		job.setMapperClass(SimdocToHBaseMapper.class);
		job.setMapOutputKeyClass(NullWritable.class);
		job.setMapOutputValueClass(NullWritable.class);
		job.setNumReduceTasks(0);
		job.setMapSpeculativeExecution(false);

		return job.waitForCompletion(true) ? 0 : 1;
	}

	public static class SimdocToHBaseMapper extends Mapper<IntWritable, Writable, NullWritable, NullWritable> {
		private MapFile.Reader[] readers;
		private DocumentTable table;
		private HTableInterface hTable;
		private Configuration config;
		private final DocumentKeySerializer keySerializer = new DocumentKeySerializer();

		@Override
		protected void map(IntWritable key, Writable value, Context context) throws IOException {
			if ((value instanceof VectorWritable))
				try {
					Text mainDocId = lookupDocId(key, context);
					Vector vector = ((VectorWritable) value).get();
					SimdocToHBaseJob.log.debug("Main Doc Key: " + key + " | Main Doc ID: " + mainDocId);
					Iterator<Element> it = vector.nonZeroes().iterator();
					Map<String, Double> valueT = new HashMap<String, Double>();
					while (it.hasNext()) {
						Vector.Element e = (Vector.Element) it.next();
						int docIdx = e.index();
						Text docId = lookupDocId(new IntWritable(docIdx), context);
						double similarityScore = e.get();
						byte[] rowKeyBytes = Base64.decode(docId.toString());
						DocumentKey docKey = keySerializer.toObject(rowKeyBytes);
						valueT.put(docKey.id, Double.valueOf(similarityScore));
						SimdocToHBaseJob.log.debug("Similar Doc Key: " + docIdx + " | Similar Doc ID: " + docKey.id
								+ " | Similarity Score: " + similarityScore);
					}

					byte[] rowKeyBytes = Base64.decode(mainDocId.toString());
					DocumentKey docKey = keySerializer.toObject(rowKeyBytes);
					Document doc = table.getDocument(docKey.id, docKey.collection, 4, hTable);
					if (doc == null) {
						doc = new Document(docKey.id, docKey.collection);
					}

					table.setSimilarDocuments(doc, valueT);
					table.putDocument(doc, hTable);
				} catch (Throwable e) {
					SimdocToHBaseJob.log.error("Error getting the Simdocs to HBase: ", e);
				}
		}

		private Text lookupDocId(IntWritable key, Context context) throws IOException {
			Text docId = new Text();
			Partitioner partitioner = new HashPartitioner();
			Writable entry = MapFileOutputFormat.getEntry(readers, partitioner, key, docId);
			if (entry == null) {
				context.getCounter(GaiaCounters.DOC_KEY_NOT_FOUND).increment(1L);
			}
			return docId;
		}

		@Override
		protected void setup(Context context) {
			System.setProperty("regions.number", config.get(SimdocToHBaseJob.NUMBER_OF_REGIONS));

			Configuration hbaseConf = HBaseConfiguration.create();
			hbaseConf.set("hbase.zookeeper.quorum", config.get(ZK));
			HTablePool pool = new HTablePool(hbaseConf, 8, new HTableFactory() {
				public HTableInterface createHTableInterface(Configuration conf, byte[] tableName) {
					HTableInterface table = super.createHTableInterface(conf, tableName);
					if ((table instanceof HTable)) {
						((HTable) table).setAutoFlush(false);
					}
					return table;
				}
			});
			table = new DocumentTable(pool, hbaseConf);
			hTable = pool.getTable(DocumentTable.TABLE);

			config = context.getConfiguration();
			try {
				Path docLookupDir = new Path(config.get(DOC_LOOKUP_DIR));
				FileSystem fs = docLookupDir.getFileSystem(config);
				readers = MapFileOutputFormat.getReaders(docLookupDir, config);
			} catch (IOException e) {
				throw new RuntimeException("Could not setup SimdocToHBaseMapper: " + e);
			}
		}

		@Override
		protected void cleanup(Context context) throws IOException {
			hTable.close();
			table.close();
			closeReaders();
		}

		private void closeReaders() {
			if (readers == null)
				return;
			for (int i = 0; i < readers.length; i++)
				try {
					readers[i].close();
				} catch (Exception e) {
					throw new RuntimeException("Could not close SimdocToHBaseMapper reader: " + e);
				}
		}
	}
}
