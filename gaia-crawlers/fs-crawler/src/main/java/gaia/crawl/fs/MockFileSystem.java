package gaia.crawl.fs;

import java.io.IOException;
import java.net.URI;
import java.util.Date;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FSInputStream;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.permission.FsPermission;
import org.apache.hadoop.util.Progressable;

public class MockFileSystem extends FileSystem {
	static final URI NAME = URI.create("mockfs:///");

	public FSDataOutputStream append(Path arg0, int arg1, Progressable arg2) throws IOException {
		throw new UnsupportedOperationException("append " + arg0);
	}

	public FSDataOutputStream create(Path path, FsPermission perm, boolean overwrite, int buffersize, short replication,
			long blocksize, Progressable progress) throws IOException {
		throw new UnsupportedOperationException("create " + path);
	}

	public boolean delete(Path path) throws IOException {
		throw new UnsupportedOperationException("delete " + path);
	}

	public boolean delete(Path path, boolean recursive) throws IOException {
		throw new UnsupportedOperationException("delete " + path + " " + recursive);
	}

	public FileStatus getFileStatus(Path path) throws IOException {
		String pathString = path.toUri().getPath().toString();
		Date d = new Date();
		if (pathString.equals("/mock/fs"))
			return new FileStatus(1000L, false, 3, 1048576L, d.getTime(), d.getTime(), null, "mock", "fs", path);
		if (pathString.startsWith("/crawl")) {
			if (pathString.equals("/crawl")) {
				return new FileStatus(1000L, true, 3, 1048576L, d.getTime(), d.getTime(), null, "mock", "fs", path);
			}
			return new FileStatus(1000L, false, 3, 1048576L, d.getTime(), d.getTime(), null, "mock", "fs", path);
		}

		return null;
	}

	public URI getUri() {
		return NAME;
	}

	public Path getWorkingDirectory() {
		throw new UnsupportedOperationException("getWorkingDirectory");
	}

	public FileStatus[] listStatus(Path path) throws IOException {
		String pathString = path.toUri().getPath().toString();
		Date d = new Date();
		if (pathString.equals("/crawl")) {
			FileStatus[] stats = new FileStatus[100];
			for (int i = 0; i < 100; i++) {
				stats[i] = new FileStatus(1000L, false, 3, 1048576L, d.getTime(), d.getTime(), null, "mock", "fs", new Path(
						path, i + ""));
			}
			return stats;
		}
		return null;
	}

	public boolean mkdirs(Path arg0, FsPermission arg1) throws IOException {
		throw new UnsupportedOperationException("mkdirs " + arg0 + " " + arg1);
	}

	public FSDataInputStream open(Path path, int arg1) throws IOException {
		String pathString = path.toUri().getPath().toString();
		if (!pathString.startsWith("/crawl/")) {
			throw new IOException("only /crawl/0 to /crawl/99 paths are supported here");
		}
		if (pathString.endsWith("/99")) {
			try {
				Thread.sleep(100000000L);
			} catch (InterruptedException e) {
				throw new IOException("MockFileSystem.open(" + path + ") was interrupted");
			}
		}
		byte[] bytes = new String("fake content of " + pathString).getBytes("UTF-8");
		MockInputStream mis = new MockInputStream(bytes);
		FSDataInputStream is = new FSDataInputStream(mis);
		return is;
	}

	public boolean rename(Path arg0, Path arg1) throws IOException {
		throw new UnsupportedOperationException("rename " + arg0 + " " + arg1);
	}

	public void setWorkingDirectory(Path arg0) {
		throw new UnsupportedOperationException("setWorkingDirectory " + arg0);
	}

	private static final class MockInputStream extends FSInputStream {
		byte[] bytes;
		long pos;

		MockInputStream(byte[] bytes) {
			this.bytes = bytes;
			pos = 0L;
		}

		public long getPos() throws IOException {
			return pos;
		}

		public void seek(long l) throws IOException {
			if (l >= bytes.length) {
				throw new IOException("position out of range " + l + " > " + bytes.length);
			}
			pos = l;
		}

		public boolean seekToNewSource(long l) throws IOException {
			seek(l);
			return true;
		}

		public int read() throws IOException {
			if (pos < bytes.length) {
				return bytes[((int) pos++)];
			}
			return -1;
		}
	}
}
