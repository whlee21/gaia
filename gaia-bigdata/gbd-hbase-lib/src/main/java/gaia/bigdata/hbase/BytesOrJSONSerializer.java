package gaia.bigdata.hbase;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import org.apache.hadoop.hbase.util.Bytes;
import org.codehaus.jackson.map.ObjectMapper;

public class BytesOrJSONSerializer implements ValueSerializer<Object> {
	public static final byte NULL = 0;
	public static final byte BYTE = 1;
	public static final byte SHORT = 2;
	public static final byte INT = 3;
	public static final byte LONG = 4;
	public static final byte FLOAT = 5;
	public static final byte DOUBLE = 6;
	public static final byte STRING = 7;
	public static final byte BYTE_ENCODED = 0;
	public static final byte JSON_ENCODED = 16;
	private static final ObjectMapper mapper = new ObjectMapper();

	public byte[] toBytes(Object o) throws IOException {
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		if ((o instanceof Byte)) {
			os.write(BYTE);
			os.write(((Byte) o).byteValue());
		} else if ((o instanceof Short)) {
			os.write(SHORT);
			os.write(Bytes.toBytes(((Short) o).shortValue()));
		} else if ((o instanceof Integer)) {
			os.write(INT);
			os.write(Bytes.toBytes(((Integer) o).intValue()));
		} else if ((o instanceof Long)) {
			os.write(LONG);
			os.write(Bytes.toBytes(((Long) o).longValue()));
		} else if ((o instanceof Float)) {
			os.write(FLOAT);
			os.write(Bytes.toBytes(((Float) o).floatValue()));
		} else if ((o instanceof Double)) {
			os.write(DOUBLE);
			os.write(Bytes.toBytes(((Double) o).doubleValue()));
		} else if ((o instanceof String)) {
			os.write(STRING);
			os.write(Bytes.toBytes((String) o));
		} else if (o == null) {
			os.write(NULL);
		} else {
			os.write(JSON_ENCODED);
			mapper.writeValue(os, o);
		}
		return os.toByteArray();
	}

	public Object toObject(byte[] bytes) throws IOException {
		byte type = bytes[0];
		switch (type & 0xF0) {
		case 0:
			switch (type & 0xF) {
			case NULL:
				return null;
			case BYTE:
				return Byte.valueOf(bytes[1]);
			case SHORT:
				return Short.valueOf(Bytes.toShort(bytes, 1, bytes.length - 1));
			case INT:
				return Integer.valueOf(Bytes.toInt(bytes, 1, bytes.length - 1));
			case LONG:
				return Long.valueOf(Bytes.toLong(bytes, 1, bytes.length - 1));
			case FLOAT:
				return Float.valueOf(Bytes.toFloat(bytes, 1));
			case DOUBLE:
				return Double.valueOf(Bytes.toDouble(bytes, 1));
			case STRING:
				return Bytes.toString(bytes, 1, bytes.length - 1);
			}
			throw new IOException("Encountered unexpected magic byte '" + type + "' (scalar)");
		case 16:
			return mapper.readValue(bytes, 1, bytes.length - 1, Object.class);
		}
		throw new IOException("Encountered unexpected magic byte '" + type + "'");
	}
}
