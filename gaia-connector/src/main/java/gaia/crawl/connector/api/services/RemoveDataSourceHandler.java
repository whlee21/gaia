package gaia.crawl.connector.api.services;

import gaia.api.ErrorUtils;
import gaia.api.ObjectSerializer;
import gaia.crawl.ConnectorManager;
import gaia.crawl.connector.api.services.parsers.RequestBodyParser;
import gaia.crawl.datasource.DataSourceId;
import gaia.utils.StringUtils;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

public class RemoveDataSourceHandler extends BaseHandler implements DeleteMethodHandler {

	public RemoveDataSourceHandler(ConnectorManager connManager, ObjectSerializer serializer, RequestBodyParser bodyParser) {
		super(connManager, serializer, bodyParser);
	}

	@Override
	public Response handles(MultivaluedMap<String, String> queryParams, HttpHeaders headers) {
		try {
			String id = (String) getParam("id", queryParams, true);
			boolean force = StringUtils.getBoolean(getParam("force", queryParams, false), true).booleanValue();
			return buildResponse(Status.OK, connManager.removeDataSource(new DataSourceId(id), force));
		} catch (Exception e) {
			throw ErrorUtils.statusExp(e);
		}
	}

}
