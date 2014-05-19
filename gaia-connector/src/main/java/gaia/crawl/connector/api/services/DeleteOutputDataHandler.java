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

public class DeleteOutputDataHandler extends BaseHandler implements DeleteMethodHandler {

	public DeleteOutputDataHandler(ConnectorManager connManager, ObjectSerializer serializer, RequestBodyParser bodyParser) {
		super(connManager, serializer, bodyParser);
	}

	@Override
	public Response handles(MultivaluedMap<String, String> queryParams, HttpHeaders headers) {
		String id = (String) getParam("id", queryParams, true);
		DataSourceId dsId = new DataSourceId(id);
		try {
			connManager.deleteOutputData(dsId);
			return buildResponse(Status.OK);
		} catch (Exception e) {
			throw ErrorUtils.statusExp(e);
		}
	}

}
