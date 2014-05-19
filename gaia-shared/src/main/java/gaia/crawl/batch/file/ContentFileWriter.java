package gaia.crawl.batch.file;

import gaia.crawl.batch.BatchContentWriter;
import gaia.crawl.io.Content;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

class ContentFileWriter extends BatchContentWriter {
	public static final byte VERSION = 1;
	RandomAccessFile out;
	boolean open = false;
	long count;

	public ContentFileWriter(File output) throws IOException {
		if (!output.getParentFile().exists()) {
			output.getParentFile().mkdirs();
		}
		out = new RandomAccessFile(output, "rw");
		if (out.length() == 0L) {
			out.writeByte(1);

			out.writeLong(0L);
		} else {
			byte ver = out.readByte();
			if (ver != 1) {
				throw new IOException("Incompatible versions, expected 1 got " + ver);
			}
			count = out.readLong();
		}
		out.seek(out.length());
		open = true;
	}

	public void write(Content content) throws IOException {
		content.write(out);
		count += 1L;
	}

	public boolean isOpen() throws IOException {
		return open;
	}

	public long getCount() {
		return count;
	}

	public void close() throws IOException {
		out.seek(1L);
		out.writeLong(count);
		out.close();
		open = false;
	}
}
