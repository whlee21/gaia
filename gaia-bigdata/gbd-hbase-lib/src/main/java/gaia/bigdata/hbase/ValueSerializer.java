package gaia.bigdata.hbase;

import java.io.IOException;

public interface ValueSerializer<T> {
	public byte[] toBytes(T paramT) throws IOException;

	public T toObject(byte[] paramArrayOfByte) throws IOException;
}
