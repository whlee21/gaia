package gaia.crawl.datasource;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gaia.utils.StringUtils;

@SuppressWarnings("serial")
public class FieldMapping implements Serializable {
	private static final Logger LOG = LoggerFactory.getLogger(FieldMapping.class);
	public static final String MAPPING_APERTURE_KEY = "mapping.aperture";
	public static final String MAPPING_TIKA_KEY = "mapping.tika";
	private String uniqueKey = "id";
	private String datasourceField = "data_source";
	private String defaultField;
	private String dynamicField = "attr";
	private boolean verifySchema = true;
	private boolean addGaiaSearchFields = true;
	private boolean addOriginalContent = false;
	private Map<String, String> mappings = new HashMap<String, String>();
	private Map<String, String> literals = new HashMap<String, String>();
	private Map<String, FType> types = new HashMap<String, FType>();
	private Map<String, Boolean> multiVal = new HashMap<String, Boolean>();
	public static final String DATASOURCE_FIELD = "datasource_field";
	public static final String DEFAULT_FIELD = "default_field";
	public static final String DYNAMIC_FIELD = "dynamic_field";
	public static final String UNIQUE_KEY = "unique_key";
	public static final String VERIFY_SCHEMA = "verify_schema";
	public static final String ADD_GAIASEARCH_FIELDS = "gaiasearch_fields";
	public static final String ORIGINAL_CONTENT = "original_content";
	public static final String MAPPINGS = "mappings";
	public static final String LITERALS = "literals";
	public static final String TYPES = "types";
	public static final String MULTI_VAL = "multi_val";

	public FType checkType(String name) {
		if (name == null) {
			return FType.STRING;
		}
		name = name.toLowerCase();
		FType res = (FType) types.get(name);
		if (res == null) {
			return FType.STRING;
		}
		return res;
	}

	public Map<String, String> getMappings() {
		return mappings;
	}

	void setMappings(Map<String, String> mapping) {
		mappings = mapping;
	}

	public void setLiteral(String field, String value) {
		literals.put(field, value);
	}

	public String getLiteral(String field) {
		return (String) literals.get(field);
	}

	public void removeLiteral(String field) {
		literals.remove(field);
	}

	public Map<String, String> getLiterals() {
		return Collections.unmodifiableMap(literals);
	}

	void setLiterals(Map<String, String> literals) {
		this.literals = literals;
	}

	public void setFrom(FieldMapping other, boolean overwrite) {
		if (other == null) {
			return;
		}
		if ((uniqueKey == null) || (overwrite)) {
			uniqueKey = other.getUniqueKey();
		}
		if (overwrite) {
			dynamicField = other.getDynamicField();
			defaultField = other.getDefaultField();
			addGaiaSearchFields = other.addGaiaSearchFields;
			verifySchema = other.verifySchema;
		}

		for (Map.Entry<String, String> e : other.getMappings().entrySet()) {
			if ((overwrite) || (!mappings.containsKey(e.getKey()))) {
				mappings.put(e.getKey(), e.getValue());
			}
		}

		for (Map.Entry<String, FieldMapping.FType> e : other.types.entrySet()) {
			if ((overwrite) || (!types.containsKey(e.getKey()))) {
				types.put(e.getKey(), e.getValue());
			}
		}

		for (Map.Entry<String, Boolean> e : other.multiVal.entrySet()) {
			if ((overwrite) || (!multiVal.containsKey(e.getKey()))) {
				multiVal.put(e.getKey(), e.getValue());
			}
		}

		for (Map.Entry<String, String> e : other.getLiterals().entrySet())
			if ((overwrite) || (!literals.containsKey(e.getKey())))
				literals.put(e.getKey(), e.getValue());
	}

	public void defineMapping(String from, String to) {
		defineMapping(from, to, true);
	}

	public void defineMapping(String from, String to, boolean overwrite) {
		String key = from.toLowerCase();
		if (overwrite)
			mappings.put(key, to);
		else if (!mappings.containsKey(key))
			mappings.put(key, to);
	}

