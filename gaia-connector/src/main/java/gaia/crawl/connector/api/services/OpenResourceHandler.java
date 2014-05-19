package gaia.crawl.connector.api.services;

import gaia.api.ErrorUtils;
import gaia.api.ObjectSerializer;
import gaia.crawl.ConnectorManager;
import gaia.crawl.connector.api.services.parsers.RequestBodyParser;
import gaia.crawl.datasource.DataSourceId;

import java.io.InputStream;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

public class OpenResourceHandler extends BaseHandler implements GetMethodHandler {

	public OpenResourceHandler(ConnectorManager connManager, ObjectSerializer serializer, RequestBodyParser bodyParser) {
		super(connManager, serializer, bodyParser);
	}

	@Override
	public Response handles(MultivaluedMap<String, String> queryParams, HttpHeaders headers) {
		String crawler = (String) getParam("crawler", queryParams, true);
		String collection = (String) getParam("collection", queryParams, false);
		String dsId = (String) getParam("id", queryParams, false);
		DataSourceId ds = null;
		if (dsId != null) {
			ds = new DataSourceId(dsId);
		}
		String name = (String) getParam("name", queryParams, true);
		InputStream is = null;
		try {
			is = connManager.openResource(crawler, name, collection, ds);
		} catch (Exception e) {
			throw ErrorUtils.statusExp(e);
		}
		return buildResponse(Status.OK);
		// FIXME: by whlee21
//		return new InputRepresentation(is, MediaType.APPLICATION_ALL);
	}

}
