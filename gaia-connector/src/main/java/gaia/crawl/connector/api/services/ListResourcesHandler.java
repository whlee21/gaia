package gaia.crawl.connector.api.services;

import gaia.api.ErrorUtils;
import gaia.api.ObjectSerializer;
import gaia.crawl.ConnectorManager;
import gaia.crawl.connector.api.services.parsers.RequestBodyParser;
import gaia.crawl.datasource.DataSourceId;
import gaia.crawl.resource.Resource;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

public class ListResourcesHandler extends BaseHandler implements GetMethodHandler {

	public ListResourcesHandler(ConnectorManager connManager, ObjectSerializer serializer, RequestBodyParser bodyParser) {
		super(connManager, serializer, bodyParser);
	}

	@Override
	public Response handles(MultivaluedMap<String, String> queryParams, HttpHeaders headers) {
		String crawler = (String) getParam("crawler", queryParams, true);
		String collection = (String) getParam("collection", queryParams, false);
		String dsId = (String) getParam("id", queryParams, false);
		DataSourceId ds = null;
		if (dsId != null)
			ds = new DataSourceId(dsId);
		List<Resource> resources = null;
		try {
			resources = connManager.listResources(crawler, collection, ds);
		} catch (Exception e) {
			throw ErrorUtils.statusExp(e);
		}
		List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
		for (Resource resource : resources) {
			result.add(resource.toMap());
		}
		return buildResponse(Status.OK, result);
	}

}
