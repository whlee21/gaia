package gaia.crawl.fs.ds;

import gaia.crawl.CrawlState;
import gaia.crawl.datasource.DataSource;
import gaia.crawl.datasource.DataSourceUtils;
import gaia.crawl.fs.FS;
import gaia.crawl.fs.FSObject;
import gaia.crawl.fs.hdfs.Converter;
import gaia.crawl.fs.hdfs.DefaultConverter;
import gaia.crawl.fs.hdfs.SFOCrawler;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.util.ReflectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HDFSFS extends FS {
	static final Logger LOG = LoggerFactory.getLogger(HDFSFS.class);

	public static Map<String, String> defaultConf = new HashMap<String, String>();
	private Configuration conf;
	private FileSystem fs;
	private Converter conv;

	public static Configuration prepareConfiguration(Map<String, String> overrides, String rootUri, String username,
			String password) {
		ClassLoader ctxCl = Thread.currentThread().getContextClassLoader();
		try {
			Thread.currentThread().setContextClassLoader(HDFSFS.class.getClassLoader());

			Configuration res = new Configuration();

			if (overrides != null) {
				for (Map.Entry<String, String> e : overrides.entrySet()) {
					res.set((String) e.getKey(), (String) e.getValue());
				}
			}
			res.setClassLoader(HDFSFS.class.getClassLoader());
			Path p = new Path(rootUri);
			URI u = p.toUri();
			String scheme = u.getScheme();
			if (scheme.startsWith("s3")) {
				res.set("fs." + scheme + ".awsAccessKeyId", username);
				res.set("fs." + scheme + ".awsSecretAccessKey", password);
			} else if (scheme.startsWith("ftp")) {
				res.set("fs." + scheme + ".user." + u.getHost(), username);
				res.set("fs." + scheme + ".password." + u.getHost(), password);
			}
			return res;
		} finally {
			Thread.currentThread().setContextClassLoader(ctxCl);
		}
	}

	public HDFSFS(DataSource fsds) throws IOException {
		super(fsds);
		conf = prepareConfiguration(defaultConf, DataSourceUtils.getSourceUri(fsds), (String) fsds.getProperty("username"),
				(String) fsds.getProperty("password"));

		Path p = new Path(DataSourceUtils.getSourceUri(fsds));

		Thread.currentThread().setContextClassLoader(HDFSFS.class.getClassLoader());
		fs = p.getFileSystem(conf);
	}

	public void init(CrawlState state) {
		super.init(state);
		String convClazz = (String) fsds.getProperty("converter");
		if (StringUtils.isBlank(convClazz))
			convClazz = DefaultConverter.class.getName();
		try {
			Class clazz = Class.forName(convClazz);
			conv = ((Converter) ReflectionUtils.newInstance(clazz, conf));
		} catch (Throwable t) {
			LOG.warn("Can't instantiate converter '" + convClazz + "', using default: " + t.toString());
			conv = new DefaultConverter();
		}
		try {
			conv.init(state);
		} catch (Exception e) {
			LOG.warn("Can't instantiate converter '" + convClazz + "', using default: " + e.toString());
			conv = new DefaultConverter();
			try {
				conv.init(state);
			} catch (Exception e1) {
				LOG.error("Should never happen", e1);
			}
		}
	}

	public FSObject get(String path) throws IOException {
		Path p = new Path(path);
		FileStatus fstat = fs.getFileStatus(p);
		return new HDFSFSObject(fstat, fs, conf, conv);
	}

	public void close() {
	}

	static {
		defaultConf.put("fs.s3n.impl.disable.cache", "true");
		defaultConf.put("fs.s3.impl.disable.cache", "true");
		defaultConf.put("fs.hdfs.impl.disable.cache", "true");
	}

	private static class HDFSFSObject extends FSObject {
		Path path;
		FileSystem fs;
		Configuration conf;
		Converter conv;
		boolean subCrawl = true;

		HDFSFSObject(FileStatus fstat, FileSystem fs, Configuration conf, Converter conv) throws IOException {
			path = fstat.getPath();
			this.fs = fs;
			this.conf = conf;
			this.conv = conv;
			directory = fstat.isDirectory();
			size = fstat.getLen();
			lastModified = fstat.getModificationTime();
			acls = new String[] { fstat.getPermission().toString() };
			owner = fstat.getOwner();
			group = fstat.getGroup();
			uri = path.makeQualified(fs.getUri(), fs.getWorkingDirectory()).toUri().toString();
			name = this.path.getName();
		}

		public Iterable<FSObject> getChildren() throws IOException {
			FileStatus[] fstats = fs.listStatus(path);
			if (fstats == null) {
				return null;
			}
			if (fstats.length == 0) {
				return null;
			}

			if ((subCrawl) && (isSFOFormat(fstats))) {
				try {
					return new SFOCrawler(fs, path, conf, conv);
				} catch (IOException ioe) {
					HDFSFS.LOG.warn("Failed to sub-crawl " + path + ", attempting regular crawl: " + ioe.getMessage());
				}
			}
			List<FSObject> res = new ArrayList<FSObject>(fstats.length);
			for (int i = 0; i < fstats.length; i++) {
				res.add(new HDFSFSObject(fstats[i], fs, conf, conv));
			}
			return res;
		}

		private boolean isSFOFormat(FileStatus[] fstats) {
			boolean res = true;
			for (FileStatus fs : fstats) {
				Path p = fs.getPath();
				String name = p.getName();
				if (name.startsWith("part-")) {
					int idx = name.lastIndexOf(45);
					try {
						Integer.parseInt(name.substring(idx));
					} catch (NumberFormatException nfe) {
						res = false;
						break;
					}
				} else if ((!name.startsWith("_")) && (!name.startsWith("."))) {
					res = false;
					break;
				}
			}
			return res;
		}

		public InputStream open() throws IOException {
			if (directory) {
				return null;
			}
			return fs.open(path);
		}
	}
}
