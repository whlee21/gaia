package gaia.crawl.connector.api.services;

import gaia.api.ErrorUtils;
import gaia.api.ObjectSerializer;
import gaia.crawl.ConnectorManager;
import gaia.crawl.connector.api.services.parsers.RequestBodyParser;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

public class GetCrawlerSpecsHandler extends BaseHandler implements GetMethodHandler {

	public GetCrawlerSpecsHandler(ConnectorManager connManager, ObjectSerializer serializer, RequestBodyParser bodyParser) {
		super(connManager, serializer, bodyParser);
	}

	@Override
	public Response handles(MultivaluedMap<String, String> queryParams, HttpHeaders headers) {
		try {
			String crawler = (String) getParam("crawler", queryParams, true);
			return buildResponse(Status.OK, connManager.getCrawlerSpecs(crawler));
		} catch (Exception e) {
			throw ErrorUtils.statusExp(e);
		}
	}

}
