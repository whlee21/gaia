package gaia.crawl.connector.api.services;

import gaia.api.ErrorUtils;
import gaia.api.ObjectSerializer;
import gaia.crawl.ConnectorManager;
import gaia.crawl.connector.api.services.parsers.RequestBodyParser;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

public class GetBatchJobStatusesHandler extends BaseHandler implements GetMethodHandler {

	public GetBatchJobStatusesHandler(ConnectorManager connManager, ObjectSerializer serializer, RequestBodyParser bodyParser) {
		super(connManager, serializer, bodyParser);
	}

	@Override
	public Response handles(MultivaluedMap<String, String> queryParams, HttpHeaders headers) {
		String crawler = (String) getParam("crawler", queryParams, true);
		String collection = (String) getParam("collection", queryParams, false);
		String id = (String) getParam("id", queryParams, false);
		try {
			return buildResponse(Status.OK, connManager.getBatchJobStatuses(crawler, collection, id));
		} catch (Exception e) {
			throw ErrorUtils.statusExp(e);
		}
	}

}
