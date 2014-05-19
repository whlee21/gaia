package gaia.crawl.connector.api.services;

import gaia.api.ErrorUtils;
import gaia.api.ObjectSerializer;
import gaia.crawl.ConnectorManager;
import gaia.crawl.connector.api.services.parsers.RequestBodyParser;

import java.util.Map;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

public class InitCrawlersFromJarHandler extends BaseHandler implements PostMethodHandler {

	public InitCrawlersFromJarHandler(ConnectorManager connManager, ObjectSerializer serializer,
			RequestBodyParser bodyParser) {
		super(connManager, serializer, bodyParser);
	}

	@Override
	public Response handles(MultivaluedMap<String, String> queryParams, HttpHeaders headers, String body) {
		try {
			RequestBody requestBody = bodyParser.parse(body);
			Map<String, Object> m = requestBody.getProperties();
			String url = (String) getParam("url", queryParams, m, true);
			return buildResponse(Response.Status.OK, connManager.initCrawlersFromJar(url));
		} catch (Exception e) {
			throw ErrorUtils.statusExp(e);
		}
	}

}