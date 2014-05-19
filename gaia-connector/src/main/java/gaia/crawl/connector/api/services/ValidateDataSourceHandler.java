package gaia.crawl.connector.api.services;

import gaia.api.ErrorUtils;
import gaia.api.ObjectSerializer;
import gaia.crawl.ConnectorManager;
import gaia.crawl.connector.api.services.parsers.RequestBodyParser;
import gaia.crawl.datasource.DataSource;
import gaia.utils.StringUtils;

import java.util.Map;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

public class ValidateDataSourceHandler extends BaseHandler implements PutMethodHandler {

	public ValidateDataSourceHandler(ConnectorManager connManager, ObjectSerializer serializer,
			RequestBodyParser bodyParser) {
		super(connManager, serializer, bodyParser);
	}

	@Override
	public Response handles(MultivaluedMap<String, String> queryParams, HttpHeaders headers, String body) {
		try {
			RequestBody requestBody = bodyParser.parse(body);
			Map<String, Object> m = requestBody.getProperties();
			DataSource ds = DataSource.fromMap(m);
			boolean remove = StringUtils.getBoolean((String) getParam("remove", queryParams, m, false), true).booleanValue();
			boolean verifyAccess = StringUtils.getBoolean((String) getParam("verify_access", queryParams, m, false), true)
					.booleanValue();
			return buildResponse(Status.OK, connManager.validateDataSource(ds, remove, verifyAccess));
		} catch (Exception e) {
			throw ErrorUtils.statusExp(e);
		}
	}

}
