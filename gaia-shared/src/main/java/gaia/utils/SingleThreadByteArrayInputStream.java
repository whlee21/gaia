package gaia.utils;

import java.io.InputStream;

public final class SingleThreadByteArrayInputStream extends InputStream {
	protected byte[] buffer = null;

	private int count = 0;

	private int pos = 0;

	public SingleThreadByteArrayInputStream(byte[] buf, int count) {
		buffer = buf;
		this.count = count;
	}

	public final int available() {
		return count - pos;
	}

	public final int read() {
		return pos < count ? buffer[(pos++)] & 0xFF : -1;
	}

	public final int read(byte[] b, int off, int len) {
		if (pos >= count)
			return -1;

		if (pos + len > count)
			len = count - pos;

		System.arraycopy(buffer, pos, b, off, len);
		pos += len;
		return len;
	}

	public final long skip(long n) {
		if (pos + n > count)
			n = count - pos;
		if (n < 0L)
			return 0L;
		pos = ((int) (pos + n));
		return n;
	}
}
