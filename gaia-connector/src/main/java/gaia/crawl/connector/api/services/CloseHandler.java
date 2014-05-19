package gaia.crawl.connector.api.services;

import gaia.api.ErrorUtils;
import gaia.api.ObjectSerializer;
import gaia.crawl.ConnectorManager;
import gaia.crawl.CrawlId;
import gaia.crawl.connector.api.services.parsers.RequestBodyParser;
import gaia.utils.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

public class CloseHandler extends BaseHandler implements PutMethodHandler {

	public CloseHandler(ConnectorManager connManager, ObjectSerializer serializer, RequestBodyParser bodyParser) {
		super(connManager, serializer, bodyParser);
	}

	@Override
	public Response handles(MultivaluedMap<String, String> queryParams, HttpHeaders headers, String body) {
		try {
			RequestBody requestBody = bodyParser.parse(body);
			Map<String, Object> m = requestBody.getProperties();
			String collection = (String) getParam("collection", queryParams, m, false);
			String crawler = (String) getParam("crawler", queryParams, m, false);
			boolean dryRun = StringUtils.getBoolean((String) getParam("dryRun", queryParams, m, false), true).booleanValue();
			List<CrawlId> lst = connManager.close(crawler, collection, dryRun);
			List<String> res = new ArrayList<String>();
			for (CrawlId cid : lst) {
				res.add(cid.toString());
			}
			return buildResponse(Response.Status.OK, res);
		} catch (Exception e) {
			throw ErrorUtils.statusExp(e);
		}
	}

}