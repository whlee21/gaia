package gaia.crawl.datasource;

import gaia.api.Error;

import java.util.List;
import java.util.Map;

public class GenericSpec extends DataSourceSpec {
	public GenericSpec() {
		super(DataSourceSpec.Category.Other.toString());
	}

	public List<Error> validate(Map<String, Object> map) {
		return super.validate(map);
	}

	protected void addCrawlerSupportedProperties() {
	}
}
