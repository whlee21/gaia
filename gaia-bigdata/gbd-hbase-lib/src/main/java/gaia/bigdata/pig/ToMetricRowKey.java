package gaia.bigdata.pig;

import gaia.bigdata.hbase.metrics.MetricKey;
import gaia.bigdata.hbase.metrics.MetricKeySerializer;
import java.io.IOException;
import org.apache.pig.EvalFunc;
import org.apache.pig.data.DataByteArray;
import org.apache.pig.data.Tuple;

public class ToMetricRowKey extends EvalFunc<DataByteArray> {
	private static final MetricKeySerializer serializer = new MetricKeySerializer();

	public DataByteArray exec(Tuple tuple) throws IOException {
		if ((tuple == null) || (tuple.size() != 3))
			throw new IOException("Incorrect number of values (expected three).");
		try {
			String collection = (String) tuple.get(0);
			String metric = (String) tuple.get(1);
			long timestamp = ((Long) tuple.get(2)).longValue();
			MetricKey key = new MetricKey(collection, metric, timestamp);
			return new DataByteArray(serializer.toBytes(key));
		} catch (Exception e) {
			throw new IOException(e);
		}
	}
}
