package gaia.commons.server.api.services;

import gaia.commons.server.api.resources.ResourceDefinition;
import gaia.commons.server.api.resources.ResourceInstance;
import gaia.commons.server.controller.spi.Predicate;
import gaia.commons.server.controller.spi.TemporalInfo;

import java.util.List;
import java.util.Map;

public interface Request {

	/**
	 * Enum of request types.
	 */
	public enum Type {
		GET, POST, PUT, DELETE, QUERY_POST
	}

	/**
	 * Process the request.
	 * 
	 * @return the result
	 */
	public Result process();

	/**
	 * Obtain the resource definition which corresponds to the resource being
	 * operated on by the request. The resource definition provides information
	 * about the resource type;
	 * 
	 * @return the associated {@link ResourceDefinition}
	 */
	public ResourceInstance getResource();

	/**
	 * Obtain the URI of this request.
	 * 
	 * @return the request uri
	 */
	public String getURI();

	/**
	 * Obtain the http request type. Type is one of {@link Type}.
	 * 
	 * @return the http request type
	 */
	public Type getRequestType();

	/**
	 * Obtain the api version of the request. The api version is specified in
	 * the request URI.
	 * 
	 * @return the api version of the request
	 */
	public int getAPIVersion();

	/**
	 * Obtain the query predicate that was built from the user provided
	 * predicate fields in the query string. If multiple predicates are
	 * supplied, then they will be combined using the appropriate logical
	 * grouping predicate such as 'AND'.
	 * 
	 * @return the user defined predicate
	 */
	public Predicate getQueryPredicate();

	/**
	 * Obtain the partial response fields and associated temporal information
	 * which were provided in the query string of the request uri.
	 * 
	 * @return map of partial response propertyId to temporal information
	 */
	public Map<String, TemporalInfo> getFields();

	/**
	 * Obtain the request body data.
	 */
	public RequestBody getBody();

	/**
	 * Obtain the http headers associated with the request.
	 * 
	 * @return the http headers
	 */
	public Map<String, List<String>> getHttpHeaders();
}
