package gaia.crawl.gaialogs;

import gaia.crawl.CrawlerController;
import gaia.crawl.DataSourceFactory;

public class LogDataSourceFactory extends DataSourceFactory {
	public LogDataSourceFactory(CrawlerController cc) {
		super(cc);
		types.put("gaiasearchlogs", new LogSpec());
	}
}
