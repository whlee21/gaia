package gaia.crawl.api;

import org.restlet.resource.Delete;

public interface DataSourceCrawlDataResource {
	@Delete
	public void remove() throws Exception;
}
