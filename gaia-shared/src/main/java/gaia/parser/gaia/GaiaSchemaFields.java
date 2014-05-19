package gaia.parser.gaia;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.solr.core.SolrCore;
import org.apache.solr.schema.IndexSchema;
import org.apache.solr.schema.SchemaField;
import org.apache.solr.search.SolrIndexSearcher;
import org.apache.solr.util.RefCounted;

import gaia.utils.SolrTools;

class GaiaSchemaFields {
	public GaiaQueryParser parser = null;

	IndexSchema schema = null;

	SolrCore core = null;

	public GaiaQueryParserUtils.InsensitiveStringMap<GaiaSchemaField> schemaFields = new GaiaQueryParserUtils.InsensitiveStringMap();

	public GaiaSchemaFieldTypes schemaFieldTypes = null;

	public GaiaSchemaFields(GaiaQueryParser parser) {
		this.parser = parser;
		schema = parser.getSchema();
		core = parser.getCore();

		schemaFieldTypes = new GaiaSchemaFieldTypes(parser, this);
	}

	public GaiaSchemaField get(String fieldName) {
		if (schemaFields == null) {
			return null;
		}
		GaiaSchemaField f = (GaiaSchemaField) schemaFields.get(fieldName);
		if (f == null) {
			SchemaField sf = getSchemaField(fieldName);
			if (sf != null) {
				f = new GaiaSchemaField(this, sf);
				schemaFields.put(fieldName, f);
			}
		}

		return f;
	}

	public List<String> getNames() {
		List<String> fieldNames = new ArrayList<String>();

		Map<String, SchemaField> schemaFields = schema.getFields();
		Iterator<Entry<String, SchemaField>> it = schemaFields.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<String, SchemaField> entry = it.next();
			String key = (String) entry.getKey();
			fieldNames.add(key);
		}

		return fieldNames;
	}

	public SchemaField getSchemaField(String fieldName) {
		Map<String, SchemaField> schemaFields = schema.getFields();
		Iterator<Map.Entry<String, SchemaField>> it = schemaFields.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<String, SchemaField> entry = it.next();
			String key = (String) entry.getKey();
			if (key.equalsIgnoreCase(fieldName)) {
				return (SchemaField) entry.getValue();
			}
		}

		String regex = schema.getDynamicPattern(fieldName);
		if ((regex != null) && (!regex.equals("*"))) {
			return schema.getFieldOrNull(fieldName);
		}

		regex = schema.getDynamicPattern(fieldName.toLowerCase());
		if ((regex != null) && (!regex.equals("*"))) {
			return schema.getFieldOrNull(fieldName.toLowerCase());
		}

		RefCounted<SolrIndexSearcher> rcsis = core.getSearcher();
		String matchedFieldName = null;
		try {
			Collection<String> names = SolrTools.getAllFieldNames(((SolrIndexSearcher) rcsis.get()).getIndexReader());
			Iterator<String> it2 = names.iterator();
			while (it2.hasNext()) {
				String s = it2.next();
				if (s.equalsIgnoreCase(fieldName)) {
					matchedFieldName = s;
					break;
				}
			}
		} finally {
			rcsis.decref();
		}
		if (matchedFieldName != null) {
			return schema.getFieldOrNull(matchedFieldName);
		}
		return null;
	}

	public boolean isValidFieldName(String fieldName) {
		return get(fieldName) != null;
	}

	public void setTextPrefix(String prefix) {
		schemaFieldTypes.setTextPrefix(prefix);
	}
}
