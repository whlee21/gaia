package gaia.crawl.fs.ds;

import gaia.api.Error;
import gaia.crawl.datasource.DataSource;
import gaia.crawl.fs.FS;
import gaia.crawl.fs.FSObject;

import java.util.List;
import java.util.Map;

public class S3Spec extends FSSpec {
	protected void addCrawlerSupportedProperties() {
		addCommonFSProperties();
		addAuthProperties();
		addGeneralProperties();
	}

	protected void reachabilityCheck(Map<String, Object> map, String url, List<Error> errors) {
		String accessKey = (String) map.get("username");
		String secretKey = (String) map.get("password");
		S3FS fs = null;
		try {
			fs = new S3FS(accessKey, secretKey);
			FSObject fso = fs.get(url);
		} catch (Exception e) {
			errors.add(new Error("url", Error.E_EXCEPTION, e.getMessage()));
		} finally {
			if (fs != null)
				fs.close();
		}
	}

	public String getFSPrefix() {
		return "s3://";
	}

	public FS createFS(DataSource ds) throws Exception {
		return new S3FS(ds);
	}
}
