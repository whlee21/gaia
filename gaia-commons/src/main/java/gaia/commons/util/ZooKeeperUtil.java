package gaia.commons.util;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.curator.test.TestingServer;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ZooKeeperUtil {
	private static transient Logger LOG = LoggerFactory.getLogger(ZooKeeperUtil.class);

	public static TestingServer startMiniZK(int zkPort) throws Exception {
		LOG.info("Starting ZK on {}", Integer.valueOf(zkPort));
		TestingServer tmpZK = new TestingServer(zkPort);
		LOG.info("ZK started, connect at {}", tmpZK.getConnectString());

		CountDownLatch latch = new CountDownLatch(1);
		Watcher watcher = new ZKWatcher(latch);
		ZooKeeper zk = new ZooKeeper(tmpZK.getConnectString(), 100000, watcher);
		latch.await(10L, TimeUnit.SECONDS);
		ZooKeeper.States states = zk.getState();
		LOG.info("Alive: " + states.isAlive());
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
			System.out.println("Couldn't parse args: " + Arrays.asList(args));
			return;
		}
		String zkHost = cli.getOption("zkHost");
		String path = cli.getOption("path");
		if (cli.hasOption("clean")) {
			System.out.println("Not supported currently");
		} else {
			if (cli.hasOption("start")) {
				int port = Integer.parseInt(cli.getOption("port"));
				TestingServer testingServer = startMiniZK(port);
				System.out.println("Connect to ZK at " + testingServer.getConnectString());
				System.out.println("Ctrl-c to stop");
			}

			if (cli.hasOption("add")) {
				System.out.println("Not supported currently");
			}
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
