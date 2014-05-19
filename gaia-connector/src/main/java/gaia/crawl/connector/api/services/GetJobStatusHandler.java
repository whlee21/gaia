package gaia.crawl.connector.api.services;

import gaia.api.ErrorUtils;
import gaia.api.ObjectSerializer;
import gaia.crawl.ConnectorManager;
import gaia.crawl.CrawlId;
import gaia.crawl.connector.api.services.parsers.RequestBodyParser;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

public class GetJobStatusHandler extends BaseHandler implements GetMethodHandler {

	public GetJobStatusHandler(ConnectorManager connManager, ObjectSerializer serializer, RequestBodyParser bodyParser) {
		super(connManager, serializer, bodyParser);
	}

	@Override
	public Response handles(MultivaluedMap<String, String> queryParams, HttpHeaders headers) {
		try {
			String crawler = (String) getParam("crawler", queryParams, false);
			String id = (String) getParam("id", queryParams, true);
			CrawlId cid = new CrawlId(id);
			return buildResponse(Status.OK, connManager.getStatus(crawler, cid));
		} catch (Exception e) {
			throw ErrorUtils.statusExp(e);
		}
	}

}
