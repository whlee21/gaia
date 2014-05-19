package gaia.bigdata.pig;

import gaia.bigdata.hbase.documents.DocumentKey;
import gaia.bigdata.hbase.documents.DocumentKeySerializer;
import java.io.IOException;
import org.apache.pig.EvalFunc;
import org.apache.pig.data.DataByteArray;
import org.apache.pig.data.Tuple;
import org.apache.pig.data.TupleFactory;

public class FromDocumentRowKey extends EvalFunc<Tuple> {
	private static final DocumentKeySerializer serializer = new DocumentKeySerializer();

	public Tuple exec(Tuple tuple) throws IOException {
		if ((tuple == null) || (tuple.size() != 1))
			throw new IOException("Incorrect number of values (expected one).");
		try {
			DataByteArray id = (DataByteArray) tuple.get(0);
			DocumentKey key = serializer.toObject(id.get());
			Tuple out = TupleFactory.getInstance().newTuple(2);
			out.set(0, key.collection);
			out.set(1, key.id);
			return out;
		} catch (Exception e) {
			throw new IOException(e);
		}
	}
}
