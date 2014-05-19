package gaia.commons.server.api.resources;

import gaia.commons.server.api.query.Query;
import gaia.commons.server.controller.spi.Resource;

import java.util.Map;

public interface ResourceInstance {

	/**
	 * Set the values of the parent foreign keys.
	 * 
	 * @param mapIds
	 *            map of all parent foreign keys. Map from resource type to id
	 *            value.
	 */
	public void setIds(Map<Resource.Type, String> mapIds);

	/**
	 * Obtain the primary and foreign key properties for the resource.
	 * 
	 * @return map of primary and foreign key values keyed by resource type
	 */
	public Map<Resource.Type, String> getIds();

	/**
	 * Return the query associated with the resource. Each resource has one
	 * query.
	 * 
	 * @return the associated query
	 */
	public Query getQuery();

	/**
	 * Return the resource definition for this resource type. All information in
	 * the definition is static and is specific to the resource type, not the
	 * resource instance.
	 * 
	 * @return the associated resource definition
	 */
	public ResourceDefinition getResourceDefinition();

	/**
	 * Return all sub-resource instances. This will include all children of this
	 * resource as well as any other resources referred to via a foreign key
	 * property.
	 * 
	 * @return all sub-resource instances
	 */
	public Map<String, ResourceInstance> getSubResources();

	/**
	 * Determine if resource is a collection resource.
	 * 
	 * @return true if the resource is a collection resource; false otherwise
	 */
	public boolean isCollectionResource();
}
