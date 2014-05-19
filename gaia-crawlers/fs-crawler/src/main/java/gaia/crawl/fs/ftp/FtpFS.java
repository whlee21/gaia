package gaia.crawl.fs.ftp;

import gaia.crawl.datasource.DataSource;
import gaia.crawl.fs.FS;
import gaia.crawl.fs.FSObject;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.enterprisedt.net.ftp.FTPConnectMode;
import com.enterprisedt.net.ftp.FTPException;
import com.enterprisedt.net.ftp.FTPFile;
import com.enterprisedt.net.ftp.FileTransferClient;

public class FtpFS extends FS {
	private static final Logger LOG = LoggerFactory.getLogger(FtpFS.class);

	private FileTransferClient ftp = null;
	String curPath = null;

	public FtpFS(DataSource fsds) {
		super(fsds);
	}

	public static FileTransferClient createClient(URL u, String user, String pass) throws IOException {
		String host = u.getHost();
		FileTransferClient client = new FileTransferClient();
		try {
			client.getAdvancedFTPSettings().setConnectMode(FTPConnectMode.PASV);
			client.setRemoteHost(host);
			client.setUserName(user);
			client.setPassword(pass);
			if (u.getPort() != -1) {
				client.setRemotePort(u.getPort());
			}
			client.connect();
			try {
				client.changeDirectory(u.getPath());
			} catch (FTPException e) {
				throw new IOException("Root path '" + u.getPath() + "' doesn't exist or is not a directory.");
			}
		} catch (FTPException e) {
			throw new IOException("Can't connect: " + e.getMessage());
		}
		return client;
	}

	public FSObject get(String path) throws IOException {
		URL u = null;
		try {
			u = new URL(path);
		} catch (MalformedURLException mue) {
			throw new IOException(mue.toString());
		}
		String user = (String) fsds.getProperty("username");
		String pass = (String) fsds.getProperty("password");
		ftp = createClient(u, user, pass);
		FTPFile file = new FTPFile("");
		file.setPath(u.getPath());
		file.setLastModified(new Date());
		file.setDir(true);
		file.setPermissions("");
		return new FtpObject(this, u, file);
	}

	public void close() {
		try {
			if (ftp != null) {
				if (ftp.isConnected()) {
					ftp.disconnect(true);
				}
				ftp = null;
			}
		} catch (Exception e) {
			LOG.warn("Error closing FtpFS: " + e.toString(), e);
		}
	}

	private static final class FtpObject extends FSObject {
		FtpFS fs;
		FTPFile file;
		String path;
		URL u;

		FtpObject(FtpFS fs, URL u, FTPFile file) throws IOException {
			this.fs = fs;
			this.u = u;
			this.file = file;
			acls = new String[] { file.getPermissions() };
			owner = (file.getOwner() != null ? file.getOwner() : "");
			name = file.getName();
			directory = file.isDir();
			group = (file.getGroup() != null ? file.getGroup() : "");
			lastModified = file.lastModified().getTime();
			size = file.size();
			uri = (u.getProtocol() + "://" + u.getHost());
			if (u.getPort() != -1) {
				uri = (uri + ":" + u.getPort());
			}
			path = file.getPath();
			if (path == null) {
				path = u.getPath();
			}
			uri += path;
			if (!uri.endsWith("/")) {
				uri += "/";
			}
			String name = file.getName();
			if (name != null) {
				uri += name;
				if (directory) {
					if (!path.endsWith("/")) {
						path += "/";
					}
					path += name;
				}
			}
		}

		public Iterable<FSObject> getChildren() throws IOException {
			if (!directory)
				return null;
			try {
				fs.ftp.changeDirectory(path);
				FTPFile[] files = fs.ftp.directoryList();
				List<FSObject> res = new ArrayList<FSObject>();
				for (FTPFile f : files) {
					try {
						FtpObject o = new FtpObject(fs, u, f);
						res.add(o);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
				return res;
			} catch (Exception e) {
				throw new IOException("Error listing dir", e);
			}
		}

		public InputStream open() throws IOException {
			if (directory)
				return null;
			try {
				fs.ftp.changeDirectory(path);
				return fs.ftp.downloadStream(name);
			} catch (Exception e) {
				throw new IOException("Error opening stream", e);
			}
		}
	}
}
