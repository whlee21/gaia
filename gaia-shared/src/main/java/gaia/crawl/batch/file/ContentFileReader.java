package gaia.crawl.batch.file;

import gaia.crawl.batch.BatchContentReader;
import gaia.crawl.io.Content;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

class ContentFileReader extends BatchContentReader {
	RandomAccessFile in;
	long count;
	long currentRecord = 0L;

	public ContentFileReader(File input) throws IOException {
		in = new RandomAccessFile(input, "r");
		if (in.length() > 9L) {
			byte ver = in.readByte();
			if (ver != 1) {
				throw new IOException("Incompatible versions, expected 1 got " + ver);
			}

			count = in.readLong();
		}
	}

	public boolean read(Content content) throws IOException {
		if (in.getFilePointer() >= in.length()) {
			return false;
		}
		assert (currentRecord < count);
		content.readFields(in);
		currentRecord += 1L;
		return true;
	}

	public long getCount() {
		return count;
	}

	public long getCurrentRecord() {
		return currentRecord;
	}

	public void close() throws IOException {
		in.close();
	}
}
