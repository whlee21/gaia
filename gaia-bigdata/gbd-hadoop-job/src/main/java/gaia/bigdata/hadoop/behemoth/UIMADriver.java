package gaia.bigdata.hadoop.behemoth;

import gaia.bigdata.hbase.documents.Document;
import gaia.bigdata.hbase.documents.DocumentKey;
import gaia.bigdata.hbase.documents.DocumentKeySerializer;
import gaia.bigdata.hbase.documents.DocumentTable;

import java.io.IOException;
import java.net.URI;
import java.util.LinkedList;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.filecache.DistributedCache;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.client.HTableFactory;
import org.apache.hadoop.hbase.client.HTableInterface;
import org.apache.hadoop.hbase.client.HTablePool;
import org.apache.hadoop.hbase.util.Base64;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.input.SequenceFileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.NullOutputFormat;
import org.apache.hadoop.util.ToolRunner;
import org.apache.mahout.common.AbstractJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.digitalpebble.behemoth.BehemothDocument;
import com.digitalpebble.behemoth.uima.UIMAMapper;

public class UIMADriver extends AbstractJob {
	private static final Logger log = LoggerFactory.getLogger(UIMADriver.class);
	public static final String ZK_CONNECT_OPTION = "zkConnect";
	public static final String PEAR_PATH_OPTION = "pearPath";
	public static final String NUMBER_OF_REGIONS_OPTION = "numberOfRegions";
	private static final String ZK_CONNECT = UIMADriver.class.getName() + ".zkConnect";
	private static final String PEAR_PATH = UIMADriver.class.getName() + ".pearPath";
	private static final String NUMBER_OF_REGIONS = UIMADriver.class.getName() + ".numberOfRegions";

	public static void main(String[] args) throws Exception {
		System.exit(ToolRunner.run(new UIMADriver(), args));
	}

	public int run(String[] args) throws Exception {
		addInputOption();
		addOption("zkConnect", "zk", "Connection string for ZooKeeper", true);
		addOption("pearPath", "p", "Path to PEAR file", true);
		addOption("numberOfRegions", "reg", "Number of HBase regions", false);

		if (parseArguments(args) == null) {
			return 1;
		}

		String pearPath = getOption("pearPath");
		String zkConnect = getOption("zkConnect");

		Path zap = new Path(pearPath);
		FileSystem fs = zap.getFileSystem(new Configuration());
		if (!fs.exists(zap)) {
			log.error("The UIMA application " + pearPath + "can't be found on HDFS - aborting");
			return 1;
		}

		Configuration conf = HBaseConfiguration.create(getConf());
		conf.set("hbase.zookeeper.quorum", zkConnect);
		conf.set(ZK_CONNECT, zkConnect);
		conf.set(PEAR_PATH, pearPath);
		conf.set(NUMBER_OF_REGIONS, getOption("numberOfRegions", DocumentKeySerializer.getNumberOfRegions() + ""));

		Job job = new Job(conf);
		job.setJarByClass(getClass());
		job.setJobName(getClass().getName());

		DistributedCache.addCacheFile(new URI(pearPath), job.getConfiguration());

		job.setInputFormatClass(SequenceFileInputFormat.class);
		SequenceFileInputFormat.addInputPath(job, getInputPath());
		job.setMapperClass(Map.class);
		job.setOutputKeyClass(NullWritable.class);
		job.setOutputValueClass(NullWritable.class);
		job.setOutputFormatClass(NullOutputFormat.class);
		job.setNumReduceTasks(0);
		job.setMapSpeculativeExecution(false);

		return job.waitForCompletion(true) ? 0 : 1;
	}

	public static class Map extends UIMAMapper {
		// private final Mapper delegate = new UIMAMapper();
		private final UIMADriver.LinkedOutputCollector<Text, BehemothDocument> collector = new UIMADriver.LinkedOutputCollector<Text, BehemothDocument>();
		private final DocumentKeySerializer keySerializer = new DocumentKeySerializer();
		private final Text url = new Text();
		private final BehemothDocument behemothDoc = new BehemothDocument();
		private DocumentTable table;
		private HTableInterface hTable;

		@Override
		protected void setup(Context ctx) throws IOException {
			System.setProperty("regions.number", ctx.getConfiguration().get(UIMADriver.NUMBER_OF_REGIONS));

			Configuration conf = ctx.getConfiguration();
			conf.set("uima.pear.path", conf.get(UIMADriver.PEAR_PATH));

			super.setup(ctx);

			Configuration hbaseConf = HBaseConfiguration.create();
			hbaseConf.set("hbase.zookeeper.quorum", ctx.getConfiguration().get(UIMADriver.ZK_CONNECT));
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
		}

		@Override
		protected void cleanup(Context ctx) throws IOException {
			super.cleanup(ctx);
			hTable.close();
			table.close();
		}
		
		protected void map(Text row, Text text, Context ctx) throws IOException, InterruptedException {
			byte[] rowBytes = Base64.decode(row.toString());
			DocumentKey key = keySerializer.toObject(rowBytes);
			Document doc = table.getDocument(key.id, key.collection, 16, hTable);
			if (doc == null) {
				UIMADriver.log.warn("Document {} not found", key);
				ctx.getCounter(UIMADriver.Counters.DOC_NOT_FOUND).increment(1L);
				return;
			}

			int annotationCount1 = 0;
			if (doc.annotations != null) {
				annotationCount1 = doc.annotations.size();
			}

			url.set(doc.id);
			behemothDoc.setUrl(doc.id);
			behemothDoc.setText(text.toString());
			behemothDoc.setAnnotations(doc.annotations);

			// delegate.map(url, behemothDoc, ctx);
			super.map(url, behemothDoc, ctx);
			collector.collect(url, behemothDoc);
			assert (collector.size() == 1);
			BehemothDocument docWithAnnotations = (BehemothDocument) collector.poll();

			doc.annotations = docWithAnnotations.getAnnotations();
			int annotationCount2 = 0;
			if (doc.annotations != null) {
				annotationCount2 = doc.annotations.size();
			}
			ctx.getCounter(UIMADriver.Counters.TOTAL_ANNOTATIONS).increment(annotationCount2);
			ctx.getCounter(UIMADriver.Counters.NEW_ANNOTATIONS).increment(annotationCount2 - annotationCount1);
			table.putDocument(doc, hTable);
			ctx.getCounter(UIMADriver.Counters.ANNOTATED_DOCS).increment(1L);
		}
	}

	private static class LinkedOutputCollector<K, V> extends LinkedList<V> {
		public void collect(K key, V value) throws IOException {
			super.add(value);
		}
	}

	static enum Counters {
		ANNOTATED_DOCS, TOTAL_ANNOTATIONS, NEW_ANNOTATIONS, DOC_NOT_FOUND;
	}
}
