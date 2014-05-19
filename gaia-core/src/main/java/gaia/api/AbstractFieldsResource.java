package gaia.api;

import gaia.admin.collection.CollectionManager;
import gaia.utils.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.solr.core.CoreContainer;
import org.apache.solr.core.SolrCore;
import org.apache.solr.schema.FieldSchemaExposer;
import org.apache.solr.schema.FieldType;
import org.apache.solr.schema.IndexSchema;
import org.restlet.resource.ResourceException;
import org.restlet.resource.ServerResource;

public abstract class AbstractFieldsResource extends ServerResource {
	public static final String INCLUDE_DYNAMIC_PARAM = "include_dynamic";
	static final Set<String> tmp;
	static final Set<String> COMMON_KEYS;
	protected CoreContainer cores;
	protected CollectionManager cm;
	protected String collection;
	protected SolrCore solrCore;
	static final String[] propertyNames = FieldSchemaExposer.getPropertyNames();
	static final Map<String, Integer> propertyMap = FieldSchemaExposer.getPropertyMap();

	protected AbstractFieldsResource(CollectionManager cm, CoreContainer cores) {
		this.cores = cores;
		this.cm = cm;
	}

	public abstract AbstractFieldAttributeReader getAttributeReader();

	public abstract String getEntityLabel();

	public void doInit() throws ResourceException {
		collection = ((String) getRequestAttributes().get("coll_name"));
		solrCore = cores.getCore(collection);
		setExisting(solrCore != null);
	}

	public void doRelease() {
		if (solrCore != null)
			solrCore.close();
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

	protected static void validateCommonParams(Map<String, Object> params, AddUpdateMode mode, List<Error> errors) {
		if (AddUpdateMode.REMOVE == mode)
			return;

		Boolean indexed = StringUtils.getBoolean(params.get(AbstractFieldAttributeReader.INDEXED), false);

		Boolean omitTf = StringUtils.getBoolean(params.get(AbstractFieldAttributeReader.OMIT_TF), false);

		Boolean omitPositions = StringUtils.getBoolean(params.get(AbstractFieldAttributeReader.OMIT_POSITIONS), false);

		Boolean vectors = StringUtils.getBoolean(params.get(AbstractFieldAttributeReader.TERM_VECTORS), false);

		if (null == indexed) {
			errors.add(new Error(AbstractFieldAttributeReader.INDEXED, Error.E_MISSING_VALUE,
					"indexed was not specified, or determined from the fieldType"));
		} else if (!indexed.booleanValue()) {
			if ((null != omitTf) && (!omitTf.booleanValue())) {
				errors.add(new Error(AbstractFieldAttributeReader.OMIT_TF, Error.E_INVALID_VALUE,
						"omit_tf cannot be false if indexed is false"));
			}
			if ((null != omitPositions) && (!omitPositions.booleanValue())) {
				errors.add(new Error(AbstractFieldAttributeReader.OMIT_POSITIONS, Error.E_INVALID_VALUE,
						"omit_positions cannot be false if indexed is false"));
			}
			if ((null != vectors) && (vectors.booleanValue())) {
				errors.add(new Error(AbstractFieldAttributeReader.TERM_VECTORS, Error.E_INVALID_VALUE,
						"term_vectors cannot be true if indexed is false"));
			}
		}

		if (null == omitTf) {
			errors.add(new Error(AbstractFieldAttributeReader.OMIT_TF, Error.E_MISSING_VALUE,
					"omit_tf was not specified, or determined from the fieldType"));
		}
		if (null == omitPositions) {
			errors.add(new Error(AbstractFieldAttributeReader.OMIT_POSITIONS, Error.E_MISSING_VALUE,
					"omit_positions was not specified, or determined from the fieldType"));
		}

		if ((null != omitTf) && (null != omitPositions) && (omitTf.booleanValue()) && (!omitPositions.booleanValue())) {
			errors.add(new Error(AbstractFieldAttributeReader.OMIT_POSITIONS, Error.E_INVALID_VALUE,
					"omit_positions cannot be false if omit_tf is true"));
		}
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
		String name = getString(m, AbstractFieldAttributeReader.NAME);

		if ((name == null) || (name.trim().length() == 0)) {
			throw ErrorUtils.statusExp(422, new Error(AbstractFieldAttributeReader.NAME, Error.E_MISSING_VALUE,
					"name must be specified"));
		}

		return name;
	}

	public static FieldType getFieldTypeProp(IndexSchema schema, Map<String, Object> m) {
		String type = getString(m, AbstractFieldAttributeReader.TYPE_NAME);
		if (type == null) {
			throw ErrorUtils.statusExp(422, new Error(AbstractFieldAttributeReader.TYPE_NAME, Error.E_MISSING_VALUE,
					"You must specify a field_type for: " + getString(m, AbstractFieldAttributeReader.NAME)));
		}

		FieldType fieldType = (FieldType) schema.getFieldTypes().get(type);
		if (fieldType == null) {
			throw ErrorUtils.statusExp(422, new Error(AbstractFieldAttributeReader.TYPE_NAME, Error.E_INVALID_VALUE,
					"field_type is not valid:" + type));
		}

		return fieldType;
	}

	static {
		tmp = new HashSet<String>();
		tmp.add(AbstractFieldAttributeReader.COPY_FIELDS);
		tmp.add(AbstractFieldAttributeReader.NAME);
		tmp.add(AbstractFieldAttributeReader.INDEX_FOR_AUTOCOMPLETE);
		tmp.add(AbstractFieldAttributeReader.INDEX_FOR_SPELLCHECK);
		tmp.add(AbstractFieldAttributeReader.INDEXED);
		tmp.add(AbstractFieldAttributeReader.MULTI_VALUED);
		tmp.add(AbstractFieldAttributeReader.OMIT_TF);
		tmp.add(AbstractFieldAttributeReader.OMIT_POSITIONS);
		tmp.add(AbstractFieldAttributeReader.STORED);
		tmp.add(AbstractFieldAttributeReader.TERM_VECTORS);
		tmp.add(AbstractFieldAttributeReader.TYPE_NAME);
		COMMON_KEYS = Collections.unmodifiableSet(tmp);
	}

	public static enum AddUpdateMode {
		NEW, UPDATE, REMOVE;
	}
}
