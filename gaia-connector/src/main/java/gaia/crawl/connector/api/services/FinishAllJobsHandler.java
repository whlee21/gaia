package gaia.crawl.connector.api.services;

import gaia.api.ErrorUtils;
import gaia.api.ObjectSerializer;
import gaia.crawl.ConnectorManager;
import gaia.crawl.connector.api.services.parsers.RequestBodyParser;
import gaia.utils.StringUtils;

import java.util.Map;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FinishAllJobsHandler extends BaseHandler implements PutMethodHandler {

	private static final Logger LOG = LoggerFactory.getLogger(FinishAllJobsHandler.class);

	public FinishAllJobsHandler(ConnectorManager connManager, ObjectSerializer serializer, RequestBodyParser bodyParser) {
		super(connManager, serializer, bodyParser);
	}

	@Override
	public Response handles(MultivaluedMap<String, String> queryParams, HttpHeaders headers, String body) {
		try {
			RequestBody requestBody = bodyParser.parse(body);
			Map<String, Object> m = requestBody.getProperties();
			String collection = (String) getParam("collection", queryParams, m, false);
			String crawler = (String) getParam("crawler", queryParams, m, false);
			boolean abort = StringUtils.getBoolean(getParam("abort", queryParams, m, false), true).booleanValue();
			Object o = getParam("waitTime", queryParams, m, false);
			long waitTime = -1L;
			if (o != null) {
				try {
					waitTime = Long.parseLong(o.toString());
				} catch (Exception e) {
					LOG.warn("Invalid waitTime, using -1: " + e.toString());
				}
			}
			return buildResponse(Response.Status.OK, connManager.finishAllJobs(crawler, collection, abort, waitTime));
		} catch (Exception e) {
			throw ErrorUtils.statusExp(e);
		}
	}

}
