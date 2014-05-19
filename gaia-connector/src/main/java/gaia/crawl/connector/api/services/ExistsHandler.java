package gaia.crawl.connector.api.services;

import gaia.api.ErrorUtils;
import gaia.api.ObjectSerializer;
import gaia.crawl.ConnectorManager;
import gaia.crawl.connector.api.services.parsers.RequestBodyParser;
import gaia.crawl.datasource.DataSource;
import gaia.crawl.datasource.DataSourceId;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

public class ExistsHandler extends BaseHandler implements GetMethodHandler {

	public ExistsHandler(ConnectorManager connManager, ObjectSerializer serializer, RequestBodyParser bodyParser) {
		super(connManager, serializer, bodyParser);
	}

	@Override
	public Response handles(MultivaluedMap<String, String> queryParams, HttpHeaders headers) {
		String id = (String) getParam("id", queryParams, true);
		DataSource ds = null;
		try {
			ds = connManager.getDataSource(new DataSourceId(id));
		} catch (Exception e) {
			throw ErrorUtils.statusExp(e);
		}
		if (ds == null) {
			return buildResponse(Status.OK, false);
		}
		return buildResponse(Status.OK, true);
	}

}
