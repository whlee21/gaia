package gaia.search.server.api.services;

import gaia.api.ObjectSerializer;
import gaia.crawl.ConnectorManager;
import gaia.search.server.api.services.parsers.RequestBodyParser;

import javax.ws.rs.GET;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xnap.commons.i18n.I18nFactory;

public class CrawlersStatusService  extends BaseService {
	
	private static org.xnap.commons.i18n.I18n i18n = I18nFactory.getI18n(CrawlersStatusService.class);
	private static final Logger LOG = LoggerFactory.getLogger(CrawlersStatusService.class);
	
	private String crawlerName;
	private ConnectorManager cm;

	public CrawlersStatusService(ObjectSerializer serializer, RequestBodyParser bodyParser, String crawlerName, ConnectorManager cm) {
		super(serializer, bodyParser);
		this.crawlerName = crawlerName;
		this.cm = cm;
	}
	
	@GET
	@Produces("text/plain")
	public Response getCrawlersStatuses(@Context HttpHeaders headers, @Context UriInfo ui) {
		LOG.debug("hhokyung CrawlersStatusService getCrawlersStatuses crawlerName : "+crawlerName);
//		return handleRequest(headers, null, ui, Request.Type.GET, createCrawlersStatusResource(null));
		return null;
	}
}