package gaia;

import gaia.crawl.datasource.DataSourceAPI;
import gaia.crawl.datasource.DataSourceSpec;
import gaia.crawl.datasource.FieldMapping;
import gaia.crawl.impl.TikaParserController;
import gaia.yaml.KVYamlBean;

import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

@Singleton
public class Defaults extends KVYamlBean {
	private static final Logger LOG = LoggerFactory.getLogger(Defaults.class);
	public static final String ENABLED_CRAWLERS_KEY = "enabled.crawlers";
	public static final String ENABLED_DATASOURCES_KEY = "enabled.datasources";
	public static final String RESTRICTED_DATASOURCES_KEY = "restricted.datasources";
	public static final String EXTRA_CRAWLERS_KEY = "extra.crawlers";
	public static final String CRAWL_ITEM_TIMEOUT_KEY = "crawl.item.timeout";
	public static Defaults INSTANCE;
	public static Injector injector;

	public Defaults() {
	}

	@Inject
	public Defaults(@Named("defaults-filename") String file) {
		super(file, true);
	}

	protected void initDefaultValues() {
		init(Group.datasource, "caching", Boolean.valueOf(false));
		init(Group.datasource, "commit_on_finish", Boolean.valueOf(true));
		init(Group.datasource, "commit_within", Integer.valueOf(900000));
		init(Group.datasource, "bounds", "none");
		init(Group.datasource, "gaia.aperture." + DataSourceSpec.Type.web.toString() + "." + "bounds",
				DataSourceAPI.Bounds.tree.toString());
		init(Group.datasource, "gaia.aperture." + DataSourceSpec.Type.web.toString() + "." + "crawl_depth",
				Integer.valueOf(3));
		init(Group.datasource, "crawl_depth", Integer.valueOf(-1));
		init(Group.datasource, "indexing", Boolean.valueOf(true));
		init(Group.datasource, "max_bytes", Integer.valueOf(10485760));
		init(Group.datasource, "max_docs", Integer.valueOf(-1));
		init(Group.datasource, "max_threads", Integer.valueOf(1));
		init(Group.datasource, "parsing", Boolean.valueOf(true));
		init(Group.datasource, "verify_access", Boolean.valueOf(true));
		init(Group.datasource, "proxy_host", null);
		init(Group.datasource, "proxy_port", Integer.valueOf(-1));
		init(Group.datasource, "proxy_username", null);
		init(Group.datasource, "proxy_password", null);
		init(Group.datasource, "use_direct_solr", Boolean.valueOf(false));
		init(Group.datasource, "add_failed_docs", Boolean.valueOf(false));

		init(Group.datasource, "gaia.aperture." + DataSourceSpec.Type.web.toString() + "." + "exclude_paths",
				Arrays.asList(new String[0]));
		init(Group.datasource, "gaia.aperture." + DataSourceSpec.Type.file.toString() + "." + "exclude_paths",
				Arrays.asList(new String[0]));
		init(Group.datasource, "gaia.fs.exclude_paths", Arrays.asList(new String[0]));
		init(Group.datasource, "gaia.gcm." + DataSourceSpec.Type.sharepoint.toString() + "." + "excluded_urls",
				Arrays.asList(new String[0]));

		FieldMapping defaultMapping = FieldMapping.defaultFieldMapping();
		init(Group.datasource, FieldMapping.MAPPING_APERTURE_KEY, defaultMapping);
		init(Group.datasource, FieldMapping.MAPPING_TIKA_KEY, defaultMapping);

		init(Group.datasource, TikaParserController.INCLUDE_IMAGES_KEY, Boolean.valueOf(false));
		init(Group.datasource, TikaParserController.FLATTEN_COMPOUND_KEY, Boolean.valueOf(true));

		init(Group.crawlers, DataSourceAPI.FS_CRAWL_HOME, null);

		init(Group.crawlers, CRAWL_ITEM_TIMEOUT_KEY, Integer.valueOf(600000));

		init(Group.crawlers, ENABLED_CRAWLERS_KEY, "");

		init(Group.http, "agent.string", "");
		init(Group.http, "agent.browser", "Mozilla/5.0");
		init(Group.http, "agent.email", "crawler at example dot com");
		init(Group.http, "agent.name", "GaiaSearch");
		init(Group.http, "agent.url", "");
		init(Group.http, "agent.version", "");
		init(Group.http, "num.threads", Integer.valueOf(1));
		init(Group.http, "timeout", Integer.valueOf(10000));
		init(Group.http, "accept.charset", "utf-8,ISO-8859-1;q=0.7,*;q=0.7");
		init(Group.http, "accept.language", "en-us,en-gb,en;q=0.7,*;q=0.3");
		init(Group.http, "accept.mime",
				"text/html,application/xml;q=0.9,application/xhtml+xml,text/xml;q=0.9,text/plain;q=0.8,image/png,*/*;q=0.5");
		init(Group.http, "accept.encoding", "x-gzip, gzip, deflate");
		init(Group.http, "max.redirects", Integer.valueOf(10));
		init(Group.http, "use.http11", Boolean.valueOf(true));
		init(Group.http, "crawl.delay", Integer.valueOf(2000));

		init(Group.ftp, "username", "anonymous");
		init(Group.ftp, "password", "crawl@example.com");
	}

	public static void main(String[] args) throws Exception {
		if (args.length == 0) {
			System.err.println("Usage: Defaults <outputFile>");
			System.exit(-1);
		}
		Defaults d = new Defaults("defaults.yml");
		d.save(args[0]);
		System.exit(0);
	}

	public static enum Group implements KVYamlBean.Group {
		datasource, crawlers, http, ftp, click, general;
	}
}
