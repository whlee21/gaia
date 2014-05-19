package gaia.bigdata.pig;

import gaia.bigdata.hbase.metrics.MetricKey;
import gaia.bigdata.hbase.metrics.MetricKeySerializer;
import java.io.IOException;
import org.apache.pig.EvalFunc;
import org.apache.pig.data.DataByteArray;
import org.apache.pig.data.Tuple;
import org.apache.pig.data.TupleFactory;

public class FromMetricRowKey extends EvalFunc<Tuple> {
	private static final MetricKeySerializer serializer = new MetricKeySerializer();

	public Tuple exec(Tuple tuple) throws IOException {
		if ((tuple == null) || (tuple.size() != 1))
			throw new IOException("Incorrect number of values (expected one).");
		try {
			DataByteArray id = (DataByteArray) tuple.get(0);
			MetricKey key = serializer.toObject(id.get());
			Tuple out = TupleFactory.getInstance().newTuple(3);
			out.set(0, key.collection);
			out.set(1, key.metric);
			out.set(2, Long.valueOf(key.timestamp));
			return out;
		} catch (Exception e) {
			throw new IOException(e);
		}
	}
}
