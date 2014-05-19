package gaia.bigdata.hbase.metrics;

import gaia.bigdata.hbase.ValueSerializer;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.zip.CRC32;
import java.util.zip.Checksum;
import org.apache.hadoop.hbase.util.Bytes;

public class MetricKeySerializer implements ValueSerializer<MetricKey> {
	public byte[] toBytes(MetricKey key) throws IOException {
		Checksum hash = new CRC32();
		byte[] collection_bytes = Bytes.toBytes(key.collection);
		byte[] metric_bytes = Bytes.toBytes(key.metric);
		ByteBuffer bb = ByteBuffer.allocate(2 + collection_bytes.length + 1
				+ metric_bytes.length + 1 + 8);

		hash.update(collection_bytes, 0, collection_bytes.length);
		long hashValue = hash.getValue();
		bb.put((byte) (int) (hashValue >>> 0));
		bb.put((byte) (int) (hashValue >>> 8));

		bb.put(collection_bytes);
		bb.put((byte) 30);
		bb.put(metric_bytes);
		bb.put((byte) 30);

		bb.putLong(9223372036854775807L - key.timestamp);

		return bb.array();
	}

	public MetricKey toObject(byte[] bytes) throws IOException {
		ByteBuffer bb = ByteBuffer.wrap(bytes);
		byte[] ba = new byte[bytes.length - 3 - 8];
		bb.getShort();
		bb.get(ba);
		bb.get();
		long ts = 9223372036854775807L - bb.getLong();

		int i;
		for (i = 0; (i < ba.length) && (ba[i] != 30); i++)
			;
		if ((i == 0) || (i == ba.length - 1)) {
			throw new IOException(
					"Expected to find the delimiter (0x1E) somewhere in the middle");
		}
		return new MetricKey(Bytes.toString(ba, 0, i), Bytes.toString(ba,
				i + 1, ba.length - i - 1), ts);
	}
}
