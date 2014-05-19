package gaia.bigdata.hbase.metrics;


public class MetricTableAdmin {
	public static final String USAGE = "Usage:\n\tMetricTableAdmin [zookeeper host:port] [collection] [metric] [epoch timestamp] [int|float|string] [value]";

	public static void main(String[] args) throws Exception {
		if (args.length != 6) {
			System.err
					.println("Usage:\n\tMetricTableAdmin [zookeeper host:port] [collection] [metric] [epoch timestamp] [int|float|string] [value]");
			System.exit(1);
		}
		try {
			String zkConnect = args[0];
			String collection = args[1];
			String metric = args[2];
			long timestamp = Long.parseLong(args[3]);
			String type = args[4];
			String valueString = args[5];
			Object value = null;
			if (type.equalsIgnoreCase("int"))
				value = Integer.valueOf(Integer.parseInt(valueString));
			else if (type.equalsIgnoreCase("float"))
				value = Float.valueOf(Float.parseFloat(valueString));
			else {
				value = valueString;
			}
			System.out.println("Adding value '" + value + "' to metric '"
					+ metric + "' for collection '" + collection
					+ "' with timestamp '" + timestamp + "'");
			MetricTable table = new MetricTable(zkConnect);
			table.putMetric(collection, timestamp, metric, value);
			System.out.println("OK");
			table.close();
		} catch (Exception e) {
			System.err
					.println("Usage:\n\tMetricTableAdmin [zookeeper host:port] [collection] [metric] [epoch timestamp] [int|float|string] [value]");
			throw e;
		}
	}
}
