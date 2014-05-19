package gaia.crawl.fs.ds;

import gaia.api.Error;
import gaia.Defaults;
import gaia.crawl.datasource.DataSource;
import gaia.crawl.fs.FS;
import gaia.crawl.fs.hdfs.Converter;
import gaia.spec.SpecProperty;
import gaia.spec.Validator;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HDFSSpec extends FSSpec {
	private static final Logger LOG = LoggerFactory.getLogger(HDFSSpec.class);
	public static final String CONVERTER = "converter";

	protected void addCrawlerSupportedProperties() {
		addCommonFSProperties();
		addSpecProperty(new SpecProperty(CONVERTER, "datasource.converter", String.class, Defaults.INSTANCE.getString(
				Defaults.Group.datasource, CONVERTER), new ConverterValidator(), false));

		addGeneralProperties();
	}

	protected synchronized void reachabilityCheck(Map<String, Object> map, String url, List<Error> errors) {
		Path p = new Path(url);
		ClassLoader ctxCl = Thread.currentThread().getContextClassLoader();
		try {
			String username = (String) map.get("username");
			String password = (String) map.get("password");
			Configuration conf = HDFSFS.prepareConfiguration(HDFSFS.defaultConf, url, username, password);

			Thread.currentThread().setContextClassLoader(HDFSFS.class.getClassLoader());
			StringWriter sw = new StringWriter();
			Configuration.dumpConfiguration(conf, sw);
			LOG.info(new StringBuilder().append("Conf: ").append(sw.toString()).toString());
			FileSystem fs = p.getFileSystem(conf);
			FileStatus fstat = fs.getFileStatus(p);
			LOG.debug(new StringBuilder().append("rootUri status: ").append(fstat.getGroup()).append(":")
					.append(fstat.getOwner()).append(" ").append(fstat.isDirectory() ? "d" : "-").append(fstat.getPermission())
					.toString());
		} catch (IOException e) {
			LOG.error("reachabilityCheck", e);
			errors.add(new Error("url", Error.E_EXCEPTION, e.getMessage()));
		} finally {
			Thread.currentThread().setContextClassLoader(ctxCl);
		}
	}

	public String getFSPrefix() {
		return "hdfs://";
	}

	public FS createFS(DataSource ds) throws Exception {
		return new HDFSFS(ds);
	}

	public static class ConverterValidator extends Validator {
		public List<Error> validate(SpecProperty specProp, Object value) {
			if ((value == null) || (value.toString().trim().length() == 0)) {
				if (!specProp.required) {
					return Collections.emptyList();
				}
				List<Error> res = new ArrayList<Error>(1);
				res.add(new Error(specProp.name, Error.E_EMPTY_VALUE));
				return res;
			}
			List<Error> errors = new ArrayList<Error>();
			String clazz = value.toString();
			try {
				Class cls = Class.forName(clazz);
				if (!Converter.class.isAssignableFrom(cls))
					errors.add(new Error(CONVERTER, Error.E_INVALID_VALUE, "not a subclass of Converter"));
			} catch (Throwable t) {
				errors.add(new Error(CONVERTER, Error.E_INVALID_VALUE, "cannot load Converter class: " + t.getMessage()));
			}
			return errors;
		}

		public Object cast(SpecProperty specProp, Object value) {
			return value.toString();
		}
	}
}
