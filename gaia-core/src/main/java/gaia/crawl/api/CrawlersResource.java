package gaia.crawl.api;

import java.util.List;
import java.util.Map;
import org.restlet.resource.Get;
import org.restlet.resource.Post;

public interface CrawlersResource {
	@Post("json")
	public Map<String, Object> initCrawlers(Map<String, Object> paramMap);

	@Get("json")
	public List<Map<String, Object>> list();
}
