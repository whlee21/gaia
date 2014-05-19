package gaia.commons.server.api.services;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class RequestBody {

	/**
	 * The associated query.
	 */
	private String m_query;

	/**
	 * The associated partial response fields.
	 */
	private String m_fields;

	/**
	 * The body properties.
	 */
	private Set<NamedPropertySet> m_propertySets = new HashSet<NamedPropertySet>();

	/**
	 * The request body. Query and partial response data is stripped before
	 * setting.
	 */
	private String m_body;

	/**
	 * Request properties.
	 */
	private Map<String, Object> m_requestInfoProps = new HashMap<String, Object>();

	/**
	 * Set the query string.
	 * 
	 * @param query
	 *            the query string from the body
	 */
	public void setQueryString(String query) {
		m_query = query;
	}

	/**
	 * Obtain that query that was specified in the body.
	 * 
	 * @return the query from the body or null if no query was present in the
	 *         body
	 */
	public String getQueryString() {
		return m_query;
	}

	/**
	 * Set the partial response fields from the body.
	 * 
	 * @param fields
	 *            the partial response fields
	 */
	public void setPartialResponseFields(String fields) {
		m_fields = fields;
	}

	/**
	 * Obtain the partial response fields that were specified in the body.
	 * 
	 * @return the partial response fields or null if not specified in the body
	 */
	public String getPartialResponseFields() {
		return m_fields;
	}

	/**
	 * Add a property set. A property set is a set of related properties and
	 * values. For example, if the body contained properties for three different
	 * resources to be created, each would be represented as distinct property
	 * set.
	 * 
	 * @param propertySet
	 *            the property set to add
	 */
	public void addPropertySet(NamedPropertySet propertySet) {
		m_propertySets.add(propertySet);
	}

	/**
	 * Obtain all property sets or an empty set if no properties were specified
	 * in the body.
	 * 
	 * @return all property sets or an empty set
	 */
	public Set<NamedPropertySet> getNamedPropertySets() {
		return m_propertySets;
	}

	/**
	 * Obtain all property sets as a set of maps. Convenience method that
	 * converts a Set<NamedPropertySet> to a Set<Map<String, Object>>.
	 * 
	 * @return a set of property maps for the request
	 */
	public Set<Map<String, Object>> getPropertySets() {
		Set<NamedPropertySet> setNamedProps = getNamedPropertySets();
		Set<Map<String, Object>> setProps = new HashSet<Map<String, Object>>(
				setNamedProps.size());

		for (NamedPropertySet namedProps : setNamedProps) {
			setProps.add(namedProps.getProperties());
		}

		return setProps;
	}

	/**
	 * Return a map of request info properties. These properties are meta-data
	 * for the request.
	 */
	public Map<String, Object> getRequestInfoProperties() {
		return m_requestInfoProps;
	}

	/**
	 * Add a request info property.
	 */
	public void addRequestInfoProperty(String key, Object val) {
		m_requestInfoProps.put(key, val);
	}

	/**
	 * Set the body from the request.
	 * 
	 * @param body
	 *            the request body
	 */
	public void setBody(String body) {
		if (body != null && !body.isEmpty()) {
			m_body = body;
		}
	}

	/**
	 * Obtain the request body.
	 * 
	 * @return the request body
	 */
	public String getBody() {
		return m_body;
	}
}
