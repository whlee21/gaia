package gaia.bigdata.hbase;

import java.io.Closeable;
import java.io.IOException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.HTableDescriptor;
import org.apache.hadoop.hbase.MasterNotRunningException;
import org.apache.hadoop.hbase.TableExistsException;
import org.apache.hadoop.hbase.client.HBaseAdmin;
import org.apache.hadoop.hbase.client.HTableFactory;
import org.apache.hadoop.hbase.client.HTablePool;
import org.apache.zookeeper.client.ConnectStringParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gaia.bigdata.util.ZooKeeperUtil;

public abstract class HBaseTable implements Closeable {
	private static transient Logger log = LoggerFactory.getLogger(HBaseTable.class);
	protected final HTablePool pool;
	public static final int DEFAULT_HBASE_THREADS = 8;

	public HBaseTable(String zkConnect) {
		pool = openTablePool(zkConnect);
	}

	public HBaseTable(HTablePool pool, Configuration conf) {
		try {
			bootstrap(conf);
		} catch (Exception e) {
			throw new RuntimeException("Could not bootstrap table " + getClass().getName(), e);
		}
		this.pool = pool;
	}

	public void close() throws IOException {
		pool.close();
	}

	protected HTablePool openTablePool(String zkConnect) {
		ConnectStringParser parse = new ConnectStringParser(zkConnect);
		if (parse.getServerAddresses().isEmpty()) {
			throw new RuntimeException("zkHost not properly set");
		}

		Configuration hbaseConfig = HBaseConfiguration.create();

		hbaseConfig.set("hbase.zookeeper.quorum", ZooKeeperUtil.toHBaseQuorumAddresses(parse));
		hbaseConfig.set("hbase.zookeeper.property.clientPort", String.valueOf(ZooKeeperUtil.toHBaseClientPort(parse)));
		try {
			bootstrap(hbaseConfig);
		} catch (Exception e) {
			throw new RuntimeException("Could not bootstrap table " + getClass().getName(), e);
		}
		HTableFactory tableFactory = new HTableFactory();
		return new HTablePool(hbaseConfig, DEFAULT_HBASE_THREADS, tableFactory);
	}

	protected boolean bootstrap(Configuration hbaseConfig) throws MasterNotRunningException, IOException {
		HBaseAdmin admin = new HBaseAdmin(hbaseConfig);
		HTableDescriptor tDesc = getTableDescriptor();
		try {
			byte[][] splits = getSplits();
			admin.createTable(tDesc, splits);
			return true;
		} catch (TableExistsException e) {
			log.info("Bootstrap was called.  No actual table was created.");
			return false;
		} finally {
			admin.close();
		}
	}

	protected abstract HTableDescriptor getTableDescriptor();

	protected abstract byte[][] getSplits();
}
