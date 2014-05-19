package gaia.crawl.connector.api.services;

import gaia.api.ObjectSerializer;
import gaia.crawl.ConnectorManager;
import gaia.crawl.connector.api.services.parsers.BodyParseException;
import gaia.crawl.connector.api.services.parsers.RequestBodyParser;

import java.net.URI;
import java.util.Map;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.restlet.resource.ResourceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BaseHandler {

	private static final Logger LOG = LoggerFactory.getLogger(BaseHandler.class);
	
	protected ConnectorManager connManager;

	protected ObjectSerializer serializer;

	protected RequestBodyParser bodyParser;

	public BaseHandler(ConnectorManager connManager, ObjectSerializer serializer, RequestBodyParser bodyParser) {
		this.connManager = connManager;
		this.serializer = serializer;
		this.bodyParser = bodyParser;
	}

	protected Response buildResponse(Response.Status status) {
		return Response.status(status).build();
	}

	protected Response buildResponse(Response.Status status, Object result) {
		return Response.status(status).entity(serializer.serialize(result)).build();
	}

	protected Response buildResponse(Response.Status status, URI seeOther, Object result) {
		if (seeOther == null)
			return Response.status(status).entity(serializer.serialize(result)).build();
		else
			return Response.seeOther(seeOther).status(status).entity(serializer.serialize(result)).build();
	}

	protected RequestBody getRequestBody(String body) throws BodyParseException {
		return bodyParser.parse(body);
	}

	protected Object getParam(String name, MultivaluedMap<String, String> queryParams, boolean require) throws ResourceException {
		return getParam(name, queryParams, null, require);
	}
	
	protected Object getParam(String name, MultivaluedMap<String, String> queryParams, Map<String, Object> params,
			boolean require) throws WebApplicationException {
		Object param = null;
		if (queryParams != null) {
			param = queryParams.getFirst(name);
		}
		if ((param == null || "".equals(param)) && (params != null)) {
			param = params.get(name);
		}
		if ((param == null || "".equals(param)) && (require)) {
			throw new WebApplicationException(new Exception("'" + name + "' attribute required"), 422);
		}
		return param;
	}
}
