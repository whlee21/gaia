package gaia.crawl.connector.api.services;

import gaia.api.ErrorUtils;
import gaia.api.ObjectSerializer;
import gaia.crawl.ConnectorManager;
import gaia.crawl.connector.api.services.parsers.RequestBodyParser;
import gaia.crawl.datasource.DataSource;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

public class GetDataSourcesHandler extends BaseHandler implements GetMethodHandler {

	public GetDataSourcesHandler(ConnectorManager connManager, ObjectSerializer serializer, RequestBodyParser bodyParser) {
		super(connManager, serializer, bodyParser);
	}

	@Override
	public Response handles(MultivaluedMap<String, String> queryParams, HttpHeaders headers) {
		String collection = (String) getParam("collection", queryParams, false);
		List<DataSource> lst = null;
		try {
			lst = connManager.getDataSources(collection);
		} catch (Exception e) {
			throw ErrorUtils.statusExp(e);
		}
		List<Map<String, Object>> res = new ArrayList<Map<String, Object>>();
		for (DataSource ds : lst) {
			res.add(ds.toMap());
		}
		return buildResponse(Status.OK, res);
	}

}
