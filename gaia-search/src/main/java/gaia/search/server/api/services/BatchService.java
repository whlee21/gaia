package gaia.search.server.api.services;

import gaia.api.Error;
import gaia.api.ErrorUtils;
import gaia.api.ObjectSerializer;
import gaia.crawl.ConnectorManager;
import gaia.crawl.batch.BatchStatus;
import gaia.search.server.api.services.parsers.RequestBodyParser;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.xnap.commons.i18n.I18nFactory;

import com.google.inject.Injector;

public class BatchService extends BaseService {
	
	private static org.xnap.commons.i18n.I18n i18n = I18nFactory.getI18n(BatchService.class);

	private String collection;
	private ConnectorManager cm;

	public BatchService(ObjectSerializer serializer, RequestBodyParser bodyParser, String collectionName, ConnectorManager cm) {
		super(serializer, bodyParser);		
		this.collection= collectionName;
		this.cm = cm;
	}

	@GET
	@Produces("text/plain")
	public Response getBatches(@Context HttpHeaders headers, @Context UriInfo ui) throws Exception {

		//String crawlerType = (String) getRequest().getAttributes().get("crawler");
		//String dsId = (String) getRequestAttributes().get("id");
		List<Map<String, Object>> res = new ArrayList<Map<String, Object>>();
		List<BatchStatus> stats = cm.listBatches(null, collection, null);
		for (BatchStatus bs : stats) {
			res.add(bs.toMap());
		}
		return buildResponse(Response.Status.OK, res); 
	}

	@DELETE
	@Produces("text/plain")
	public Response deleteBatches(@Context HttpHeaders headers, @Context UriInfo ui) {

//		String crawlerType = (String) getRequest().getAttributes().get("crawler");
//		String dsId = (String) getRequest().getAttributes().get("id");
//		String batchId = (String) getRequest().getAttributes().get("batch_id");
//		String collection = (String) getRequestAttributes().get("coll_name");
		if (collection == null) {
			throw ErrorUtils.statusExp(422, new Error("collection", i18n.tr(Error.E_MISSING_VALUE)));
		}

		List<Error> errors = new ArrayList<Error>();
		try {
			if (!cm.deleteBatches(null, collection, null))
				;
		} catch (Exception e) {
			errors.add(new Error("deleteBatches", i18n.tr(Error.E_EXCEPTION, e.toString())));
		}
		if (errors.size() > 0) {
			throw ErrorUtils.statusExp(422, errors);
		}
		return buildResponse(Response.Status.NO_CONTENT);
	}

	@Path("{crawler}")
	public CrawlerService getBatchCrawlerHandler(@PathParam("crawler") String crawlerName) {
		 return new CrawlerService(serializer, bodyParser, collection, crawlerName, cm);
	}

	// protected ResourceInstance createBatchResource(String collectionName) {
	// Map<Resource.Type, String> mapIds = new HashMap<Resource.Type, String>();
	// mapIds.put(Resource.Type.Batch, collectionName);
	// return createResource(Resource.Type.Batch, mapIds);
	// }
}
