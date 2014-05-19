package gaia.crawl.fs.ds;

import gaia.Defaults;
import gaia.api.Error;
import gaia.crawl.datasource.DataSource;
import gaia.crawl.datasource.DataSourceAPI;
import gaia.crawl.datasource.DataSourceSpec;
import gaia.crawl.datasource.FieldMapping;
import gaia.crawl.datasource.FieldMappingUtil;
import gaia.crawl.fs.FS;
import gaia.spec.SpecProperty;
import gaia.spec.Validator;
import gaia.utils.StringUtils;

import java.net.URI;
import java.util.List;
import java.util.Map;

public abstract class FSSpec extends DataSourceSpec {
	public static String CRAWL_ITEM_TIMEOUT = "crawl_item_timeout";

	protected FSSpec() {
		super(DataSourceSpec.Category.FileSystem.toString());
	}

	protected void addCommonFSProperties() {
		addSpecProperty(new SpecProperty.Separator("Filesystem URL"));
		addSpecProperty(new SpecProperty("url", "datasource.url", String.class, null, Validator.URI_VALIDATOR, true));

		addSpecProperty(new SpecProperty.Separator("other limits"));
		addSpecProperty(new SpecProperty("max_docs", "datasource.max_docs", Integer.class,
				Integer.valueOf(Defaults.INSTANCE.getInt(Defaults.Group.datasource, "max_docs")),
				Validator.INT_STRING_VALIDATOR, false));

		addSpecProperty(new SpecProperty("max_bytes", "datasource.max_bytes", Long.class, Long.valueOf(Defaults.INSTANCE
				.getLong(Defaults.Group.datasource, "max_bytes")), Validator.LONG_STRING_VALIDATOR, false));

		addSpecProperty(new SpecProperty("index_directories", "datasource.index_directories", Boolean.class,
				Boolean.valueOf(Defaults.INSTANCE.getBoolean(Defaults.Group.datasource, "index_directories",
						Boolean.valueOf(false))), Validator.BOOLEAN_VALIDATOR, false, SpecProperty.HINTS_ADVANCED));

		addSpecProperty(new SpecProperty("remove_old_docs", "datasource.remove_old_docs", Boolean.class,
				Boolean.valueOf(Defaults.INSTANCE.getBoolean(Defaults.Group.datasource, "remove_old_docs",
						Boolean.valueOf(true))), Validator.BOOLEAN_VALIDATOR, false, SpecProperty.HINTS_ADVANCED));

		addSpecProperty(new SpecProperty("max_threads", "datasource.max_threads", Integer.class,
				Integer.valueOf(Defaults.INSTANCE.getInt(Defaults.Group.datasource, "max_threads")),
				Validator.INT_STRING_VALIDATOR, false, SpecProperty.HINTS_ADVANCED));

		addSpecProperty(new SpecProperty("add_failed_docs", "datasource.add_failed_docs", Boolean.class, Boolean.FALSE,
				Validator.BOOLEAN_VALIDATOR, false, SpecProperty.HINTS_ADVANCED));

		addSpecProperty(new SpecProperty(
				CRAWL_ITEM_TIMEOUT,
				"datasource." + CRAWL_ITEM_TIMEOUT,
				Integer.class,
				Integer.valueOf(Defaults.INSTANCE.getInt(Defaults.Group.crawlers, "crawl.item.timeout", Integer.valueOf(600000))),
				Validator.NON_NEG_INT_STRING_VALIDATOR, false, SpecProperty.HINTS_ADVANCED));
	}

	protected void addGeneralProperties() {
		addBatchProcessingProperties();
		addFieldMappingProperties();
		addBoundaryLimitsProperties();
		overwriteBoundaryLimits();
		addCommitProperties();
		addVerifyAccessProperties();
	}

	private void overwriteBoundaryLimits() {
		getSpecProperty("bounds").defaultValue = DataSourceAPI.Bounds.tree.toString();

		List excludes = Defaults.INSTANCE.getList(Defaults.Group.datasource, "gaia.fs.exclude_paths", null);
		if (excludes != null)
			getSpecProperty("exclude_paths").defaultValue = excludes;
	}

	protected void addAuthProperties() {
		addSpecProperty(new SpecProperty.Separator("authentication"));
		addSpecProperty(new SpecProperty("username", "datasource.username", String.class, null,
				Validator.NOT_NULL_VALIDATOR, true));

		addSpecProperty(new SpecProperty("password", "datasource.password", String.class, null,
				Validator.NOT_NULL_VALIDATOR, true, SpecProperty.HINTS_PASSWORD));
	}

	public List<Error> validate(Map<String, Object> map) {
		List<Error> errors = super.validate(map);
		String url = (String) map.get("url");
		if (url == null)
			return errors;
		try {
			URI u = new URI(url);
			String path = u.getPath();
			if ((path == null) || (path.trim().isEmpty())) {
				errors.add(new Error("url", Error.E_INVALID_VALUE, "URLs must always contain a path, at least the leading /"));
			}
		} catch (Throwable e) {
		}
		if ((map.get("verify_access") != null) && (!StringUtils.getBoolean(map.get("verify_access")).booleanValue())) {
			return errors;
		}

		if (errors.isEmpty()) {
			reachabilityCheck(map, url, errors);
		}
		return errors;
	}

	protected abstract void reachabilityCheck(Map<String, Object> paramMap, String paramString, List<Error> paramList);

	public abstract String getFSPrefix();

	public abstract FS createFS(DataSource paramDataSource) throws Exception;

	protected void addCrawlerSupportedProperties() {
	}

	public FieldMapping getDefaultFieldMapping() {
		FieldMapping mapping = new FieldMapping();
		mapping.defineMapping("batch_id", "batch_id");
		FieldMappingUtil.addTikaFieldMapping(mapping, true);
		return mapping;
	}
}
