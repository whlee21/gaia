package gaia.search.ui.api;

import gaia.api.ObjectSerializer;
import gaia.search.ui.api.services.parsers.BodyParseException;
import gaia.search.ui.api.services.parsers.RequestBody;
import gaia.search.ui.api.services.parsers.RequestBodyParser;

import java.net.URI;

import javax.ws.rs.core.Response;

public class BaseService {

	protected ObjectSerializer serializer;

	protected RequestBodyParser bodyParser;

	public BaseService(ObjectSerializer serializer, RequestBodyParser bodyParser) {
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
}
