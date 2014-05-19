package gaia.search.server.api.services;

import gaia.api.Error;
import gaia.api.ErrorUtils;
import gaia.api.ObjectSerializer;
import gaia.crawl.ConnectorManager;
import gaia.crawl.datasource.DataSource;
import gaia.crawl.datasource.DataSourceId;
import gaia.search.server.api.services.parsers.RequestBodyParser;
import gaia.utils.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xnap.commons.i18n.I18nFactory;

public class CrawlerJobService  extends BaseService {
	
	private static org.xnap.commons.i18n.I18n i18n = I18nFactory.getI18n(CrawlerJobService.class);
	private static final Logger LOG = LoggerFactory.getLogger(CrawlerJobService.class);
	
	private final String collection;
	private final String crawlerName;
	private ConnectorManager cm;

	public CrawlerJobService(ObjectSerializer serializer, RequestBodyParser bodyParser, String collectionName
			, String crawlerName, ConnectorManager cm) {
		super(serializer, bodyParser);
		this.collection = collectionName;
		this.crawlerName = crawlerName;
		this.cm = cm;
	}

	@GET
	@Produces("text/plain")
	public Response getCrawlerJobs(@Context HttpHeaders headers, @Context UriInfo ui) {
		List<Error> errors = new ArrayList<Error>();
		if (crawlerName == null) {
			errors.add(new Error("crawler", i18n.tr(Error.E_MISSING_VALUE)));
		}
		if (errors.size() > 0)
			throw ErrorUtils.statusExp(422, errors);
		try {
			List<Map<String, Object>> res = cm.listJobs(crawlerName, true);
			return buildResponse(Response.Status.OK, res);
		} catch (Exception e) {
			throw ErrorUtils.statusExp(e);
		}
	}

	@PUT
	@Produces("text/plain")
	public Response defineNStartCrawlerJob(String body, @Context HttpHeaders headers, @Context UriInfo ui) throws Exception {

		LOG.debug("### hhokyung defineNStartCrawlerJob 1");

		RequestBody requestBody = getRequestBody(body);
		Map<String, Object> map = requestBody.getProperties();
		
		Response resultVal = defineNStartCrawlerJob(map);
		return resultVal;
	}
	

	public Response defineNStartCrawlerJob(Map<String, Object> map) throws Exception {
		LOG.debug("### hhokyung defineNStartCrawlerJob 1");
		
		List<Error> errors = new ArrayList<Error>();
		
		String newDsId = (String) map.get("ds_id");
		String batchId = (String) map.get("batch_id");
		String crawlerType = (String) map.get("batch_id");

		if (collection == null) {
			errors.add(new Error("collection", i18n.tr(Error.E_MISSING_VALUE)));
		}
		if (batchId == null) {
			errors.add(new Error("batch_id", i18n.tr(Error.E_MISSING_VALUE)));
		}
		if (crawlerType == null) {
			errors.add(new Error("crawler", i18n.tr(Error.E_MISSING_VALUE)));
		}
		String newCollection = (String) map.get("new_collection");
		if (errors.size() > 0) {
			throw ErrorUtils.statusExp(422, errors);
		}
		boolean parse = map.get("parse") != null ? StringUtils.getBoolean(map.get("parse")).booleanValue() : false;
		boolean index = map.get("index") != null ? StringUtils.getBoolean(map.get("index")).booleanValue() : true;

		DataSource ds = null;
		if (newDsId != null) {
			ds = cm.getDataSource(new DataSourceId(newDsId));
			if (ds == null) {
				throw ErrorUtils.statusExp(422, new Error("new_ds_id", Error.E_NOT_FOUND, i18n.tr("unknown data source id " + newDsId)));
			}
		}

		try {
			cm.startBatchJob(crawlerType, newCollection, batchId, ds, parse, index);
			return buildResponse(Response.Status.NO_CONTENT);
		} catch (Exception e) {
			throw ErrorUtils.statusExp(422, i18n.tr(e.toString()));
		}
	}

	@Path("{batch_id}")
	public CrawlerJobBatchService getCrawlerJobBatchHandler(@PathParam("batch_id") String batchId) {
		 return new CrawlerJobBatchService(serializer, bodyParser, collection,  crawlerName, batchId, cm);
	}
}
