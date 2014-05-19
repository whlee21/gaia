package gaia.crawl.connector.api.services;

import gaia.api.Error;
import gaia.api.ErrorUtils;
import gaia.api.ObjectSerializer;
import gaia.crawl.ConnectorManager;
import gaia.crawl.CrawlId;
import gaia.crawl.JobStateException;
import gaia.crawl.connector.api.services.parsers.RequestBodyParser;
import gaia.utils.StringUtils;

import java.util.Map;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StopJobHandler extends BaseHandler implements PutMethodHandler {

	private static final Logger LOG = LoggerFactory.getLogger(StopJobHandler.class);

	public StopJobHandler(ConnectorManager connManager, ObjectSerializer serializer, RequestBodyParser bodyParser) {
		super(connManager, serializer, bodyParser);
	}

	@Override
	public Response handles(MultivaluedMap<String, String> queryParams, HttpHeaders headers, String body) {
		String id = null;
		try {
			RequestBody requestBody = bodyParser.parse(body);
			Map<String, Object> m = requestBody.getProperties();
			String crawler = (String) getParam("crawler", queryParams, m, true);
			id = (String) getParam("id", queryParams, m, true);
			boolean abort = StringUtils.getBoolean((String) getParam("abort", queryParams, m, false), true).booleanValue();
			Object o = getParam("waitTime", queryParams, m, false);
			long waitTime = -1L;
			if (o != null)
				try {
					waitTime = Long.parseLong(o.toString());
				} catch (Exception e) {
					LOG.warn("Invalid waitTime, using -1: " + e.toString());
				}
			return buildResponse(Response.Status.OK, connManager.stopJob(crawler, new CrawlId(id), abort, waitTime));
		} catch (JobStateException jse) {
			throw ErrorUtils.statusExp(422, new Error(id, Error.E_INVALID_OPERATION, jse.toString()));
		} catch (Exception e) {
			throw ErrorUtils.statusExp(e);
		}
	}

}
