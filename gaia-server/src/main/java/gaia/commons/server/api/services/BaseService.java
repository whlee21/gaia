package gaia.commons.server.api.services;

import gaia.commons.server.Error;
import gaia.commons.server.api.resources.ResourceInstance;
import gaia.commons.server.api.resources.ResourceInstanceFactory;
import gaia.commons.server.api.services.parsers.BodyParseException;
import gaia.commons.server.api.services.parsers.JsonRequestBodyParser;
import gaia.commons.server.api.services.parsers.RequestBodyParser;
import gaia.commons.server.api.services.serializers.JsonSerializer;
import gaia.commons.server.api.services.serializers.ResultSerializer;
import gaia.commons.server.controller.spi.Resource;

import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class BaseService {

	protected final static Logger LOG = LoggerFactory.getLogger(BaseService.class);

	protected ResourceInstanceFactory m_resourceFactory;

	private ResultSerializer m_serializer = new JsonSerializer();

	public BaseService(ResourceInstanceFactory resourceFactory) {
		this.m_resourceFactory = resourceFactory;
	}

	/**
	 * All requests are funneled through this method so that common logic can be
	 * executed. Creates a request instance and invokes it's process method.
	 * 
	 * @param headers
	 *          http headers
	 * @param body
	 *          http body
	 * @param uriInfo
	 *          uri information
	 * @param requestType
	 *          http request type
	 * @param resource
	 *          resource instance that is being acted on
	 * 
	 * @return the response of the operation in serialized form
	 */
	protected Response handleRequest(HttpHeaders headers, String body, UriInfo uriInfo, Request.Type requestType,
			ResourceInstance resource) {
		Result result = new ResultImpl(new ResultStatus(ResultStatus.STATUS.OK));
		try {
			Set<RequestBody> requestBodySet = getBodyParser().parse(body);
			Iterator<RequestBody> iterator = requestBodySet.iterator();
			while (iterator.hasNext() && result.getStatus().getStatus().equals(ResultStatus.STATUS.OK)) {
				RequestBody requestBody = iterator.next();
				Request request = getRequestFactory().createRequest(headers, requestBody, uriInfo, requestType, resource);
				result = request.process();
			}
		} catch (BodyParseException e) {
			result = new ResultImpl(new ResultStatus(ResultStatus.STATUS.BAD_REQUEST, new Error("request.body",
					Error.E_INVALID_VALUE, e.getMessage())));
		}
		
		return Response.status(result.getStatus().getStatusCode()).entity(getResultSerializer().serialize(result)).build();
	}

	/**
	 * Obtain the factory from which to create Request instances.
	 * 
	 * @return the Request factory
	 */
	protected RequestFactory getRequestFactory() {
		return new RequestFactory();
	}

	protected ResourceInstance createResource(Resource.Type type, Map<Resource.Type, String> mapIds) {
		return m_resourceFactory.createResource(type, mapIds);
	}

	protected ResultSerializer getResultSerializer() {
		return m_serializer;
	}

	protected RequestBodyParser getBodyParser() {
		return new JsonRequestBodyParser();
	}
}
