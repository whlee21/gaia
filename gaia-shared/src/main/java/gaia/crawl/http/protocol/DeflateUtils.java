package gaia.crawl.http.protocol;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DeflateUtils {
	private static final Logger LOG = LoggerFactory.getLogger(DeflateUtils.class);
	private static final int EXPECTED_COMPRESSION_RATIO = 5;
	private static final int BUF_SIZE = 4096;

	public static final byte[] inflateBestEffort(byte[] in) {
		return inflateBestEffort(in, Integer.MAX_VALUE);
	}

	public static final byte[] inflateBestEffort(byte[] in, int sizeLimit) {
		if (sizeLimit < 0) {
			sizeLimit = Integer.MAX_VALUE;
		}
		ByteArrayOutputStream outStream = new ByteArrayOutputStream(EXPECTED_COMPRESSION_RATIO * in.length);

		Inflater inflater = new Inflater(true);
		InflaterInputStream inStream = new InflaterInputStream(new ByteArrayInputStream(in), inflater);

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
			LOG.info("Caught Exception in inflateBestEffort");
			try {
				outStream.close();
			} catch (IOException ee) {
			}
		}
		return outStream.toByteArray();
	}

	public static final byte[] inflate(byte[] in) throws IOException {
		ByteArrayOutputStream outStream = new ByteArrayOutputStream(EXPECTED_COMPRESSION_RATIO * in.length);

		InflaterInputStream inStream = new InflaterInputStream(new ByteArrayInputStream(in));

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

	public static final byte[] deflate(byte[] in) {
		ByteArrayOutputStream byteOut = new ByteArrayOutputStream(in.length / EXPECTED_COMPRESSION_RATIO);

		DeflaterOutputStream outStream = new DeflaterOutputStream(byteOut);
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
	}
}
