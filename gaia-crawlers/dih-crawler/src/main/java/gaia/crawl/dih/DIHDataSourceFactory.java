package gaia.crawl.dih;

import gaia.crawl.CrawlerController;
import gaia.crawl.DataSourceFactory;
import gaia.crawl.datasource.DataSourceSpec;

public class DIHDataSourceFactory extends DataSourceFactory {
	public DIHDataSourceFactory(CrawlerController cc) {
		super(cc);
		types.put(DataSourceSpec.Type.jdbc.toString(), new JDBCSpec());
	}
}
