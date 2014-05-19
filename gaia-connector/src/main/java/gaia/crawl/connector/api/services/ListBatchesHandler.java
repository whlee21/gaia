package gaia.crawl.connector.api.services;

import gaia.api.ErrorUtils;
import gaia.api.ObjectSerializer;
import gaia.crawl.ConnectorManager;
import gaia.crawl.batch.BatchStatus;
import gaia.crawl.connector.api.services.parsers.RequestBodyParser;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

public class ListBatchesHandler extends BaseHandler implements GetMethodHandler {

	public ListBatchesHandler(ConnectorManager connManager, ObjectSerializer serializer, RequestBodyParser bodyParser) {
		super(connManager, serializer, bodyParser);
	}

	@Override
	public Response handles(MultivaluedMap<String, String> queryParams, HttpHeaders headers) {
		String crawler = (String) getParam("crawler", queryParams, false);
		String collection = (String) getParam("collection", queryParams, false);
		String id = (String) getParam("id", queryParams, false);
		List<BatchStatus> lst = null;
		try {
			lst = connManager.listBatches(crawler, collection, id);
		} catch (Exception e) {
			throw ErrorUtils.statusExp(e);
		}
		List<Map<String, Object>> res = new ArrayList<Map<String, Object>>();
		for (BatchStatus bs : lst) {
			res.add(bs.toMap());
		}
		return buildResponse(Status.OK, res);
	}

}
