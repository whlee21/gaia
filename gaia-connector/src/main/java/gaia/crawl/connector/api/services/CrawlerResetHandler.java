package gaia.crawl.connector.api.services;

import gaia.api.ErrorUtils;
import gaia.api.ObjectSerializer;
import gaia.crawl.ConnectorManager;
import gaia.crawl.connector.api.services.parsers.RequestBodyParser;
import gaia.crawl.datasource.DataSourceId;

import java.util.Map;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

public class CrawlerResetHandler extends BaseHandler implements PutMethodHandler {

	public CrawlerResetHandler(ConnectorManager connManager, ObjectSerializer serializer, RequestBodyParser bodyParser) {
		super(connManager, serializer, bodyParser);
	}

	@Override
	public Response handles(MultivaluedMap<String, String> queryParams, HttpHeaders headers, String body) {
		try {
			RequestBody requestBody = bodyParser.parse(body);
			Map<String, Object> m = requestBody.getProperties();
			String collection = (String) getParam("collection", queryParams, m, false);
			String crawler = (String) getParam("crawler", queryParams, m, true);
			String id = (String) getParam("id", queryParams, m, true);
			return buildResponse(Status.OK, connManager.reset(crawler, collection, new DataSourceId(id)));
		} catch (Exception e) {
			throw ErrorUtils.statusExp(e);
		}
	}

}
