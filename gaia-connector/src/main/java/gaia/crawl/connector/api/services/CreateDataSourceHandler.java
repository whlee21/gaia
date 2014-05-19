package gaia.crawl.connector.api.services;

import gaia.api.Error;
import gaia.api.ErrorUtils;
import gaia.api.ObjectSerializer;
import gaia.crawl.ConnectorManager;
import gaia.crawl.DataSourceFactoryException;
import gaia.crawl.connector.api.services.parsers.RequestBodyParser;
import gaia.crawl.datasource.DataSource;

import java.util.List;
import java.util.Map;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CreateDataSourceHandler extends BaseHandler implements PostMethodHandler {

	private static final Logger LOG = LoggerFactory.getLogger(CreateDataSourceHandler.class);
	
	public CreateDataSourceHandler(ConnectorManager connManager, ObjectSerializer serializer, RequestBodyParser bodyParser) {
		super(connManager, serializer, bodyParser);
	}

	@Override
	public Response handles(MultivaluedMap<String, String> queryParams, HttpHeaders headers, String body) {
		try {
			RequestBody requestBody = bodyParser.parse(body);
			Map<String, Object> m = requestBody.getProperties();
			DataSource ds = connManager.createDataSource(m);
			return buildResponse(Response.Status.OK, ds.toMap());
		} catch (DataSourceFactoryException dse) {
			List<Error> errors = dse.getErrors();
			if ((errors != null) && (!errors.isEmpty())) {
				throw ErrorUtils.statusExp(422, errors);
			}
			throw ErrorUtils.statusExp(dse);
		} catch (Exception e) {
			throw ErrorUtils.statusExp(e);
		}
	}
}
