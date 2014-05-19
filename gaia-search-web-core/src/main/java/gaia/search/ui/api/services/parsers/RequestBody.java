package gaia.search.ui.api.services.parsers;

import java.util.HashMap;
import java.util.Map;

public class RequestBody {

	/**
	 * The associated query.
	 */
	private String m_query;

	/**
	 * The associated partial response fields.
	 */
	// private String m_fields;

	/**
	 * The body properties.
	 */
	// private Set<NamedPropertySet> m_propertySets = new
	// HashSet<NamedPropertySet>();

	/**
	 * The request body. Query and partial response data is stripped before
	 * setting.
	 */
	private String m_body;

	/**
	 * Request properties.
	 */
	// private Map<String, Object> m_requestInfoProps = new HashMap<String,
	// Object>();
	private Map<String, Object> m_properties;// = new HashMap<String, Object>();

	/**
	 * Set the query string.
	 * 
	 * @param query
	 *          the query string from the body
	 */
	public void setQueryString(String query) {
		m_query = query;
	}

	/**
	 * Obtain that query that was specified in the body.
	 * 
	 * @return the query from the body or null if no query was present in the body
	 */
	public String getQueryString() {
		return m_query;
	}

	public Map<String, Object> getProperties() {
		return m_properties;
	}

	public void setProperties(Map<String, Object> properties) {
		m_properties = properties;
	}

	/**
	 * Set the body from the request.
	 * 
	 * @param body
	 *          the request body
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
