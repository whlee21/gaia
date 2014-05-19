package gaia.crawl.datasource;

import gaia.Defaults;
import gaia.api.Error;
import gaia.spec.AllowedValuesValidator;
import gaia.spec.Spec;
import gaia.spec.SpecProperty;
import gaia.spec.Validator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class DataSourceSpec extends Spec {
	protected static final String DS_ = "datasource.";
	protected Defaults defaults = Defaults.INSTANCE;

	protected DataSourceSpec(String category) {
		super(category);
		addSpecProperty(new SpecProperty.Separator("general"));

		addSpecProperty(new SpecProperty("id", "datasource.id", Object.class, null, Validator.NOT_BLANK_VALIDATOR, false,
				true, SpecProperty.HINTS_DEFAULT));

		addSpecProperty(new SpecProperty("name", "datasource.name", String.class, null, Validator.NOT_BLANK_VALIDATOR, true));

		addSpecProperty(new SpecProperty("crawler", "datasource.crawler", String.class, null,
				Validator.NOT_BLANK_VALIDATOR, true));

		addSpecProperty(new SpecProperty("type", "datasource.type", String.class, null, Validator.NOT_BLANK_VALIDATOR, true));

		addSpecProperty(new SpecProperty("category", "datasource.category", String.class, null, Validator.NOOP_VALIDATOR,
				false, true, new String[] { SpecProperty.Hint.hidden.toString() }));

		addSpecProperty(new SpecProperty("collection", "datasource.collection", String.class, null,
				Validator.NOT_BLANK_VALIDATOR, true));

		addCrawlerSupportedProperties();
		addSpecProperty(new SpecProperty("output_type", "datasource.output_type", String.class, "solr",
				Validator.TREAT_BLANK_AS_NULL_VALIDATOR, false, SpecProperty.HINTS_ADVANCED));

		addSpecProperty(new SpecProperty("output_args", "datasource.output_args", String.class, "threads=2,buffer=1",
				Validator.TREAT_BLANK_AS_NULL_VALIDATOR, false, SpecProperty.HINTS_ADVANCED));
	}

	protected abstract void addCrawlerSupportedProperties();

	protected void addBatchProcessingProperties() {
		addSpecProperty(new SpecProperty.Separator("batch processing"));
		addSpecProperty(new SpecProperty("parsing", "datasource.parsing", Boolean.class, Boolean.valueOf(defaults
				.getBoolean(Defaults.Group.datasource, "parsing", Boolean.valueOf(true))), Validator.BOOLEAN_VALIDATOR, false,
				SpecProperty.HINTS_ADVANCED));

		addSpecProperty(new SpecProperty("indexing", "datasource.indexing", Boolean.class, Boolean.valueOf(defaults
				.getBoolean(Defaults.Group.datasource, "indexing", Boolean.valueOf(true))), Validator.BOOLEAN_VALIDATOR, false,
				SpecProperty.HINTS_ADVANCED));

		addSpecProperty(new SpecProperty("caching", "datasource.caching", Boolean.class, Boolean.valueOf(defaults
				.getBoolean(Defaults.Group.datasource, "caching", Boolean.valueOf(false))), Validator.BOOLEAN_VALIDATOR, false,
				SpecProperty.HINTS_ADVANCED));
	}

	protected void addBoundaryLimitsProperties() {
		addSpecProperty(new SpecProperty.Separator("boundary limits"));
		addSpecProperty(new SpecProperty("crawl_depth", "datasource.crawl_depth", Long.class, Long.valueOf(defaults
				.getLong(Defaults.Group.datasource, "crawl_depth")), Validator.LONG_STRING_VALIDATOR, false));

		List<Object> values = new ArrayList<Object>();
		for (DataSourceAPI.Bounds bound : DataSourceAPI.Bounds.values()) {
			values.add(bound.toString());
		}
		addSpecProperty(new SpecProperty("bounds", "datasource.bounds", String.class, defaults.getString(
				Defaults.Group.datasource, "bounds"), new AllowedValuesValidator(values), false, false, null, values));

		addSpecProperty(new SpecProperty("include_paths", "datasource.include_paths", List.class, Collections.emptyList(),
				Validator.VALID_REGEX_VALIDATOR, false));

		addSpecProperty(new SpecProperty("exclude_paths", "datasource.exclude_paths", List.class, Collections.emptyList(),
				Validator.VALID_REGEX_VALIDATOR, false));
	}

	protected void addFieldMappingProperties() {
		addSpecProperty(new SpecProperty.Separator("field mapping"));
		addSpecProperty(new SpecProperty("mapping", "datasource.mapping", Map.class, null, Validator.NOOP_VALIDATOR, false));
	}

	protected void addCommitProperties() {
		addSpecProperty(new SpecProperty("commit_within", "datasource.commit_within", Integer.class,
				Integer.valueOf(defaults.getInt(Defaults.Group.datasource, "commit_within")),
				Validator.INT_STRING_VALIDATOR, false, SpecProperty.HINTS_ADVANCED));

		addSpecProperty(new SpecProperty("commit_on_finish", "datasource.commit_on_finish", Boolean.class,
				Boolean.valueOf(defaults.getBoolean(Defaults.Group.datasource, "commit_on_finish")),
				Validator.BOOLEAN_VALIDATOR, false, SpecProperty.HINTS_ADVANCED));
	}

	protected void addVerifyAccessProperties() {
		addSpecProperty(new SpecProperty("verify_access", "datasource.verify_access", Boolean.class,
				Boolean.valueOf(defaults.getBoolean(Defaults.Group.datasource, "verify_access")),
				Validator.BOOLEAN_VALIDATOR, false, SpecProperty.HINTS_ADVANCED));
	}

	protected List<Error> rawValidate(Map<String, Object> map) {
		return super.validate(map);
	}

	public List<Error> validate(Map<String, Object> map) {
		List<Error> res = rawValidate(map);

		Set<String> fields = new HashSet<String>(map.keySet());
		for (SpecProperty p : getSpecProperties()) {
			fields.remove(p.name);
		}
		if (fields.size() > 0) {
			res.add(new Error(fields.toString(), Error.E_FORBIDDEN_KEY, "Unknown or dissallowed key found:" + fields));
		}
		return res;
	}

	public static Map<String, Object> toMap(DataSourceSpec spec) {
		HashMap<String, Object> res = new HashMap<String, Object>();
		res.put("category", spec.name);
		List<SpecProperty> specProps = spec.getSpecProperties();
		List<Object> props = new ArrayList<Object>(specProps.size());
		res.put("props", props);
		for (SpecProperty p : specProps) {
			Map<String, Object> mp = new HashMap<String, Object>();
			props.add(mp);
			if ((p instanceof SpecProperty.Separator)) {
				mp.put("name", "---");
				mp.put("description", p.description);
			} else {
				mp.put("name", p.name);
				mp.put("required", Boolean.valueOf(p.required));
				mp.put("read_only", Boolean.valueOf(p.readOnly));
				mp.put("hints", Arrays.asList(p.hints));
				mp.put("description", p.description);
				mp.put("default_value", p.defaultValue);
				mp.put("type", p.type != null ? p.type.getSimpleName().toLowerCase() : null);
				mp.put("allowed_values", p.allowedValues);
			}
		}

		return res;
	}

	public FieldMapping getDefaultFieldMapping() {
		return null;
	}

	public Map<String, Object> cast(Map<String, Object> input) {
		input.put("category", name);
		return super.cast(input);
	}

	public static enum Type {
		web, file, jdbc, solrxml, sharepoint, external;
	}

	public static enum Category {
		Web, FileSystem, Jdbc, SolrXml, GCM, External, Other;
	}
}
