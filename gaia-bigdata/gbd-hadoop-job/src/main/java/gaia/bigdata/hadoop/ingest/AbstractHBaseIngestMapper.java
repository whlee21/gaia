package gaia.bigdata.hadoop.ingest;

import gaia.bigdata.hbase.documents.Document;
import gaia.bigdata.hbase.documents.DocumentTable;
import gaia.bigdata.util.ZooKeeperUtil;

import java.io.IOException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.client.HTableFactory;
import org.apache.hadoop.hbase.client.HTableInterface;
import org.apache.hadoop.hbase.client.HTablePool;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Writable;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.zookeeper.client.ConnectStringParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractHBaseIngestMapper<K extends Writable, V extends Writable> extends
		Mapper<K, V, NullWritable, NullWritable> {
	protected static final Logger log = LoggerFactory.getLogger(AbstractHBaseIngestMapper.class);

	public static final String COLLECTION = AbstractHBaseIngestMapper.class.getName() + ".collection";
	public static final String MIME_TYPE = AbstractHBaseIngestMapper.class.getName() + ".mimeType";
	public static final String ZK_CONNECT = AbstractHBaseIngestMapper.class.getName() + ".zkConnect";
	public static final String TEMP_DIR = AbstractHBaseIngestMapper.class.getName() + ".tmpDir";
	public static final String HBASE_THREADS = AbstractHBaseIngestMapper.class.getName() + ".hbaseNumThreads";
	public static final String OVERWRITE = AbstractHBaseIngestMapper.class.getName() + ".overwrite";
	public static final String NUMBER_OF_REGIONS = AbstractHBaseIngestMapper.class.getName() + ".numberOfRegions";
	public static final int DEFAULT_HBASE_THREADS = 8;
	private Configuration conf;
	private DocumentTable table;
	private HTableInterface hTable;
	private boolean overwrite = true;

	@Override
	protected void setup(Context context) {
		System.setProperty("regions.number", conf.get(NUMBER_OF_REGIONS));
		conf = context.getConfiguration();
		Configuration hbaseConf = HBaseConfiguration.create();
		String connectStr = conf.get(ZK_CONNECT);
		if (connectStr == null) {
			throw new RuntimeException("No " + ZK_CONNECT + " property set for Ingest");
		}
		ConnectStringParser parser = new ConnectStringParser(connectStr);
		hbaseConf.set("hbase.zookeeper.quorum", ZooKeeperUtil.toHBaseQuorumAddresses(parser));
		hbaseConf.set("hbase.zookeeper.property.clientPort", String.valueOf(ZooKeeperUtil.toHBaseClientPort(parser)));
		overwrite = conf.getBoolean(OVERWRITE, true);
		HTablePool pool = new HTablePool(hbaseConf, conf.getInt(HBASE_THREADS, 8), new HTableFactory() {
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
	protected final void cleanup(Context context) throws IOException {
		hTable.close();
		table.close();
	}

	public final void map(K key, V value, Context context) throws IOException {
		Document[] documents = toDocuments(key, value, context);
		if ((documents != null) && (documents.length > 0)) {
			for (Document doc : documents)
				try {
					table.putDocument(doc, hTable, overwrite);
					context.getCounter(Counters.DOCS_ADDED).increment(1L);
				} catch (Exception e) {
					log.error("Could not add document " + doc.id, e);
					context.getCounter(Counters.DOCS_PUT_FAILED).increment(1L);
				}
		} else {
			log.warn("No documents were created for key: {}", key);
			context.getCounter(Counters.DOCS_CONVERT_FAILED).increment(1L);
		}
	}

	public final String getCollection() {
		return conf.get(COLLECTION);
	}

	protected abstract Document[] toDocuments(K paramK, V paramV, Context context)
			throws IOException;

	public abstract AbstractJobFixture getFixture();

	public static enum Counters {
		DOCS_PUT_FAILED, DOCS_ADDED, DOCS_CONVERT_FAILED;
	}
}
