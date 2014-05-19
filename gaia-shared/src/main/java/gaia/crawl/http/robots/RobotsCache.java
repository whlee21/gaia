package gaia.crawl.http.robots;

import java.util.Collections;
import org.apache.solr.search.LRUCache;

public class RobotsCache extends LRUCache<String, BaseRobotRules> {
	public RobotsCache() {
		init(Collections.emptyMap(), null, null);
	}
}
