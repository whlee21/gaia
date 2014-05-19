package gaia.api;

import gaia.admin.collection.CollectionManager;
import gaia.utils.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.core.Response;

import org.apache.solr.core.CoreContainer;
import org.apache.solr.schema.FieldSchemaExposer;
import org.apache.solr.schema.FieldType;
import org.apache.solr.schema.IndexSchema;
import org.restlet.resource.Get;
import org.restlet.resource.ResourceException;

public abstract class AbstractFieldServerResource extends AbstractFieldsResource implements AbstractFieldResource {
	protected String name;
	static final String[] propertyNames = FieldSchemaExposer.getPropertyNames();
	static final Map<String, Integer> propertyMap = FieldSchemaExposer.getPropertyMap();

	protected AbstractFieldServerResource(CollectionManager cm, CoreContainer cores) {
		super(cm, cores);
	}

	public void doInit() throws ResourceException {
		super.doInit();

		name = ((String) getRequest().getAttributes().get("name"));
	}

	public void doRelease() {
		if (solrCore != null)
			solrCore.close();
	}

	@Get("json")
	public Map<String, Object> retrieve() {
		if (!isExisting()) {
			throw ErrorUtils.statusExp(Response.Status.NOT_FOUND, "URI not found");
		}
		Map<String, Object> attributes = null;

		attributes = getAttributeReader().getAttributes(name);

		if ((attributes == null) || (attributes.size() == 0)) {
			throw ErrorUtils.statusExp(Response.Status.NOT_FOUND, getEntityLabel() + ": " + name
					+ " could not be found in Schema");
		}

		return attributes;
	}

	protected static List<Error> checkForValidFields(Map<String, Object> params, Set<String> validKeys) {
		List<Error> errors = new ArrayList<Error>();
		Set<String> keys = params.keySet();
		for (String key : keys) {
			if (!validKeys.contains(key)) {
				errors.add(new Error(key, Error.E_FORBIDDEN_KEY, "Unknown or dissallowed key found:" + key));
			}
		}
		return errors;
	}

	static String getString(Map<String, Object> params, String key) {
		Object value = params.get(key);

		return value == null ? null : value.toString();
	}

	protected static int parseProperties(String name, FieldType fieldType, Map<String, Object> params) {
		Map<String, String> propMap = new HashMap<String, String>();
		for (String propertyName : propertyNames) {
			String paramName;
			if (propertyName.equals("termVectors")) {
				paramName = AbstractFieldAttributeReader.TERM_VECTORS;
			} else {
				if (propertyName.equals("omitTermFreqAndPositions")) {
					paramName = AbstractFieldAttributeReader.OMIT_TF;
				} else {
					if (propertyName.equals("omitPositions")) {
						paramName = AbstractFieldAttributeReader.OMIT_POSITIONS;
					} else {
						if (propertyName.equals("multiValued"))
							paramName = AbstractFieldAttributeReader.MULTI_VALUED;
						else
							paramName = propertyName;
					}
				}
			}
			if (params.containsKey(paramName)) {
				Boolean enabled = StringUtils.getBoolean(params.get(paramName));
				if (enabled != null) {
					propMap.put(propertyName, enabled.toString());
				}
			}
		}
		return FieldSchemaExposer.calcProps(name, fieldType, propMap);
	}

	public static String getNameProp(Map<String, Object> m) {
		String name = getString(m, "name");

		if ((name == null) || (name.trim().length() == 0)) {
			throw ErrorUtils.statusExp(422, new Error("name", Error.E_MISSING_VALUE, "name must be specified"));
		}

		return name;
	}

	public static FieldType getFieldTypeProp(IndexSchema schema, Map<String, Object> m) {
		String type = getString(m, AbstractFieldAttributeReader.TYPE_NAME);
		if (type == null) {
			throw ErrorUtils.statusExp(422, new Error(AbstractFieldAttributeReader.TYPE_NAME, Error.E_MISSING_VALUE,
					"You must specify a field_type for: " + getString(m, "name")));
		}

		FieldType fieldType = (FieldType) schema.getFieldTypes().get(type);
		if (fieldType == null) {
			throw ErrorUtils.statusExp(422, new Error(AbstractFieldAttributeReader.TYPE_NAME, Error.E_INVALID_VALUE,
					"field_type is not valid:" + type));
		}

		return fieldType;
	}
}
