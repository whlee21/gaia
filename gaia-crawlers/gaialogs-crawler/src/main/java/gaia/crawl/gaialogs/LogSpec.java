package gaia.crawl.gaialogs;

import gaia.Defaults;
import gaia.crawl.datasource.DataSourceSpec;
import gaia.crawl.datasource.FieldMapping;
import gaia.spec.SpecProperty;
import gaia.spec.Validator;

public class LogSpec extends DataSourceSpec {
	public static final String TYPE = "gaiasearchlogs";
	public static final String DELETE_AFTER = "delete_after";
	public static final Long DELETE_AFTER_DEFAULT = new Long(86400000L);
	public static final Integer COMMIT_WITHIN_DEFAULT = Integer.valueOf(60000);

	public static final LogSpec INSTANCE = new LogSpec();

	public LogSpec() {
		super(DataSourceSpec.Category.Other.toString());
	}

	protected void addCrawlerSupportedProperties() {
		addSpecProperty(new SpecProperty("commit_within", "datasource.commit_within", Integer.class,
				Integer.valueOf(Defaults.INSTANCE.getInt(Defaults.Group.datasource, "commit_within")),
				Validator.INT_STRING_VALIDATOR, false));

		addSpecProperty(new SpecProperty("delete_after", "datasource.delete_after", Long.class, DELETE_AFTER_DEFAULT,
				Validator.NUMBER_VALIDATOR, false));
	}

	public FieldMapping getDefaultFieldMapping() {
		return null;
	}
}
