package gaia.crawl.gcm;

import gaia.crawl.datasource.DataSourceSpec;
import java.util.Map;

public interface GCMExtension {
	public void register(Map<String, DataSourceSpec> paramMap);
}
