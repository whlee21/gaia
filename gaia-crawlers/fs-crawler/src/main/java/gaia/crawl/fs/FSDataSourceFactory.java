package gaia.crawl.fs;

import gaia.crawl.CrawlerController;
import gaia.crawl.DataSourceFactory;
import gaia.crawl.DataSourceFactoryException;
import gaia.crawl.datasource.DataSource;
import gaia.crawl.fs.ds.CIFSFSSpec;
import gaia.crawl.fs.ds.FSSpec;
import gaia.crawl.fs.ds.FileSpec;
import gaia.crawl.fs.ds.FtpSpec;
import gaia.crawl.fs.ds.HDFSSpec;
import gaia.crawl.fs.ds.S3HSpec;
import gaia.crawl.fs.ds.S3Spec;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FSDataSourceFactory extends DataSourceFactory implements FSFactory {
	private static final Logger LOG = LoggerFactory.getLogger(FSDataSourceFactory.class);

	protected FSDataSourceFactory(CrawlerController cc) {
		super(cc);
		types.put(Type.file.toString(), new FileSpec());
		types.put(Type.s3.toString(), new S3Spec());
		types.put(Type.s3h.toString(), new S3HSpec());
		types.put(Type.hdfs.toString(), new HDFSSpec());
		types.put(Type.ftp.toString(), new FtpSpec());
		types.put(Type.smb.toString(), new CIFSFSSpec());
	}

	public DataSource create(Map<String, Object> m, String collection) throws DataSourceFactoryException {
		DataSource ds = super.create(m, collection);
		if (ds.getBoolean("remove_old_docs", true)) {
			ds.getFieldMapping().defineMapping("batch_id", "batch_id");
		}
		initializeUri(ds, (FSSpec) types.get(ds.getType()));
		return ds;
	}

	private void initializeUri(DataSource ds, FSSpec spec) {
		String path = (String) ds.getProperty("url");
		if (path == null) {
			return;
		}

		if (path.matches("[A-Za-z0-9]+:/[/]?.*")) {
			URI u = null;
			try {
				u = new URI(path);
				u = u.normalize();
			} catch (Exception e) {
				LOG.warn("Invalid URI syntax: " + path, e);
			}
			if (u != null) {
				path = u.getRawPath();

				String credString = u.getUserInfo();
				if (credString != null) {
					String[] creds = credString.split(":");
					ds.setProperty("username", creds[0]);
					ds.setProperty("password", creds[1]);
					try {
						u = new URI(u.getScheme(), null, u.getHost(), u.getPort(), u.getPath(), u.getQuery(), u.getFragment());
					} catch (URISyntaxException e) {
						LOG.warn("Can't remove userInfo from url: " + u.toString(), e);
					}
					ds.setProperty("url", u.toString());
				}
			}
		} else {
			URI u = null;
			String uri = null;
			String prefix = spec.getFSPrefix();
			if ((prefix.endsWith("/")) && (path.charAt(0) == '/'))
				uri = prefix + path.substring(1);
			else
				uri = prefix + path;
			try {
				u = new URI(uri).normalize();
				uri = u.toString();
			} catch (Exception e) {
				LOG.info("Can't create URI prefix, using raw path + FS prefix: " + e.getMessage());
			}
			ds.setProperty("url", uri);
		}
	}

	public FSSpec getSpec(DataSource ds) {
		return (FSSpec) types.get(ds.getType());
	}

	public FS createFS(DataSource ds) throws Exception {
		FSSpec spec = (FSSpec) types.get(ds.getType());
		if (spec == null) {
			throw new Exception("Unknown data source type '" + ds.getType() + "'");
		}
		return spec.createFS(ds);
	}

	public static enum Type {
		file,

		s3h,

		s3,

		hdfs,

		ftp,

		smb;
	}
}
