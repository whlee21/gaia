package gaia.utils;

import java.io.InputStream;
import java.io.OutputStream;

public final class SingleThreadByteArrayOutputStream extends OutputStream {
	private byte[] buffer = null;
	private int size = 0;

	public SingleThreadByteArrayOutputStream() {
		this(5120);
	}

	public SingleThreadByteArrayOutputStream(int initSize) {
		size = 0;
		buffer = new byte[initSize];
	}

	public byte[] getByteArray() {
		return buffer;
	}

	public InputStream getInputStream() {
		return new SingleThreadByteArrayInputStream(buffer, size);
	}

	public int getSize() {
		return size;
	}

	public void reset() {
		size = 0;
	}

	private void verifyBufferSize(int sz) {
		if (sz > buffer.length) {
			byte[] old = buffer;
			buffer = new byte[Math.max(sz, 2 * buffer.length)];
			System.arraycopy(old, 0, buffer, 0, old.length);
			old = null;
		}
	}

	public final void write(byte[] b) {
		verifyBufferSize(size + b.length);
		System.arraycopy(b, 0, buffer, size, b.length);
		size += b.length;
	}

	public final void write(byte[] b, int off, int len) {
		verifyBufferSize(size + len);
		System.arraycopy(b, off, buffer, size, len);
		size += len;
	}

	public final void write(int b) {
		verifyBufferSize(size + 1);
		buffer[(size++)] = ((byte) b);
	}
}
