package gaia.crawl.dih;

import gaia.api.Error;
import gaia.Defaults;
import gaia.crawl.datasource.DataSourceSpec;
import gaia.crawl.datasource.FieldMapping;
import gaia.spec.SpecProperty;
import gaia.spec.Validator;
import gaia.utils.StringUtils;

import java.util.List;
import java.util.Map;

public class JDBCSpec extends DataSourceSpec {
	public static final String DRIVER = "driver";
	public static final String DELTA_SQL_QUERY = "delta_sql_query";
	public static final String PRIMARY_KEY = "primary_key";
	public static final String SQL_SELECT_STATEMENT = "sql_select_statement";
	public static final String NESTED_QUERIES = "nested_queries";
	public static final String CLEAN_IN_FULL_IMPORT_MODE = "clean_in_full_import_mode";

	public JDBCSpec() {
		super(DataSourceSpec.Category.Jdbc.toString());
	}

	protected void addCrawlerSupportedProperties() {
		addSpecProperty(new SpecProperty.Separator("Connection parameters"));
		addSpecProperty(new SpecProperty("url", "datasource.url", String.class, null, Validator.URI_VALIDATOR, true));

		addSpecProperty(new SpecProperty(DRIVER, "datasource.driver", String.class, null, Validator.NOT_BLANK_VALIDATOR,
				true));

		addSpecProperty(new SpecProperty("username", "datasource.username", String.class, null,
				Validator.NOT_NULL_VALIDATOR, true));

		addSpecProperty(new SpecProperty("password", "datasource.password", String.class, null,
				Validator.NOT_NULL_VALIDATOR, true, SpecProperty.HINTS_PASSWORD));

		addSpecProperty(new SpecProperty.Separator("SQL parameters"));
		addSpecProperty(new SpecProperty(SQL_SELECT_STATEMENT, "datasource.sql_select_statement", String.class, null,
				Validator.NOT_BLANK_VALIDATOR, true, new String[] { SpecProperty.Hint.lengthy.toString() }));

		addSpecProperty(new SpecProperty(PRIMARY_KEY, "datasource.primary_key", String.class, null,
				Validator.NOT_NULL_VALIDATOR, false));

		addSpecProperty(new SpecProperty(DELTA_SQL_QUERY, "datasource.delta_sql_query", String.class, null,
				Validator.NOT_NULL_VALIDATOR, false, new String[] { SpecProperty.Hint.lengthy.toString() }));

		addSpecProperty(new SpecProperty(NESTED_QUERIES, "datasource.nested_queries", List.class, null,
				Validator.NOT_NULL_VALIDATOR, false, new String[] { SpecProperty.Hint.lengthy.toString() }));

		addSpecProperty(new SpecProperty(CLEAN_IN_FULL_IMPORT_MODE, "datasource.clean_in_full_import_mode", Boolean.class,
				Boolean.TRUE, Validator.BOOLEAN_VALIDATOR, false, new String[] { SpecProperty.Hint.advanced.toString() }));

		addFieldMappingProperties();
		addCommitProperties();
		addVerifyAccessProperties();
		addSpecProperty(new SpecProperty("max_docs", "datasource.max_docs", Integer.class, Integer.valueOf(this.defaults
				.getInt(Defaults.Group.datasource, "max_docs")), Validator.INT_STRING_VALIDATOR, false));
	}

	public List<Error> validate(Map<String, Object> map) {
		List<Error> errors = super.validate(map);
		if (!errors.isEmpty()) {
			return errors;
		}

		if ((map.get("verify_access") != null) && (!StringUtils.getBoolean(map.get("verify_access")).booleanValue())) {
			return errors;
		}

		errors.addAll(JDBCDataSourceValidator.validate(map));
		return errors;
	}

	public FieldMapping getDefaultFieldMapping() {
		FieldMapping defaultMapping = new FieldMapping();
		defaultMapping.setDynamicField(null);
		defaultMapping.setAddGaiaSearchFields(true);
		defaultMapping.setDefaultField(null);
		return defaultMapping;
	}
}
