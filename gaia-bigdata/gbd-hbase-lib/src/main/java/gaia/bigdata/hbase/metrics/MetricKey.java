package gaia.bigdata.hbase.metrics;

import gaia.bigdata.hbase.Key;

public class MetricKey implements Key {
	public final String collection;
	public final String metric;
	public final long timestamp;

	public MetricKey(String collection, String metric, long timestamp) {
		this.collection = collection;
		this.metric = metric;
		this.timestamp = timestamp;
	}

	public String toString() {
		return "Metric Key [collection=" + collection + ", metric=" + metric + ", ts=" + timestamp + "]";
	}
}