	public void removeMapping(String name) {
		mappings.remove(name.toLowerCase());
	}

	public void defineType(String field, FType type) {
		defineType(field, type, true);
	}

	public void defineType(String field, FType type, boolean overwrite) {
		if (overwrite)
			types.put(field.toLowerCase(), type);
		else if (!types.containsKey(field))
			types.put(field.toLowerCase(), type);
	}

	public void removeType(String field) {
		types.remove(field);
	}

	public void reset() {
		mappings.clear();
		types.clear();
		multiVal.clear();
		uniqueKey = null;
	}

	public void setUniqueKey(String uniqueKey) {
		this.uniqueKey = uniqueKey;
	}

	public String getUniqueKey() {
		return uniqueKey;
	}

	public String getDefaultField() {
		return defaultField;
	}

	public void setDefaultField(String defaultField) {
		this.defaultField = defaultField;
	}

	public void setDynamicField(String dynamicField) {
		this.dynamicField = dynamicField;
	}

	public String getDynamicField() {
		return dynamicField;
	}

	public void setDatasourceField(String dsField) {
		datasourceField = dsField;
	}

	public String getDatasourceField() {
		return datasourceField;
	}

	public void setMultivalued(String field, boolean multiValued) {
		multiVal.put(field, Boolean.valueOf(multiValued));
	}

	public Boolean isMultivalued(String field) {
		if ((dynamicField != null) && (field.startsWith(dynamicField))) {
			field = dynamicField;
		}
		Boolean b = (Boolean) multiVal.get(field);
		if (b == null) {
			return null;
		}
		return b;
	}

	public Map<String, Boolean> getMultivalued() {
		return multiVal;
	}

	public boolean isVerifySchema() {
		return verifySchema;
	}

	public void setVerifySchema(boolean verifySchema) {
		this.verifySchema = verifySchema;
	}

	public boolean isAddGaiaSearchFields() {
		return addGaiaSearchFields;
	}

	public void setAddGaiaSearchFields(boolean addGaiaSearchFields) {
		this.addGaiaSearchFields = addGaiaSearchFields;
	}

	public boolean isAddOriginalContent() {
		return addOriginalContent;
	}

	public void setAddOriginalContent(boolean addOriginalContent) {
		this.addOriginalContent = addOriginalContent;
	}

	public Map<String, Object> toMap() {
		HashMap<String, Object> map = new HashMap<String, Object>();
		map.put("datasource_field", datasourceField);
		map.put("default_field", defaultField);
		map.put("dynamic_field", dynamicField);
		map.put("verify_schema", Boolean.valueOf(verifySchema));
		map.put(ADD_GAIASEARCH_FIELDS, Boolean.valueOf(addGaiaSearchFields));
		map.put("original_content", Boolean.valueOf(addOriginalContent));
		map.put("unique_key", uniqueKey);

		if (mappings.containsKey(null)) {
			synchronized (mappings) {
				mappings.remove(null);
			}
		}
		map.put("mappings", mappings);
		if (multiVal.containsKey(null)) {
			synchronized (multiVal) {
				multiVal.remove(null);
			}
		}
		map.put("multi_val", multiVal);

		if (literals.containsKey(null)) {
			synchronized (literals) {
				literals.remove(null);
			}
		}
		map.put("literals", literals);

		Map<String, String> sTypes = new HashMap<String, String>();
		for (Map.Entry<String, FType> e : types.entrySet())
			if (e.getKey() != null) {
				sTypes.put(e.getKey(), e.getValue().toString());
			}
		map.put("types", sTypes);
		return map;
	}

