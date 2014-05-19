package gaia.bigdata.hadoop.extract;

import gaia.bigdata.hbase.documents.Document;
import gaia.bigdata.hbase.documents.DocumentKeySerializer;
import gaia.bigdata.hbase.documents.DocumentTable;
import gaia.bigdata.hbase.documents.WdTableInputFormat;

import java.io.IOException;
import java.util.List;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.client.HTableFactory;
import org.apache.hadoop.hbase.client.HTableInterface;
import org.apache.hadoop.hbase.client.HTablePool;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.filter.CompareFilter;
import org.apache.hadoop.hbase.filter.SingleColumnValueFilter;
import org.apache.hadoop.hbase.io.ImmutableBytesWritable;
import org.apache.hadoop.hbase.mapreduce.TableMapReduceUtil;
import org.apache.hadoop.hbase.mapreduce.TableMapper;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Writable;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.output.NullOutputFormat;
import org.apache.hadoop.util.ToolRunner;
import org.apache.mahout.common.AbstractJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.digitalpebble.behemoth.Annotation;
import com.digitalpebble.behemoth.BehemothDocument;
import com.digitalpebble.behemoth.tika.TikaProcessor;

public class TikaExtract extends AbstractJob {
	private static final Logger log = LoggerFactory.getLogger(TikaExtract.class);
	public static final String COLLECTION_OPTION = "collection";
	public static final String ZK_CONNECT_OPTION = "zkConnect";
	public static final String TIKA_PROCESSOR_OPTION = "tikaProcessorClass";
	public static final String NUMBER_OF_REGIONS_OPTION = "numberOfRegions";
	private static final String COLLECTION = TikaExtract.class.getName() + ".collection";
	private static final String ZK_CONNECT = TikaExtract.class.getName() + ".zkConnect";
	private static final String TIKA_PROCESSOR = TikaExtract.class.getName() + ".tikaProcessorClass";
	private static final String TIKA_METADATA = "tika.metadata";
	private static final String TIKA_ANNOTATIONS = "tika.annotations";
	private static final String NUMBER_OF_REGIONS = TikaExtract.class.getName() + ".numberOfRegions";

	public int run(String[] args) throws Exception {
		addOption(ZK_CONNECT_OPTION, "zk", "Connection string for ZooKeeper", true);
		addOption(COLLECTION_OPTION, "c", "Collection name", true);
		addOption(TIKA_PROCESSOR_OPTION, "cls", "Custom TikaProcessor subclass", false);
		addOption(NUMBER_OF_REGIONS_OPTION, "reg", "Number of HBase regions", false);

		if (parseArguments(args) == null) {
			return 1;
		}

		String zkConnect = getOption(ZK_CONNECT_OPTION);
		String collection = getOption(COLLECTION_OPTION);
		String tikaClass = getOption(TIKA_PROCESSOR_OPTION, TikaProcessor.class.getCanonicalName());

		Configuration conf = HBaseConfiguration.create(getConf());
		conf.set("hbase.zookeeper.quorum", zkConnect);
		conf.set(ZK_CONNECT, zkConnect);
		conf.set(COLLECTION, collection);
		conf.set(NUMBER_OF_REGIONS, getOption(NUMBER_OF_REGIONS_OPTION, DocumentKeySerializer.getNumberOfRegions() + ""));
		conf.setInt("mapreduce.job.counters.limit", 255);
		try {
			newInstance(tikaClass);
			conf.set(TIKA_PROCESSOR, tikaClass);
			log.info("Using TikaProcessor: {}", tikaClass);
		} catch (Exception e) {
			log.error("Unable to instantiate TikaProcessor class " + tikaClass, e);
			return 1;
		}

		Job job = new Job(conf);
		job.setJarByClass(getClass());
		job.setJobName(getClass().getName());

		DocumentKeySerializer keySerializer = new DocumentKeySerializer();
		byte[][] rowRange = keySerializer.toByteRange(collection);
		Scan scan = new Scan(rowRange[0], rowRange[1]);
		scan.setBatch(100);

		SingleColumnValueFilter filter = new SingleColumnValueFilter(DocumentTable.TEXT_CF, DocumentTable.TEXT_CONTENT_CQ,
				CompareFilter.CompareOp.EQUAL, Bytes.toBytes(""));

		filter.setFilterIfMissing(false);
		scan.setFilter(filter);
		scan.addFamily(DocumentTable.TEXT_CF);
		scan.addFamily(DocumentTable.RAW_CF);
		scan.addFamily(DocumentTable.INFO_CF);
		log.info("Using Scan: {}", scan.toString());

		TableMapReduceUtil.initTableMapperJob(DocumentTable.TABLE, scan, Map.class, NullWritable.class, NullWritable.class,
				job);

		job.setInputFormatClass(WdTableInputFormat.class);

		job.setOutputFormatClass(NullOutputFormat.class);
		job.setNumReduceTasks(0);
		job.setMapSpeculativeExecution(false);

		return job.waitForCompletion(true) ? 0 : 1;
	}

