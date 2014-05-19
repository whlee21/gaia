package gaia.crawl.behemoth;

import gaia.crawl.CrawlerController;
import gaia.crawl.DataSourceFactory;

public class BehemothDataSourceFactory extends DataSourceFactory {
	public BehemothDataSourceFactory(CrawlerController cc) {
		super(cc);
		types.put("high_volume_hdfs", new BehemothAccessSpec());
	}
}
