package gaia.crawl.solrxml;

import gaia.api.Error;
import gaia.Defaults;
import gaia.crawl.CrawlerUtils;
import gaia.crawl.datasource.DataSourceSpec;
import gaia.crawl.datasource.FieldMapping;
import gaia.spec.SpecProperty;
import gaia.spec.Validator;
import gaia.utils.StringUtils;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class SolrXMLSpec extends DataSourceSpec {
	public static final String INCLUDE_DS_META = "include_datasource_metadata";
	public static final String GENERATE_UNIQUE_KEY = "generate_unique_key";

	public SolrXMLSpec() {
		super(DataSourceSpec.Category.SolrXml.toString());
	}

	protected void addCrawlerSupportedProperties() {
		addSpecProperty(new SpecProperty("max_docs", "datasource.max_docs", Integer.class,
				Integer.valueOf(Defaults.INSTANCE.getInt(Defaults.Group.datasource, "max_docs")),
				Validator.INT_STRING_VALIDATOR, false));

		addSpecProperty(new SpecProperty.Separator("SolrXML file or dir"));
		addSpecProperty(new SpecProperty("path", "datasource.path", String.class, null, Validator.NOT_NULL_VALIDATOR, true));

		addSpecProperty(new SpecProperty("url", "datasource.url", String.class, null, Validator.NOOP_VALIDATOR, false,
				true, SpecProperty.HINTS_DEFAULT));

		addSpecProperty(new SpecProperty.Separator("SolrXML options"));
		addSpecProperty(new SpecProperty(INCLUDE_DS_META, "datasource.include_datasource_metadata", Boolean.class,
				Boolean.TRUE, Validator.BOOLEAN_VALIDATOR, false, SpecProperty.HINTS_ADVANCED));

		addSpecProperty(new SpecProperty(GENERATE_UNIQUE_KEY, "datasource.generate_unique_key", Boolean.class,
				Boolean.TRUE, Validator.BOOLEAN_VALIDATOR, false, SpecProperty.HINTS_ADVANCED));

		addSpecProperty(new SpecProperty.Separator("SolrXML limits"));
		addSpecProperty(new SpecProperty("include_paths", "datasource.include_paths", List.class,
				Arrays.asList(new String[] { ".*\\.xml" }), Validator.NOT_NULL_VALIDATOR, false));

		addSpecProperty(new SpecProperty("exclude_paths", "datasource.exclude_paths", List.class, Collections.emptyList(),
				Validator.NOT_NULL_VALIDATOR, false));

		addBatchProcessingProperties();
		addFieldMappingProperties();
		addCommitProperties();
		addVerifyAccessProperties();
	}

	public List<Error> validate(Map<String, Object> map) {
		List<Error> errors = super.validate(map);
		String fileString = (String) map.get("path");

		if ((map.get("verify_access") != null) && (!StringUtils.getBoolean(map.get("verify_access")).booleanValue())) {
			return errors;
		}

		if (errors.isEmpty()) {
			reachabilityCheck(fileString, errors);
		}
		return errors;
	}

	private void reachabilityCheck(String path, List<Error> errors) {
		File f = CrawlerUtils.resolveRelativePath(path);
		if (!f.exists()) {
			errors
					.add(new Error("path", Error.E_NOT_FOUND, "root path " + f.toURI() + " doesn't exist (permissions issue?)"));
			return;
		}
		if (!f.canRead()) {
			errors.add(new Error("path", Error.E_INVALID_VALUE, "root path " + f.toURI()
					+ " exists but is not readable (permissions issue?)"));
			return;
		}
	}

	public Map<String, Object> cast(Map<String, Object> input) {
		Map<String, Object> res = super.cast(input);
		String fileString = (String) res.get("path");
		File f = CrawlerUtils.resolveRelativePath(fileString);
		res.put("url", f.toURI().toString());
		return res;
	}

	public FieldMapping getDefaultFieldMapping() {
		FieldMapping defaultMapping = new FieldMapping();
		defaultMapping.setDynamicField(null);
		defaultMapping.setDefaultField(null);
		return defaultMapping;
	}
}