	public static TikaProcessor newInstance(String className) throws IOException {
		try {
			Class clazz = Class.forName(className);
			return (TikaProcessor) clazz.newInstance();
		} catch (Exception e) {
			throw new IOException(e.getCause());
		}
	}

	public static void main(String[] args) throws Exception {
		System.exit(ToolRunner.run(new TikaExtract(), args));
	}

	public static class Map extends TableMapper<NullWritable, NullWritable> {
		private final DocumentKeySerializer keySerializer = new DocumentKeySerializer();
		private final BehemothDocument behemothDoc = new BehemothDocument();
		private TikaProcessor processor;
		private DocumentTable table;
		private HTableInterface hTable;

		@Override
		protected void setup(Context ctx) throws IOException {
			processor = TikaExtract.newInstance(ctx.getConfiguration().get(TikaExtract.TIKA_PROCESSOR));
			processor.setConf(ctx.getConfiguration());
			System.setProperty("java.awt.headless", "true");
			System.setProperty("regions.number", ctx.getConfiguration().get(TikaExtract.NUMBER_OF_REGIONS));

			Configuration hbaseConf = HBaseConfiguration.create();
			hbaseConf.set("hbase.zookeeper.quorum", ctx.getConfiguration().get(TikaExtract.ZK_CONNECT));
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
			hTable.close();
			table.close();
		}

		@Override
		protected void map(ImmutableBytesWritable row, Result result, Context ctx) throws IOException, InterruptedException {
			Document doc = DocumentTable.resultToDocument(result);
			TikaExtract.log.debug(doc.toString());

			if (doc.text == null) {
				if (doc.content == null) {
					TikaExtract.log.error("Cannot extract text, doc has no content");
					ctx.getCounter(TikaExtract.Counters.NO_CONTENT_OR_TEXT).increment(1L);
				} else {
					ctx.getCounter(TikaExtract.Counters.PROCESSED_DOCS).increment(1L);
					process(doc, ctx);
				}
			} else {
				TikaExtract.log.debug("Skipping this doc, already has text");
				ctx.getCounter(TikaExtract.Counters.ALREADY_HAS_TEXT).increment(1L);
			}
		}

		protected void process(Document doc, Context ctx) throws IOException {
			behemothDoc.setUrl(doc.id);
			behemothDoc.setContent(doc.content);
			behemothDoc.setText(doc.text);
			behemothDoc.setContentType(doc.contentType);
			try {
				processor.process(behemothDoc, null);
			} catch (OutOfMemoryError e) {
				System.gc();
				ctx.getCounter(TikaExtract.Counters.FORCED_GC).increment(1L);
				ctx.getCounter(TikaExtract.Counters.EXTRACT_FAILED).increment(1L);
				TikaExtract.log.error("Error processing document: " + doc.id + " due to an OOM, forcing a GC", e);
			} catch (Throwable e) {
				ctx.getCounter(TikaExtract.Counters.EXTRACT_FAILED).increment(1L);
				TikaExtract.log.error("Error processing document: " + doc.id, e);
			}

			if (ctx.getConfiguration().getBoolean(TIKA_METADATA, false)) {
				for (java.util.Map.Entry<Writable, Writable> entry : behemothDoc.getMetadata(true).entrySet()) {
					doc.fields.put("tika." + ((Writable) entry.getKey()).toString(), ((Writable) entry.getValue()).toString());
				}
			}

			if (ctx.getConfiguration().getBoolean(TIKA_ANNOTATIONS, false)) {
				List<Annotation> annotations = behemothDoc.getAnnotations();
				if ((annotations != null) && (annotations.size() > 0)) {
					doc.annotations.addAll(annotations);
				}

			}

			if (behemothDoc.getText() != null)
				doc.text = behemothDoc.getText().trim();
			else {
				doc.text = null;
			}
			doc.contentType = behemothDoc.getContentType();
			try {
				table.putDocument(doc, hTable);
			} catch (Throwable e) {
				ctx.getCounter(TikaExtract.Counters.PUT_DOCUMENT_FAILED).increment(1L);
				TikaExtract.log.error("Exception in putDocument for doc: {}", doc.id, e);
			}
		}
	}

	public static enum Counters {
		NO_CONTENT_OR_TEXT, ALREADY_HAS_TEXT, PROCESSED_DOCS, FORCED_GC, EXTRACT_FAILED, PUT_DOCUMENT_FAILED;
	}
}
