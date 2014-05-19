package gaia.commons.server.api.services;

import gaia.commons.server.api.resources.ResourceInstance;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.UriInfo;

public class RequestFactory {

	/**
	 * Create a request instance.
	 * 
	 * @param headers
	 *            http headers
	 * @param uriInfo
	 *            uri information
	 * @param requestType
	 *            http request type
	 * @param resource
	 *            associated resource instance
	 * 
	 * @return a new Request instance
	 */
	public Request createRequest(HttpHeaders headers, RequestBody body,
			UriInfo uriInfo, Request.Type requestType, ResourceInstance resource) {
		switch (requestType) {
		case GET:
			return new GetRequest(headers, body, uriInfo, resource);
		case PUT:
			return new PutRequest(headers, body, uriInfo, resource);
		case DELETE:
			return new DeleteRequest(headers, body, uriInfo, resource);
		case POST:
			return ((uriInfo.getQueryParameters().isEmpty() && body
					.getQueryString() == null) || body == null) ? new PostRequest(
					headers, body, uriInfo, resource) : new QueryPostRequest(
					headers, body, uriInfo, resource);
		default:
			throw new IllegalArgumentException("Invalid request type: "
					+ requestType);
		}
	}

}
