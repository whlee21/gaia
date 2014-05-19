package gaia.crawl.connector.api.services;

import gaia.api.Error;
import gaia.api.ErrorUtils;
import gaia.api.ObjectSerializer;
import gaia.crawl.ConnectorManager;
import gaia.crawl.JobStateException;
import gaia.crawl.connector.api.services.parsers.RequestBodyParser;
import gaia.crawl.datasource.DataSourceId;

import java.util.Map;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

public class CrawlHandler extends BaseHandler implements PutMethodHandler {

	public CrawlHandler(ConnectorManager connManager, ObjectSerializer serializer, RequestBodyParser bodyParser) {
		super(connManager, serializer, bodyParser);
	}

	@Override
	public Response handles(MultivaluedMap<String, String> queryParams, HttpHeaders headers, String body) {
		String id = null;
		try {
			RequestBody requestBody = bodyParser.parse(body);
			Map<String, Object> m = requestBody.getProperties();
			id = (String) getParam("id", queryParams, m, true);
			return buildResponse(Status.OK, connManager.crawl(new DataSourceId(id)));
		} catch (JobStateException jse) {
			throw ErrorUtils.statusExp(422, new Error(id, Error.E_INVALID_OPERATION, jse.toString()));
		} catch (Exception e) {
			throw ErrorUtils.statusExp(e);
		}
	}

}
