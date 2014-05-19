package gaia.bigdata.hbase;

import java.io.IOException;

import gaia.bigdata.hbase.documents.DocumentTable;
import gaia.bigdata.hbase.metrics.MetricTable;
import gaia.bigdata.hbase.users.UserTable;

public class HBaseBootstrap {
	public static void main(String[] args) throws IOException {
		if (args.length != 1) {
			System.out.println("Must provide zk connection string host:port, e.g. localhost:2181");
		}
		String zkConnectingString = args[0];

		new MetricTable(zkConnectingString).close();
		new DocumentTable(zkConnectingString).close();
		new UserTable(zkConnectingString).close();
	}
}
