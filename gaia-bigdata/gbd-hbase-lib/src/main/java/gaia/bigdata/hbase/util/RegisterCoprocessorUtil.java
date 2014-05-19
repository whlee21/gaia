package gaia.bigdata.hbase.util;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.FsUrlStreamHandlerFactory;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.RawLocalFileSystem;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.HTableDescriptor;
import org.apache.hadoop.hbase.MasterNotRunningException;
import org.apache.hadoop.hbase.TableNotFoundException;
import org.apache.hadoop.hbase.ZooKeeperConnectionException;
import org.apache.hadoop.hbase.client.HBaseAdmin;
import org.apache.hadoop.hbase.util.Bytes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RegisterCoprocessorUtil {
	private static final Logger log = LoggerFactory
			.getLogger(RegisterCoprocessorUtil.class);

	public static void main(String[] args) throws IOException {
		if (args.length != 5) {
			log.error("Usage: java gaia.bigdata.hbase.util.RegisterCoprocessor zkConnect table jarSrcPath jarDestPath className");
			System.exit(1);
		}
		String zkConnect = args[0];
		String table = args[1];
		String src = args[2];
		String dest = args[3];
		String className = args[4];
		run(zkConnect, table, src, dest, className);
	}

	public static void run(String zkConnect, String table, String src,
			String dest, String className) throws IOException {
		byte[] tableName = Bytes.toBytes(table);

		Configuration conf = HBaseConfiguration.create();
		conf.set("hbase.zookeeper.quorum", zkConnect);

		Path srcPath = new Path(src);
		FileSystem srcFs = new RawLocalFileSystem();
		srcFs.setConf(conf);

		final Path fqSrcPath;
		if (srcFs.exists(srcPath)) {
			fqSrcPath = srcPath.makeQualified(srcFs.getUri(), srcFs.getWorkingDirectory());
			log.info("Using jar: {}", fqSrcPath);
		} else {
			srcFs.close();
			throw new IOException("src file does not exist: " + src);
		}
		try {

			ClassLoader loader = new URLClassLoader(new URL[] { fqSrcPath
					.toUri().toURL() }, conf.getClass().getClassLoader(),
					new FsUrlStreamHandlerFactory(conf));

			Class<?> clazz = loader.loadClass(className);
			clazz.newInstance();
			log.info("Loaded class {}", className);
		} catch (Exception e) {
			throw new RuntimeException("Could not load coprocessor class", e);
		}
		Path destPath = new Path(dest);
		FileSystem destFs = destPath.getFileSystem(conf);
		destFs.copyFromLocalFile(false, false, srcPath, destPath);
		Path fqDestPath = destPath.makeQualified(destFs.getUri(), destFs.getWorkingDirectory());
		log.info("Copied jar to HDFS: {}", fqDestPath);
		HBaseAdmin admin = new HBaseAdmin(conf);
		HTableDescriptor tDesc;
		try {
//			admin = new HBaseAdmin(conf);
			tDesc = admin.getTableDescriptor(tableName);
			admin.close();
		} catch (MasterNotRunningException e) {
			log.error("HBase is not running", e);
			throw new RuntimeException(e);
		} catch (ZooKeeperConnectionException e) {
			log.error("Could not connect to ZooKeeper", e);
			throw new RuntimeException(e);
		} catch (TableNotFoundException e) {
			log.error("Table does not exist", e);
			throw new RuntimeException(e);
		} catch (Exception e) {
			log.error("Could not load table descriptor", e);
			throw new RuntimeException(e);
		} finally {
			admin.close();
		}
		log.info("Connected to HBase, updating table {}", table);
		try {
			admin.disableTable(tableName);
			log.info("Table {} disabled", table);
			if (tDesc.hasCoprocessor(className)) {
				log.info("Removing exising coprocessor {}", className);
				tDesc.removeCoprocessor(className);
			}
			tDesc.addCoprocessor(className, fqDestPath, 1001, null);
			log.info("Updated descriptor");
			admin.modifyTable(tableName, tDesc);
			log.info("Updated table {}", table);
		} catch (IOException e) {
			log.error("Could not update table with new coprocessor", e);
			throw new RuntimeException(e);
		} finally {
			admin.enableTable(tableName);
			log.info("Re-enabled table {}, all done.", table);
		}
	}
}