	public static void fromMap(FieldMapping template, Map<String, Object> map) {
		if (map.containsKey("datasource_field")) {
			template.datasourceField = ((String) map.get("datasource_field"));
		}
		if (map.containsKey("default_field")) {
			template.defaultField = ((String) map.get("default_field"));
		}
		if (map.containsKey("dynamic_field")) {
			template.dynamicField = ((String) map.get("dynamic_field"));
		}
		if (map.containsKey("unique_key")) {
			template.uniqueKey = ((String) map.get("unique_key"));
		}
		if (map.containsKey("verify_schema")) {
			template.verifySchema = StringUtils.getBoolean(map.get("verify_schema")).booleanValue();
		}
		if (map.containsKey(ADD_GAIASEARCH_FIELDS)) {
			template.addGaiaSearchFields = StringUtils.getBoolean(map.get(ADD_GAIASEARCH_FIELDS)).booleanValue();
		}
		if (map.containsKey("original_content")) {
			template.addOriginalContent = StringUtils.getBoolean(map.get("original_content")).booleanValue();
		}
		Map<Object, Object> inmap = (Map) map.get("mappings");
		if (inmap != null) {
			for (Map.Entry<Object, Object> e : inmap.entrySet()) {
				String k = e.getKey().toString();
				String v = e.getValue() != null ? e.getValue().toString() : null;
				template.mappings.put(k, v);
			}
		}
		inmap = (Map) map.get("literals");
		if (inmap != null) {
			for (Map.Entry<Object, Object> e : inmap.entrySet()) {
				String k = e.getKey().toString();
				String v = e.getValue() != null ? e.getValue().toString() : null;
				template.literals.put(k, v);
			}
		}
		inmap = (Map) map.get("types");
		if (inmap != null) {
			for (Map.Entry<Object, Object> e : inmap.entrySet()) {
				FType type;
				try {
					type = FType.valueOf(e.getValue().toString());
				} catch (Exception ex) {
					LOG.warn("Invalid field type " + e.getValue() + ", skipping");
					continue;
				}

				template.types.put(e.getKey().toString(), type);
			}
		}
		inmap = (Map) map.get("multi_val");
		if (inmap != null)
			for (Map.Entry<Object, Object> e : inmap.entrySet()) {
				Boolean flag = StringUtils.getBoolean(e.getValue());
				if (flag != null) {
					template.multiVal.put(e.getKey().toString(), flag);
				}
			}
	}

	public Map<String, FType> getTypes() {
		return types;
	}

	public void setTypes(Map<String, FType> types) {
		this.types = types;
	}

	public static FieldMapping defaultFieldMapping() {
		FieldMapping defaultMapping = new FieldMapping();
		defaultMapping.setUniqueKey("id");

		defaultMapping.defineMapping("crawl_uri", "crawl_uri");
		defaultMapping.defineMapping("batch_id", "batch_id");

		defaultMapping.defineMapping("Content-Encoding", "characterSet");
		defaultMapping.defineMapping("Content-Length", "fileSize");
		defaultMapping.defineMapping("FileSize", "fileSize");
		defaultMapping.defineType("fileSize", FType.LONG);
		defaultMapping.defineMapping("FileName", "fileName");
		defaultMapping.defineMapping("Content-Type", "mimeType");
		defaultMapping.defineMapping("MimeType", "mimeType");
		defaultMapping.defineMapping("Title", "title");
		defaultMapping.defineMapping("Description", "description");
		defaultMapping.defineMapping("Subject", "subject");
		defaultMapping.defineMapping("created", "dateCreated");
		defaultMapping.defineMapping("ContentCreated", "dateCreated");
		defaultMapping.defineType("dateCreated", FType.DATE);
		defaultMapping.defineType("lastModified", FType.DATE);
		defaultMapping.defineType("date", FType.DATE);
		defaultMapping.defineMapping("creator", "creator");
		defaultMapping.defineMapping("Contributor", "author");
		defaultMapping.defineMapping("LastModifiedBy", "author");
		defaultMapping.defineMapping("author", "author");
		defaultMapping.defineMapping("fullname", "author");
		defaultMapping.defineMapping("body", "body");
		defaultMapping.defineMapping("Last-Modified", "lastModified");
		defaultMapping.defineMapping("fileLastModified", "lastModified");
		defaultMapping.defineMapping("lastModified", "lastModified");
		defaultMapping.defineMapping("ContentLastModified", "lastModified");
		defaultMapping.defineMapping("fullText", "body");
		defaultMapping.defineMapping("plainTextContent", "body");
		defaultMapping.defineMapping("plainTextMessageContent", "body");
		defaultMapping.defineMapping("keyword", "keywords");
		defaultMapping.defineMapping("messageSubject", "title");
		defaultMapping.defineMapping("name", "title");
		defaultMapping.defineMapping("Page-Count", "pageCount");
		defaultMapping.defineMapping("PageCount", "pageCount");
		defaultMapping.defineMapping("Slide-Count", "pageCount");
		defaultMapping.defineMapping("Slides", "pageCount");
		defaultMapping.defineMapping("url", "url");
		defaultMapping.defineMapping("acl", "acl");

		defaultMapping.defineMapping("date", null);
		defaultMapping.defineMapping("Last-Printed", null);
		defaultMapping.defineMapping("type", null);
		defaultMapping.defineMapping("links", null);

		defaultMapping.setMultivalued("dateCreated", false);
		defaultMapping.setMultivalued("author", true);
		defaultMapping.setMultivalued("title", false);
		defaultMapping.setMultivalued("description", false);
		defaultMapping.setMultivalued("body", false);
		defaultMapping.setMultivalued("acl", true);
		defaultMapping.setMultivalued("fileSize", false);
		defaultMapping.setMultivalued("mimeType", false);
		defaultMapping.setDynamicField("attr");
		return defaultMapping;
	}

