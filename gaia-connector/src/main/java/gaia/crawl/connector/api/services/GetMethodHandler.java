package gaia.crawl.connector.api.services;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

public interface GetMethodHandler {
	public Response handles(MultivaluedMap<String, String> queryParams, HttpHeaders headers);
}
