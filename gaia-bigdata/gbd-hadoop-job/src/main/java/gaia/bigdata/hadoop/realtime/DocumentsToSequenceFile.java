package gaia.bigdata.hadoop.realtime;

import gaia.bigdata.hbase.documents.Document;
import gaia.bigdata.hbase.documents.DocumentKeySerializer;
import gaia.bigdata.hbase.documents.DocumentTable;
import gaia.bigdata.hbase.documents.WdTableInputFormat;

import java.io.IOException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.filter.CompareFilter;
import org.apache.hadoop.hbase.filter.SingleColumnValueFilter;
import org.apache.hadoop.hbase.io.ImmutableBytesWritable;
import org.apache.hadoop.hbase.mapreduce.TableMapReduceUtil;
import org.apache.hadoop.hbase.mapreduce.TableMapper;
import org.apache.hadoop.hbase.util.Base64;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.output.SequenceFileOutputFormat;
import org.apache.hadoop.util.ToolRunner;
import org.apache.mahout.common.AbstractJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DocumentsToSequenceFile extends AbstractJob {
	private static final Logger log = LoggerFactory.getLogger(DocumentsToSequenceFile.class);
	public static final String COLLECTION_OPTION = "collection";
	public static final String ZK_CONNECT_OPTION = "zkConnect";
	public static final String NUMBER_OF_REGIONS_OPTION = "numberOfRegions";
	private static final String COLLECTION = DocumentsToSequenceFile.class.getName() + ".collection";
	private static final String ZK_CONNECT = DocumentsToSequenceFile.class.getName() + ".zkConnect";
	private static final String NUMBER_OF_REGIONS = DocumentsToSequenceFile.class.getName() + ".numberOfRegions";

	public int run(String[] args) throws Exception {
		addOption("zkConnect", "zk", "Connection string for ZooKeeper", true);
		addOption("collection", "c", "Collection name", true);
		addOption("numberOfRegions", "reg", "Number of HBase regions", false);

		addOutputOption();

		if (parseArguments(args) == null) {
			return 1;
		}

		String zkConnect = getOption("zkConnect");
		String collection = getOption("collection");
		Path output = getOutputPath();

		Configuration conf = HBaseConfiguration.create(getConf());
		conf.set("hbase.zookeeper.quorum", zkConnect);
		conf.set(ZK_CONNECT, zkConnect);
		conf.set(COLLECTION, collection);
		conf.set(NUMBER_OF_REGIONS, getOption("numberOfRegions", DocumentKeySerializer.getNumberOfRegions() + ""));

		Job job = new Job(conf);
		job.setJarByClass(getClass());
		job.setJobName(getClass().getName());

		DocumentKeySerializer keySerializer = new DocumentKeySerializer();
		byte[][] rowRange = keySerializer.toByteRange(collection);
		Scan scan = new Scan(rowRange[0], rowRange[1]);
		scan.setBatch(100);

		SingleColumnValueFilter filter = new SingleColumnValueFilter(DocumentTable.TEXT_CF, DocumentTable.TEXT_CONTENT_CQ,
				CompareFilter.CompareOp.NOT_EQUAL, Bytes.toBytes(""));

		filter.setFilterIfMissing(true);
		scan.setFilter(filter);
		scan.addFamily(DocumentTable.INFO_CF);
		scan.addFamily(DocumentTable.TEXT_CF);
		log.info("Using Scan: {}", scan.toString());

		TableMapReduceUtil.initTableMapperJob(DocumentTable.TABLE, scan, Map.class, Text.class, Text.class, job);

		System.setProperty("regions.number", conf.get(NUMBER_OF_REGIONS));

		job.setInputFormatClass(WdTableInputFormat.class);

		job.setOutputFormatClass(SequenceFileOutputFormat.class);
		SequenceFileOutputFormat.setOutputPath(job, output);
		job.setOutputKeyClass(Text.class);
		job.setOutputValueClass(Text.class);
		job.setNumReduceTasks(0);
		job.setMapSpeculativeExecution(false);

		return job.waitForCompletion(true) ? 0 : 1;
	}

	public static void main(String[] args) throws Exception {
		ToolRunner.run(new DocumentsToSequenceFile(), args);
	}

	public static class Map extends TableMapper<Text, Text> {
		private final Text keyReuse = new Text();
		private final Text valueReuse = new Text();

		@Override
		protected void map(ImmutableBytesWritable row, Result result, Context ctx) throws IOException, InterruptedException {
			Document doc = DocumentTable.resultToDocument(result);
			DocumentsToSequenceFile.log.debug(doc.toString());

			String text = doc.text;
			if (text == null) {
				ctx.getCounter(DocumentsToSequenceFile.Counters.NO_TEXT).increment(1L);
				return;
			}
			keyReuse.clear();
			keyReuse.set(Base64.encodeBytes(row.get(), 8));
			valueReuse.clear();
			valueReuse.set(text);
			ctx.write(keyReuse, valueReuse);
		}
	}

	static enum Counters {
		NO_TEXT;
	}
}
