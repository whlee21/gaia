package gaia.crawl.connector.api.services;

import java.util.Map;

import gaia.api.ErrorUtils;
import gaia.api.ObjectSerializer;
import gaia.crawl.ConnectorManager;
import gaia.crawl.connector.api.services.parsers.RequestBodyParser;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

public class InitCrawlerHandler extends BaseHandler implements PostMethodHandler {

	public InitCrawlerHandler(ConnectorManager connManager, ObjectSerializer serializer, RequestBodyParser bodyParser) {
		super(connManager, serializer, bodyParser);
	}

	@Override
	public Response handles(MultivaluedMap<String, String> queryParams, HttpHeaders headers, String body) {
		try {
			RequestBody requestBody = bodyParser.parse(body);
			Map<String, Object> m = requestBody.getProperties();
			String alias = (String) getParam("alias", queryParams, m, true);
			String className = (String) getParam("class", queryParams, m, true);
			return buildResponse(Response.Status.OK, connManager.initCrawler(alias, className));
		} catch (Exception e) {
			throw ErrorUtils.statusExp(e);
		}
	}
}
