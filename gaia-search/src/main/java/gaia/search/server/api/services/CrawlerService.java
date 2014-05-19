package gaia.search.server.api.services;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import gaia.api.Error;
import gaia.api.ErrorUtils;
import gaia.api.ObjectSerializer;
import gaia.crawl.ConnectorManager;
import gaia.crawl.batch.BatchStatus;
import gaia.search.server.api.services.parsers.RequestBodyParser;

import javax.ws.rs.DELETE;
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

public class CrawlerService   extends BaseService {
	
	private static org.xnap.commons.i18n.I18n i18n = I18nFactory.getI18n(CrawlerService.class);
	private static final Logger LOG = LoggerFactory.getLogger(CrawlerService.class);

	private String crawlerName;
	private String collection;
	private ConnectorManager cm;
	
	public CrawlerService(ObjectSerializer serializer, RequestBodyParser bodyParser, String collectionName
			, String crawlerName, ConnectorManager cm) {
		super(serializer, bodyParser);
		this.collection = collectionName;
		this.crawlerName = crawlerName;
		this.cm = cm;
	}

	@GET
	@Produces("text/plain")
	public Response getBatchCrawlers(@Context HttpHeaders headers, @Context UriInfo ui) throws Exception {
//		String crawlerType = (String) getRequest().getAttributes().get("crawler");
//		String collection = (String) getRequestAttributes().get("coll_name");
//		String dsId = (String) getRequestAttributes().get("id");
		List<Map<String, Object>> res = new ArrayList<Map<String, Object>>();
		List<BatchStatus> stats = cm.listBatches(crawlerName, collection, null);
		for (BatchStatus bs : stats) {
			res.add(bs.toMap());
		}
		return buildResponse(Response.Status.OK, res); 
	}

	@DELETE
	@Produces("text/plain")
	public Response deleteBatchCrawler(@Context HttpHeaders headers, @Context UriInfo ui) {

		if (collection == null) {
			throw ErrorUtils.statusExp(422, new Error("collection", i18n.tr(Error.E_MISSING_VALUE)));
		}
	
		List<Error> errors = new ArrayList<Error>();
		try {
			if (!cm.deleteBatches(crawlerName, collection, null))
				;
		} catch (Exception e) {
			errors.add(new Error("deleteBatches", Error.E_EXCEPTION, i18n.tr(e.toString())));
		}
		if (errors.size() > 0) {
			throw ErrorUtils.statusExp(422, errors);
		}
		return buildResponse(Response.Status.NO_CONTENT);
	}
	
	@Path("job")
	public CrawlerJobService getBatchCrawlerJobHandler() {
		return new CrawlerJobService(serializer, bodyParser, collection, crawlerName, cm);
	}
}
