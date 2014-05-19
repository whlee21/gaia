package gaia.crawl.fs.ds;

import gaia.api.Error;
import gaia.crawl.CrawlerUtils;
import gaia.crawl.datasource.DataSource;
import gaia.crawl.fs.FS;
import gaia.spec.SpecProperty;
import gaia.spec.Validator;

import java.io.File;
import java.net.URI;
import java.net.URLDecoder;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileSpec extends FSSpec {
	private static final Logger LOG = LoggerFactory.getLogger(FileSpec.class);

	protected void addCrawlerSupportedProperties() {
		addSpecProperty(new SpecProperty.Separator("root path"));
		addSpecProperty(new SpecProperty("path", "datasource.path", String.class, null, Validator.NOT_BLANK_VALIDATOR, true));

		addCommonFSProperties();
		addGeneralProperties();

		SpecProperty url = getSpecProperty("url");
		url.readOnly = true;
		url.required = false;
	}

	protected void reachabilityCheck(Map<String, Object> map, String path, List<Error> errors) {
		CrawlerUtils.fileReachabilityCheck(path, errors);
	}

	public String getFSPrefix() {
		return "file://";
	}

	public FS createFS(DataSource ds) throws Exception {
		return new FileFS(ds);
	}

	public Map<String, Object> cast(Map<String, Object> input) {
		Map<String, Object> map = super.cast(input);
		String path = (String) map.get("path");
		if (path == null) {
			return map;
		}
		try {
			path = URLDecoder.decode(path, "UTF-8");
		} catch (Exception e) {
			LOG.warn("Can't URLDecode path '" + path + "'", e);
		}
		File f;
		if (path.startsWith(getFSPrefix())) {
			f = new File(path.substring(getFSPrefix().length()));
		} else {
			if (path.startsWith("file:"))
				f = new File(path.substring("file:".length()));
			else
				f = new File(path);
		}
		f = f.getAbsoluteFile();
		path = f.toString();
		URI u = f.toURI();

		map.put("path", path);
		map.put("url", u.toString());
		return map;
	}
}
