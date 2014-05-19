package gaia.crawl.connector.api.services;

import gaia.api.ErrorUtils;
import gaia.api.ObjectSerializer;
import gaia.crawl.ConnectorManager;
import gaia.crawl.connector.api.services.parsers.RequestBodyParser;
import gaia.utils.StringUtils;

import java.util.Map;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

public class SetClosingHandler extends BaseHandler implements PutMethodHandler {

	public SetClosingHandler(ConnectorManager connManager, ObjectSerializer serializer, RequestBodyParser bodyParser) {
		super(connManager, serializer, bodyParser);
	}

	@Override
	public Response handles(MultivaluedMap<String, String> queryParams, HttpHeaders headers, String body) {
		try {
			RequestBody requestBody = bodyParser.parse(body);
			Map<String, Object> m = requestBody.getProperties();
			String collection = (String) getParam("collection", queryParams, m, false);
			String crawler = (String) getParam("crawler", queryParams, m, false);
			boolean value = StringUtils.getBoolean((String) getParam("closing", queryParams, m, true), true).booleanValue();
			connManager.setClosing(crawler, collection, value);
			return buildResponse(Status.OK);
		} catch (Exception e) {
			throw ErrorUtils.statusExp(e);
		}
	}

}
