package gaia.crawl.connector.api.services;

import gaia.api.ErrorUtils;
import gaia.api.ObjectSerializer;
import gaia.crawl.ConnectorManager;
import gaia.crawl.batch.BatchStatus;
import gaia.crawl.connector.api.services.parsers.RequestBodyParser;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

public class GetBatchStatusHandler extends BaseHandler implements GetMethodHandler {

	public GetBatchStatusHandler(ConnectorManager connManager, ObjectSerializer serializer, RequestBodyParser bodyParser) {
		super(connManager, serializer, bodyParser);
	}

	@Override
	public Response handles(MultivaluedMap<String, String> queryParams, HttpHeaders headers) {
		String crawler = (String) getParam("crawler", queryParams, true);
		String collection = (String) getParam("collection", queryParams, false);
		String id = (String) getParam("id", queryParams, true);
		BatchStatus bs = null;
		try {
			bs = connManager.getBatchStatus(crawler, collection, id);
		} catch (Exception e) {
			throw ErrorUtils.statusExp(e);
		}
		if (bs == null) {
			return buildResponse(Status.NOT_FOUND);
		}
		return buildResponse(Status.OK, bs.toMap());
	}

}
