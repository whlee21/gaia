package gaia.crawl.aperture;

import gaia.Defaults;
import gaia.api.Error;
import gaia.crawl.datasource.DataSourceAPI;
import gaia.crawl.datasource.DataSourceSpec;
import gaia.crawl.datasource.FieldMapping;
import gaia.crawl.datasource.FieldMappingUtil;
import gaia.spec.SpecProperty;
import gaia.spec.Validator;
import gaia.utils.StringUtils;

import java.util.List;
import java.util.Map;

public abstract class ApertureDataSourceSpec extends DataSourceSpec {
	protected ApertureDataSourceSpec(String category) {
		super(category);
	}

	protected void addCommonApertureProperties() {
		addBatchProcessingProperties();
		addFieldMappingProperties();
		addCommitProperties();
		addVerifyAccessProperties();
		addBoundaryLimitsProperties();
		addSpecProperty(new SpecProperty.Separator("other limits"));
		addSpecProperty(new SpecProperty("max_docs", "datasource.max_docs", Integer.class,
				Integer.valueOf(Defaults.INSTANCE.getInt(Defaults.Group.datasource, "max_docs")),
				Validator.INT_STRING_VALIDATOR, false));

		addSpecProperty(new SpecProperty("max_bytes", "datasource.max_bytes", Long.class, Long.valueOf(Defaults.INSTANCE
				.getLong(Defaults.Group.datasource, "max_bytes")), Validator.LONG_STRING_VALIDATOR, false));

		addSpecProperty(new SpecProperty.Separator("Error handling"));
		addSpecProperty(new SpecProperty("log_extra_detail", "datasource.log_extra_detail", Boolean.class, Boolean.FALSE,
				Validator.BOOLEAN_VALIDATOR, false, SpecProperty.HINTS_ADVANCED));

		addSpecProperty(new SpecProperty("fail_unsupported_file_types", "datasource.fail_unsupported_file_types",
				Boolean.class, Boolean.FALSE, Validator.BOOLEAN_VALIDATOR, false, SpecProperty.HINTS_ADVANCED));

		addSpecProperty(new SpecProperty("add_failed_docs", "datasource.add_failed_docs", Boolean.class, Boolean.FALSE,
				Validator.BOOLEAN_VALIDATOR, false, SpecProperty.HINTS_ADVANCED));

		addSpecProperty(new SpecProperty("warn_unknown_mime_types", "datasource.warn_unknown_mime_types", Boolean.class,
				Boolean.FALSE, Validator.BOOLEAN_VALIDATOR, false, SpecProperty.HINTS_ADVANCED));
	}

	public List<Error> validate(Map<String, Object> map) {
		List<Error> errors = super.validate(map);
		if (!errors.isEmpty()) {
			return errors;
		}

		Boolean parsing = StringUtils.getBoolean(map.get("parsing"), false);
		if ((parsing != null) && (parsing.equals(Boolean.FALSE))) {
			errors.add(new Error("parsing", Error.E_FORBIDDEN_VALUE, "this crawler does not support parsing==false"));
		}

		return errors;
	}

	public FieldMapping getDefaultFieldMapping() {
		FieldMapping mapping = new FieldMapping();
		FieldMappingUtil.addApertureFieldMapping(mapping, true);
		return mapping;
	}

	public Map<String, Object> cast(Map<String, Object> input) {
		if (!input.containsKey("bounds")) {
			input.put("bounds", DataSourceAPI.Bounds.tree.toString());
		}

		input.put("parsing", Boolean.valueOf(true));
		return super.cast(input);
	}
}
