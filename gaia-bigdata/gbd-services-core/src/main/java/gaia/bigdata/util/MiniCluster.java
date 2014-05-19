package gaia.bigdata.util;

import org.apache.curator.test.TestingServer;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hdfs.MiniDFSCluster;
import org.apache.hadoop.mapred.MiniMRCluster;

public class MiniCluster {
	public MiniDFSCluster dfs;
	public MiniMRCluster mr;
	public Configuration config;
	public TestingServer zookeeper;
//	public HBaseTestingUtility hbaseTestingUtil;
//	public MiniHBaseCluster miniHBase;
//	public CuratorFramework curator;
//	public ServiceDiscovery<Map> discovery;
//
	public MiniCluster(MiniDFSCluster dfs, MiniMRCluster mr,
			Configuration config) {
		this.dfs = dfs;
		this.mr = mr;
		this.config = config;
	}
//
//	public void shutdown() throws Exception {
//		if (this.mr != null) {
//			this.mr.shutdown();
//		}
//		if (this.dfs != null) {
//			this.dfs.shutdown();
//		}
//		if (this.hbaseTestingUtil != null) {
//			this.hbaseTestingUtil.shutdownMiniCluster();
//		}
//		if (this.miniHBase != null) {
//			this.miniHBase.shutdown();
//		}
//		if (this.zookeeper != null) {
//			this.zookeeper.stop();
//		}
//		if (this.curator != null)
//			this.curator.close();
//	}
}
