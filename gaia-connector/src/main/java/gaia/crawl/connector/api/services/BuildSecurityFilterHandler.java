package gaia.crawl.connector.api.services;

import gaia.api.ErrorUtils;
import gaia.api.ObjectSerializer;
import gaia.crawl.ConnectorManager;
import gaia.crawl.connector.api.services.parsers.RequestBodyParser;
import gaia.crawl.datasource.DataSourceId;
import gaia.crawl.security.SecurityFilter;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

public class BuildSecurityFilterHandler extends BaseHandler implements GetMethodHandler {

	public BuildSecurityFilterHandler(ConnectorManager connManager, ObjectSerializer serializer,
			RequestBodyParser bodyParser) {
		super(connManager, serializer, bodyParser);
	}

	@Override
	public Response handles(MultivaluedMap<String, String> queryParams, HttpHeaders headers) {
		String user = (String) getParam("user", queryParams, true);
		String id = (String) getParam("id", queryParams, true);
		DataSourceId dsId = new DataSourceId(id);
		try {
			SecurityFilter filter = connManager.buildSecurityFilter(dsId, user);
			if (filter != null) {
				return buildResponse(Status.OK, filter.toMap());
			}
			return null;
		} catch (Exception e) {
			throw ErrorUtils.statusExp(e);
		}
	}
}
