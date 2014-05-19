package gaia.search.server.api.services;

import gaia.api.Error;
import gaia.api.ErrorUtils;
import gaia.api.ObjectSerializer;
import gaia.crawl.ConnectorManager;
import gaia.search.server.api.services.parsers.RequestBodyParser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xnap.commons.i18n.I18nFactory;

import com.google.inject.Inject;

@Path("/crawlers/")
public class CrawlersService extends BaseService {
	
	private static org.xnap.commons.i18n.I18n i18n = I18nFactory.getI18n(CrawlersService.class);
	private static final Logger LOG = LoggerFactory.getLogger(CrawlersService.class);
	private ConnectorManager cm;
	private String collection;
	public static final String CRAWLERS_PARAM = "crawlers";

	@Inject
	public CrawlersService(ObjectSerializer serializer, RequestBodyParser bodyParser, ConnectorManager cm) {
		super(serializer, bodyParser);
		this.cm = cm;
	}

	@GET
	@Produces("text/plain")
	public Response getCrawlers(@Context HttpHeaders headers, @Context UriInfo ui) {
		LOG.debug("hhokyung getCrawlersStatuses GET Called");
		List<Map<String, Object>> crawlers = list();
		
		return buildResponse(Response.Status.OK, crawlers);
	}

	@Path("status")
	public CrawlersStatusService getCrawlersStatus() {
		LOG.debug("hhokyung CrawlersStatusService  GET @Path('status') Called");
		return new CrawlersStatusService(serializer, bodyParser, collection, cm);
	}
	

	public Map<String, Object> initCrawlers(Map<String, Object> m) {
		if (m.size() == 0) {
			throw ErrorUtils.statusExp(422, i18n.tr("No input content found"));
		}
		Map<String, Object> res = new HashMap<String, Object>();
		for (Map.Entry<String, Object> e : m.entrySet()) {
			if (((String) e.getKey()).equals("jar"))
				try {
					List<String> ccs = cm.initCrawlersFromJar(e.getValue().toString());

					if (ccs != null)
						for (String c : ccs)
							res.put(c, "ok:initialized");
				} catch (Exception ex) {
					res.put(e.getKey(),
							new Error((String) e.getKey(), i18n.tr(Error.E_INVALID_VALUE, e.getValue() + ": " + ex.toString())));
				}
			else {
				try {
					if (!cm.getCrawlerTypes().contains(e.getValue().toString()))
						try {
							boolean inited = cm.initCrawler(e.getValue().toString(), (String) e.getKey());
							if (inited)
								res.put(e.getKey(), "ok:initialized");
						} catch (Exception ex) {
							res.put(e.getKey(), new Error((String) e.getKey(), i18n.tr(Error.E_INVALID_VALUE, ex.toString())));
						}
					else
						res.put(e.getKey(), "ok:already exists");
				} catch (Exception ex) {
					res.put(e.getKey(), new Error((String) e.getKey(), Error.E_EXCEPTION, i18n.tr(ex.getMessage())));
				}
			}
		}
		return res;
	}

	public List<Map<String, Object>> list() {
		List<Map<String, Object>> crawlersList = new ArrayList<Map<String, Object>>();
		Set<String> types = null;
		try {
			types = cm.getCrawlerTypes();
		} catch (Exception e1) {
			LOG.warn("Unable to obtain a list of crawler types", e1);
			types = Collections.emptySet();
		}

		List<String> names = new ArrayList<String>(types);
		Collections.sort(names);
		for (String name : names) {
			Map<String, Object> res;
			try {
				res = cm.getCrawlerSpecs(name);
			} catch (Exception e) {
				LOG.warn(i18n.tr("Unable to obtain a list of datasource types for crawler " + name), e);
				continue;
			}

			crawlersList.add(res);
		}
		return crawlersList;
	}
}