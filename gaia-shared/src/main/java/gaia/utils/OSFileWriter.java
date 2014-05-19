package gaia.utils;

import gaia.Constants;
import java.io.File;
import java.io.IOException;

public class OSFileWriter {
	private File file;
	private File tmpFile;

	public OSFileWriter(File file) {
		this.file = file;
	}

	public File getWriteFile() throws IOException {
		if (!Constants.IS_UNIX) {
			return file;
		}

		File dir = file.getParentFile();
		if (!dir.exists()) {
			dir.mkdirs();
		}

		tmpFile = File.createTempFile(file.getName(), "", dir);
		return tmpFile;
	}

	public void flush() {
		if (!Constants.IS_UNIX) {
			return;
		}
		if (tmpFile == null) {
			throw new IllegalStateException("getWriteFile must have been called before flush");
		}
		boolean success = tmpFile.renameTo(file);
		if (!success) {
			success = tmpFile.delete();
			if (!success) {
				tmpFile.deleteOnExit();
			}
			throw new RuntimeException("Could not rename file " + tmpFile + " to " + file);
		}
	}
}
