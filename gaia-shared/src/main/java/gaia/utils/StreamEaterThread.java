package gaia.utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.apache.commons.io.output.NullOutputStream;

public final class StreamEaterThread extends Thread {
	private final InputStream is;
	private final OutputStream os;

	public StreamEaterThread(InputStream is, OutputStream redirect) {
		this.is = is;
		os = (null != redirect ? redirect : NullOutputStream.NULL_OUTPUT_STREAM);
		setDaemon(true);
	}

	public void run() {
		try {
			int data = -1;
			while (-1 != (data = is.read())) {
				os.write(data);
			}
			os.flush();
		} catch (IOException ioe) {
			throw new RuntimeException("Error eating streams", ioe);
		}
	}
}
