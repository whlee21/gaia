package gaia.crawl.connector.api.services;

import gaia.api.ErrorUtils;
import gaia.api.ObjectSerializer;
import gaia.crawl.ConnectorManager;
import gaia.crawl.connector.api.services.parsers.RequestBodyParser;
import gaia.utils.StringUtils;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

public class ListJobsHandler extends BaseHandler implements GetMethodHandler {

	public ListJobsHandler(ConnectorManager connManager, ObjectSerializer serializer, RequestBodyParser bodyParser) {
		super(connManager, serializer, bodyParser);
	}

	@Override
	public Response handles(MultivaluedMap<String, String> queryParams, HttpHeaders headers) {
		try {
			String crawler = (String) getParam("crawler", queryParams, false);
			boolean batch = StringUtils.getBoolean((String) getParam("batch", queryParams, false), true).booleanValue();
			return buildResponse(Status.OK, connManager.listJobs(crawler, batch));
		} catch (Exception e) {
			throw ErrorUtils.statusExp(e);
		}
	}

}
