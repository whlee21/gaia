package gaia.crawl.api;

import gaia.api.Error;
import gaia.api.ErrorUtils;
import gaia.crawl.ConnectorManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.restlet.resource.Get;
import org.restlet.resource.Post;
import org.restlet.resource.ServerResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;

public class CrawlersServerResource extends ServerResource implements CrawlersResource {
	private static final Logger LOG = LoggerFactory.getLogger(CrawlersServerResource.class);
	private ConnectorManager crawlerManager;
	public static final String CRAWLERS_PARAM = "crawlers";

	@Inject
	public CrawlersServerResource(ConnectorManager crawlerManager) {
		this.crawlerManager = crawlerManager;
	}

	@Post("json")
	public Map<String, Object> initCrawlers(Map<String, Object> m) {
		if (m.size() == 0) {
			throw ErrorUtils.statusExp(422, "No input content found");
		}
		Map<String, Object> res = new HashMap<String, Object>();
		for (Map.Entry<String, Object> e : m.entrySet()) {
			if (((String) e.getKey()).equals("jar"))
				try {
					List<String> ccs = crawlerManager.initCrawlersFromJar(e.getValue().toString());

					if (ccs != null)
						for (String c : ccs)
							res.put(c, "ok:initialized");
				} catch (Exception ex) {
					res.put(e.getKey(),
							new Error((String) e.getKey(), Error.E_INVALID_VALUE, e.getValue() + ": " + ex.toString()));
				}
			else {
				try {
					if (!crawlerManager.getCrawlerTypes().contains(e.getValue().toString()))
						try {
							boolean inited = crawlerManager.initCrawler(e.getValue().toString(), (String) e.getKey());
							if (inited)
								res.put(e.getKey(), "ok:initialized");
						} catch (Exception ex) {
							res.put(e.getKey(), new Error((String) e.getKey(), Error.E_INVALID_VALUE, ex.toString()));
						}
					else
						res.put(e.getKey(), "ok:already exists");
				} catch (Exception ex) {
					res.put(e.getKey(), new Error((String) e.getKey(), Error.E_EXCEPTION, ex.getMessage()));
				}
			}
		}
		return res;
	}

	@Get("json")
	public List<Map<String, Object>> list() {
		List<Map<String, Object>> crawlersList = new ArrayList<Map<String, Object>>();
		Set<String> types = null;
		try {
			types = crawlerManager.getCrawlerTypes();
		} catch (Exception e1) {
			LOG.warn("Unable to obtain a list of crawler types", e1);
			types = Collections.emptySet();
		}

		List<String> names = new ArrayList<String>(types);
		Collections.sort(names);
		for (String name : names) {
			Map<String, Object> res;
			try {
				res = crawlerManager.getCrawlerSpecs(name);
			} catch (Exception e) {
				LOG.warn("Unable to obtain a list of datasource types for crawler " + name, e);
				continue;
			}

			crawlersList.add(res);
		}
		return crawlersList;
	}
}
