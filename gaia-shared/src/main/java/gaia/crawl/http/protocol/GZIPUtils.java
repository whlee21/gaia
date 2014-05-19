package gaia.crawl.http.protocol;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GZIPUtils {
	private static final Logger LOG = LoggerFactory.getLogger(GZIPUtils.class);
	private static final int EXPECTED_COMPRESSION_RATIO = 5;
	private static final int BUF_SIZE = 4096;

	public static final byte[] unzipBestEffort(byte[] in) {
		return unzipBestEffort(in, Integer.MAX_VALUE);
	}

	public static final byte[] unzipBestEffort(byte[] in, int sizeLimit) {
		if (sizeLimit < 0)
			sizeLimit = Integer.MAX_VALUE;

		ByteArrayOutputStream outStream = new ByteArrayOutputStream(EXPECTED_COMPRESSION_RATIO * in.length);
		try {

			GZIPInputStream inStream = new GZIPInputStream(new ByteArrayInputStream(in));

			byte[] buf = new byte[BUF_SIZE];
			int written = 0;
			try {
				while (true) {
					int size = inStream.read(buf);
					if (size <= 0)
						break;
					if (written + size > sizeLimit) {
						outStream.write(buf, 0, sizeLimit - written);
						break;
					}
					outStream.write(buf, 0, size);
					written += size;
				}
			} catch (Exception e) {
				try {
					outStream.close();
				} catch (IOException ee) {
				}
				return outStream.toByteArray();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		return outStream.toByteArray();
	}

	public static final byte[] unzip(byte[] in) throws IOException {
		ByteArrayOutputStream outStream = new ByteArrayOutputStream(EXPECTED_COMPRESSION_RATIO * in.length);

		GZIPInputStream inStream = new GZIPInputStream(new ByteArrayInputStream(in));

		byte[] buf = new byte[BUF_SIZE];
		while (true) {
			int size = inStream.read(buf);
			if (size <= 0)
				break;
			outStream.write(buf, 0, size);
		}
		outStream.close();

		return outStream.toByteArray();
	}

	public static final byte[] zip(byte[] in) {
		try {
			ByteArrayOutputStream byteOut = new ByteArrayOutputStream(in.length / EXPECTED_COMPRESSION_RATIO);

			GZIPOutputStream outStream = new GZIPOutputStream(byteOut);
			try {
				outStream.write(in);
			} catch (Exception e) {
				LOG.warn("Exception writing compressed data", e);
			}
			try {
				outStream.close();
			} catch (IOException e) {
				LOG.warn("Exception writing compressed data", e);
			}

			return byteOut.toByteArray();
		} catch (IOException e) {
			LOG.warn("Exception writing compressed data", e);
		}
		return null;
	}
}
