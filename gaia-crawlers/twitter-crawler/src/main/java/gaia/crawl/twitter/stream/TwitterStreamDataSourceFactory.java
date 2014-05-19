package gaia.crawl.twitter.stream;

import gaia.crawl.CrawlerController;
import gaia.crawl.DataSourceFactory;

public class TwitterStreamDataSourceFactory extends DataSourceFactory {
	public TwitterStreamDataSourceFactory(CrawlerController cc) {
		super(cc);
		types.put("twitter_stream", new TwitterAccessSpec());
	}
}