	public int hashCode() {
		int prime = 31;
		int result = 1;
		result = prime * result + (addGaiaSearchFields ? 1231 : 1237);
		result = prime * result + (addOriginalContent ? 1231 : 1237);
		result = prime * result + (datasourceField == null ? 0 : datasourceField.hashCode());

		result = prime * result + (defaultField == null ? 0 : defaultField.hashCode());

		result = prime * result + (dynamicField == null ? 0 : dynamicField.hashCode());

		result = prime * result + (literals == null ? 0 : literals.hashCode());
		result = prime * result + (mappings == null ? 0 : mappings.hashCode());
		result = prime * result + (multiVal == null ? 0 : multiVal.hashCode());
		result = prime * result + (types == null ? 0 : types.hashCode());
		result = prime * result + (uniqueKey == null ? 0 : uniqueKey.hashCode());
		result = prime * result + (verifySchema ? 1231 : 1237);
		return result;
	}

	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		FieldMapping other = (FieldMapping) obj;
		if (addGaiaSearchFields != other.addGaiaSearchFields)
			return false;
		if (addOriginalContent != other.addOriginalContent)
			return false;
		if (datasourceField == null) {
			if (other.datasourceField != null)
				return false;
		} else if (!datasourceField.equals(other.datasourceField))
			return false;
		if (defaultField == null) {
			if (other.defaultField != null)
				return false;
		} else if (!defaultField.equals(other.defaultField))
			return false;
		if (dynamicField == null) {
			if (other.dynamicField != null)
				return false;
		} else if (!dynamicField.equals(other.dynamicField))
			return false;
		if (literals == null) {
			if (other.literals != null)
				return false;
		} else if (!literals.equals(other.literals))
			return false;
		if (mappings == null) {
			if (other.mappings != null)
				return false;
		} else if (!mappings.equals(other.mappings))
			return false;
		if (multiVal == null) {
			if (other.multiVal != null)
				return false;
		} else if (!multiVal.equals(other.multiVal))
			return false;
		if (types == null) {
			if (other.types != null)
				return false;
		} else if (!types.equals(other.types))
			return false;
		if (uniqueKey == null) {
			if (other.uniqueKey != null)
				return false;
		} else if (!uniqueKey.equals(other.uniqueKey))
			return false;
		if (verifySchema != other.verifySchema)
			return false;
		return true;
	}

	public String toString() {
		return toMap().toString();
	}

	public static enum FType {
		STRING, INT, LONG, FLOAT, DOUBLE, DATE;
	}
}
