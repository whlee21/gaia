package gaia.crawl.api;

import org.restlet.resource.Get;

public interface DataSourceJobStatusResource {
	@Get("json")
	public Object status() throws Exception;
}
