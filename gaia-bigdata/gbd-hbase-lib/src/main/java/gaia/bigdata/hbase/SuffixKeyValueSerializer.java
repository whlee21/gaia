package gaia.bigdata.hbase;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import org.apache.hadoop.hbase.util.Bytes;
import org.codehaus.jackson.map.ObjectMapper;

public class SuffixKeyValueSerializer {
	private final ObjectMapper mapper = new ObjectMapper();

	public KeyValue<byte[], byte[]> toBytes(String key, Object value) throws IOException {
		if ((value instanceof Byte))
			return new KeyValue<byte[], byte[]>(Bytes.toBytes(key + "_b"), new byte[] { ((Byte) value).byteValue() });
		if ((value instanceof Short))
			return new KeyValue<byte[], byte[]>(Bytes.toBytes(key + "_h"), Bytes.toBytes(((Short) value).shortValue()));
		if ((value instanceof Integer))
			return new KeyValue<byte[], byte[]>(Bytes.toBytes(key + "_i"), Bytes.toBytes(((Integer) value).intValue()));
		if ((value instanceof Long))
			return new KeyValue<byte[], byte[]>(Bytes.toBytes(key + "_q"), Bytes.toBytes(((Long) value).longValue()));
		if ((value instanceof Float))
			return new KeyValue<byte[], byte[]>(Bytes.toBytes(key + "_f"), Bytes.toBytes(((Float) value).floatValue()));
		if ((value instanceof Double))
			return new KeyValue<byte[], byte[]>(Bytes.toBytes(key + "_d"), Bytes.toBytes(((Double) value).doubleValue()));
		if ((value instanceof String))
			return new KeyValue<byte[], byte[]>(Bytes.toBytes(key + "_s"), Bytes.toBytes((String) value));
		if (value == null) {
			return new KeyValue<byte[], byte[]>(Bytes.toBytes(key + "_x"), new byte[] { 0 });
		}
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		mapper.writeValue(baos, value);
		return new KeyValue<byte[], byte[]>(Bytes.toBytes(key + "_j"), baos.toByteArray());
	}

	public KeyValue<String, Object> toObject(byte[] keyBytes, byte[] valueBytes) throws IOException {
		String keyWithSuffix = Bytes.toString(keyBytes);
		String key = keyWithSuffix.substring(0, keyWithSuffix.length() - 2);
		String suffix = keyWithSuffix.substring(keyWithSuffix.length() - 2);
		if (suffix.equals("_b"))
			return new KeyValue<String, Object>(key, Byte.valueOf(valueBytes[0]));
		if (suffix.equals("_h"))
			return new KeyValue<String, Object>(key, Short.valueOf(Bytes.toShort(valueBytes)));
		if (suffix.equals("_i"))
			return new KeyValue<String, Object>(key, Integer.valueOf(Bytes.toInt(valueBytes)));
		if (suffix.equals("_q"))
			return new KeyValue<String, Object>(key, Long.valueOf(Bytes.toLong(valueBytes)));
		if (suffix.equals("_f"))
			return new KeyValue<String, Object>(key, Float.valueOf(Bytes.toFloat(valueBytes)));
		if (suffix.equals("_d"))
			return new KeyValue<String, Object>(key, Double.valueOf(Bytes.toDouble(valueBytes)));
		if (suffix.equals("_s"))
			return new KeyValue<String, Object>(key, Bytes.toString(valueBytes));
		if (suffix.equals("_x"))
			return new KeyValue<String, Object>(key, null);
		if (suffix.equals("_j")) {
			return new KeyValue<String, Object>(key, mapper.readValue(valueBytes, 0, valueBytes.length, Object.class));
		}
		throw new IOException("Unknown column suffix " + suffix);
	}

	public static final class KeyValue<K, V> {
		public final K key;
		public final V value;

		public KeyValue(K key, V value) {
			this.key = key;
			this.value = value;
		}
	}
}
