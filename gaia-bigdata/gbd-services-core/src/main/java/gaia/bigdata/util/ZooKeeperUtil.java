package gaia.bigdata.util;

import gaia.commons.util.CLI;

import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.curator.test.TestingServer;
import org.apache.solr.common.cloud.SolrZkClient;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.client.ConnectStringParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ZooKeeperUtil {
	private static transient Logger log = LoggerFactory.getLogger(ZooKeeperUtil.class);

	public static String toHBaseQuorumAddresses(ConnectStringParser parser) {
		StringBuilder bldr = new StringBuilder();
		for (InetSocketAddress address : parser.getServerAddresses()) {
			bldr.append(address.getHostName()).append(",");
		}
		if (bldr.length() > 0) {
			bldr.setLength(bldr.length() - 1);
		}
		return bldr.toString();
	}

	public static int toHBaseClientPort(ConnectStringParser parser) {
		int port = 2181;
		Set<Integer> seen = new HashSet<Integer>();
		for (InetSocketAddress address : parser.getServerAddresses()) {
			seen.add(Integer.valueOf(address.getPort()));
		}
		if (seen.size() > 1)
			throw new IllegalArgumentException(
					"HBase doesn't support multiple ports, please check your Zookeeper connect strings");
		if (seen.size() == 1) {
			port = ((Integer) seen.iterator().next()).intValue();
		}
		return port;
	}

	public static void tryCleanPath(String zkHost, String path, int timeout) throws Exception {
		SolrZkClient zkClient = new SolrZkClient(zkHost, timeout);
		if (zkClient.exists(path, true).booleanValue()) {
			List<String> children = zkClient.getChildren(path, null, true);
			for (String string : children) {
				tryCleanPath(zkHost, new StringBuilder().append(path).append("/").append(string).toString(), timeout);
			}
			zkClient.delete(path, -1, true);
		}
		zkClient.close();
	}

	public static void addPath(String zkHost, String path, byte[] data, CreateMode createMode, int timeout)
			throws Exception {
		SolrZkClient zkClient = new SolrZkClient(zkHost, timeout);
		if (zkClient.exists(path, true).booleanValue())
			zkClient.makePath(path, data, createMode, true);
	}

	public static TestingServer startMiniZK(int zkPort) throws Exception {
		log.info("Starting ZK on {}", Integer.valueOf(zkPort));
		TestingServer tmpZK = new TestingServer(zkPort);
		log.info("ZK started, connect at {}", tmpZK.getConnectString());

		CountDownLatch latch = new CountDownLatch(1);
		Watcher watcher = new ZKWatcher(latch);
		ZooKeeper zk = new ZooKeeper(tmpZK.getConnectString(), 100000, watcher);
		latch.await(10L, TimeUnit.SECONDS);
		ZooKeeper.States states = zk.getState();
		log.info(new StringBuilder().append("Alive: ").append(states.isAlive()).toString());
		return tmpZK;
	}

	public static void main(String[] args) throws Exception {
		CLI cli = new CLI();
		cli.addFlag("start", "s", "Start a MiniZK on the port");
		cli.addOption("port", "po", "The port to start the MiniZK on");
		cli.addOption("zkHost", "z", "The ZK Host for cleaning");
		cli.addOption("path", "p", "The path");
		cli.addFlag("clean", "c", "Clean the path at zkHost");
		cli.addFlag("add", "a", "Add a path at zkHost");

		Map<String, List<String>> argMap = cli.parseArguments(args);
		if (argMap == null) {
			System.out.println(new StringBuilder().append("Couldn't parse args: ").append(Arrays.asList(args)).toString());
			return;
		}
		String zkHost = cli.getOption("zkHost");
		String path = cli.getOption("path");
		if (cli.hasOption("clean")) {
			tryCleanPath(zkHost, path, 2000);
		} else {
			if (cli.hasOption("start")) {
				int port = Integer.parseInt(cli.getOption("port"));
				TestingServer testingServer = startMiniZK(port);
				System.out.println(new StringBuilder().append("Connect to ZK at ").append(testingServer.getConnectString())
						.toString());
				System.out.println("Ctrl-c to stop");
			}
			if (cli.hasOption("add"))
				addPath(zkHost, path, "".getBytes(Charset.forName("UTF-8")), CreateMode.PERSISTENT, 2000);
		}
	}

	private static class ZKWatcher implements Watcher {
		private volatile CountDownLatch latch;

		private ZKWatcher(CountDownLatch latch) {
			this.latch = latch;
		}

		public void process(WatchedEvent event) {
			if (event.getState().equals(Watcher.Event.KeeperState.SyncConnected))
				latch.countDown();
		}
	}
}
