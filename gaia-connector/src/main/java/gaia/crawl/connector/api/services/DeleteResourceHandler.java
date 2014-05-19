package gaia.crawl.connector.api.services;

import gaia.api.ErrorUtils;
import gaia.api.ObjectSerializer;
import gaia.crawl.ConnectorManager;
import gaia.crawl.connector.api.services.parsers.RequestBodyParser;
import gaia.crawl.datasource.DataSourceId;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

public class DeleteResourceHandler extends BaseHandler implements DeleteMethodHandler {

	public DeleteResourceHandler(ConnectorManager connManager, ObjectSerializer serializer, RequestBodyParser bodyParser) {
		super(connManager, serializer, bodyParser);
	}

	@Override
	public Response handles(MultivaluedMap<String, String> queryParams, HttpHeaders headers) {
		String crawler = (String) getParam("crawler", queryParams, true);
		String collection = (String) getParam("collection", queryParams, false);
		String dsId = (String) getParam("id", queryParams, false);
		String name = (String) getParam("name", queryParams, true);
		DataSourceId ds = null;
		if (dsId != null)
			ds = new DataSourceId(dsId);
		try {
			connManager.deleteResource(crawler, name, collection, ds);
		} catch (Exception e) {
			throw ErrorUtils.statusExp(e);
		}
		return buildResponse(Status.OK, true);
	}

}
