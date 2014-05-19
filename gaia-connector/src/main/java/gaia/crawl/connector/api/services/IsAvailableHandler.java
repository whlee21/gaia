package gaia.crawl.connector.api.services;

import gaia.api.ErrorUtils;
import gaia.api.ObjectSerializer;
import gaia.crawl.ConnectorManager;
import gaia.crawl.RestConnectorManager;
import gaia.crawl.connector.api.services.parsers.RequestBodyParser;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IsAvailableHandler extends BaseHandler implements GetMethodHandler {

	private static final Logger LOG = LoggerFactory.getLogger(IsAvailableHandler.class);
	
	public IsAvailableHandler(ConnectorManager connManager, ObjectSerializer serializer, RequestBodyParser bodyParser) {
		super(connManager, serializer, bodyParser);
	}

	@Override
	public Response handles(MultivaluedMap<String, String> queryParams, HttpHeaders headers) {
		try {
			String crawlerType = (String) getParam("crawler", queryParams, true);
			return buildResponse(Status.OK, connManager.isAvailable(crawlerType));
		} catch (Exception e) {
			throw ErrorUtils.statusExp(e);
		}
	}
}
