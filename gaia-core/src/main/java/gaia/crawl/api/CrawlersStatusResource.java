package gaia.crawl.api;

import java.util.Map;
import org.restlet.resource.Get;
import org.restlet.resource.ResourceException;

public interface CrawlersStatusResource {
	@Get("json")
	public Map<String, Object> getCrawlersStatus() throws ResourceException;
}
