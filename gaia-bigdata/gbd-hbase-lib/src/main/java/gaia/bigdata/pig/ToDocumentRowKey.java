package gaia.bigdata.pig;

import gaia.bigdata.hbase.documents.DocumentKey;
import gaia.bigdata.hbase.documents.DocumentKeySerializer;
import java.io.IOException;
import org.apache.pig.EvalFunc;
import org.apache.pig.data.DataByteArray;
import org.apache.pig.data.Tuple;

public class ToDocumentRowKey extends EvalFunc<DataByteArray> {
	private static final DocumentKeySerializer serializer = new DocumentKeySerializer();

	public DataByteArray exec(Tuple tuple) throws IOException {
		if ((tuple == null) || (tuple.size() != 2))
			throw new IOException("Incorrect number of values (expected 2).");
		try {
			String collection = (String) tuple.get(0);
			String id = (String) tuple.get(1);
			DocumentKey key = new DocumentKey(id, collection);
			return new DataByteArray(serializer.toBytes(key));
		} catch (Exception e) {
			throw new IOException(e);
		}
	}
}
