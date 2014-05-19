package gaia.bigdata.models;

import gaia.bigdata.hbase.ValueSerializer;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.zip.CRC32;
import java.util.zip.Checksum;
import org.apache.hadoop.hbase.util.Bytes;

public class ClassifierModelKeySerializer implements
		ValueSerializer<ClassifierModelKey> {
	public byte[] toBytes(ClassifierModelKey value) throws IOException {
		Checksum hash = new CRC32();
		byte[] user_bytes = Bytes.toBytes(value.modelName);
		hash.update(user_bytes, 0, user_bytes.length);
		ByteBuffer bb = ByteBuffer.allocate(user_bytes.length + 3);

		long hashValue = hash.getValue();
		bb.put((byte) (int) (hashValue >>> 0));
		bb.put((byte) (int) (hashValue >>> 8));

		bb.put(user_bytes);
		bb.put((byte) 30);

		return bb.array();
	}

	public ClassifierModelKey toObject(byte[] bytes) throws IOException {
		ByteBuffer bb = ByteBuffer.wrap(bytes);
		bb.getShort();
		byte[] user_bytes = new byte[bytes.length - 3];
		bb.get(user_bytes, 0, user_bytes.length);
		return new ClassifierModelKey(Bytes.toString(user_bytes));
	}
}
