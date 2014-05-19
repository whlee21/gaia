package gaia.crawl.fs.ds;

import gaia.api.Error;
import gaia.crawl.datasource.DataSource;
import gaia.crawl.fs.FS;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;

public class S3HSpec extends FSSpec {
	protected void addCrawlerSupportedProperties() {
		addCommonFSProperties();
		addAuthProperties();
		addGeneralProperties();
	}

	protected void reachabilityCheck(Map<String, Object> map, String url, List<Error> errors) {
		Path p = new Path(url);
		FileSystem fs = null;
		try {
			String username = (String) map.get("username");
			String password = (String) map.get("password");
			Configuration conf = HDFSFS.prepareConfiguration(HDFSFS.defaultConf, url, username, password);

			fs = p.getFileSystem(conf);
			FileStatus fstat = fs.getFileStatus(p);
		} catch (IOException e) {
			errors.add(new Error("url", Error.E_EXCEPTION, e.getMessage()));
		} finally {
			if (fs != null)
				try {
					fs.close();
				} catch (Throwable t) {
				}
		}
	}

	public String getFSPrefix() {
		return "s3://";
	}

	public FS createFS(DataSource ds) throws Exception {
		return new HDFSFS(ds);
	}
}
