package gaia.bigdata.hbase.documents;

import gaia.bigdata.hbase.ValueSerializer;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.zip.CRC32;
import java.util.zip.Checksum;
import org.apache.hadoop.hbase.util.Bytes;

public class DocumentKeySerializer implements ValueSerializer<DocumentKey> {
	public byte[][] toByteRange(String collection) throws IOException {
		byte[][] byteRange = new byte[2][];
		byte[] baseKey = toBytes(new DocumentKey("", collection));
		baseKey = Bytes.head(baseKey, baseKey.length - 2);
		baseKey = Bytes.tail(baseKey, baseKey.length - 2);
		byteRange[0] = Bytes.add(baseKey, new byte[] { 30 });
		byteRange[1] = Bytes.add(baseKey, new byte[] { 31 });
		return byteRange;
	}

	public static byte[] getOriginalKey(byte[] adjustedKey) {
		return Bytes.tail(adjustedKey, adjustedKey.length - 2);
	}

	public byte[] toBytes(DocumentKey key) throws IOException {
		Checksum hash = new CRC32();
		byte[] documentBytes = Bytes.toBytes(key.id);
		hash.update(documentBytes, 0, documentBytes.length);
		long hashValue = hash.getValue();

		short salt = (short) (int) (hashValue % getNumberOfRegions());
		short absoluteSalt = (short) Math.abs(salt);
		byte[] saltBytes = Bytes.toBytes(absoluteSalt);

		byte[] collectionBytes = Bytes.toBytes(key.collection);
		ByteBuffer bb = ByteBuffer.allocate(saltBytes.length + collectionBytes.length + 1 + documentBytes.length + 1);

		bb.put(saltBytes);

		bb.put(collectionBytes);
		bb.put((byte) 30);
		bb.put(documentBytes);
		bb.put((byte) 30);

		return bb.array();
	}

	public DocumentKey toObject(byte[] bytes) throws IOException {
		ByteBuffer bb = ByteBuffer.wrap(bytes);
		byte[] ba = new byte[bytes.length - 3];
		bb.getShort();
		bb.get(ba);
		bb.get();

		int i;
		for (i = 0; (i < ba.length) && (ba[i] != 30); i++)
			;
		if ((i == 0) || (i == ba.length - 1)) {
			throw new IOException("Expected to find the delimiter (0x1E) somewhere in the middle");
		}

		return new DocumentKey(Bytes.toString(ba, i + 1, ba.length - i - 1), Bytes.toString(ba, 0, i));
	}

	public static byte[][] getSplits() {
		int numberOfBuckets = getNumberOfRegions();
		if (numberOfBuckets == 0) {
			return new byte[0][];
		}
		byte[][] splits = new byte[numberOfBuckets][];
		for (short i = 0; i < numberOfBuckets; i = (short) (i + 1)) {
			splits[i] = Bytes.toBytes(i);
		}
		return splits;
	}

	public static byte[][] getAllDistributedKeys(byte[] originalKey) throws IOException {
		byte[][] allPrefixes = getSplits();
		byte[][] keys = new byte[allPrefixes.length][];
		for (int i = 0; i < allPrefixes.length; i++) {
			keys[i] = Bytes.add(allPrefixes[i], originalKey);
		}
		return keys;
	}

	public static int getNumberOfRegions() {
		String numberOfRegionsString = System.getProperty("regions.number");
		int returnValue = 50;
		if ((numberOfRegionsString != null) && (!numberOfRegionsString.equals(""))) {
			returnValue = new Integer(numberOfRegionsString).intValue();
		}
		return returnValue;
	}
}
