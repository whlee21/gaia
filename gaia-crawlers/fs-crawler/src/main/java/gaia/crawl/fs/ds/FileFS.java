package gaia.crawl.fs.ds;

import gaia.crawl.CrawlerUtils;
import gaia.crawl.datasource.DataSource;
import gaia.crawl.fs.FS;
import gaia.crawl.fs.FSObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

class FileFS extends FS {
	FileFS(DataSource fsds) {
		super(fsds);
	}

	public FSObject get(String path) throws IOException {
		File f = CrawlerUtils.resolveRelativePath(path);
		if (f.exists()) {
			return new FileFSObject(f);
		}
		return null;
	}

	public void close() {
	}

	static class FileFSObject extends FSObject {
		static String user = System.getProperty("java.user");
		File f;

		FileFSObject(File f) {
			this.f = f;
			directory = f.isDirectory();
			lastModified = f.lastModified();
			name = f.getName();
			size = f.length();

			owner = user;
			acls = EMPTY_ACLS;

			uri = f.toURI().toString();
		}

		public Iterable<FSObject> getChildren() throws IOException {
			if (f.isDirectory()) {
				File[] files = f.listFiles();
				if ((files == null) || (files.length == 0)) {
					return null;
				}
				List<FSObject> res = new ArrayList<FSObject>(files.length);
				for (int i = 0; i < files.length; i++) {
					res.add(new FileFSObject(files[i]));
				}
				return res;
			}
			return null;
		}

		public InputStream open() throws IOException {
			return new FileInputStream(f);
		}
	}
}
