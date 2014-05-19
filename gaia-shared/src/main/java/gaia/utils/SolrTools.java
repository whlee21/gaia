package gaia.utils;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.lucene.index.FieldInfo;
import org.apache.lucene.index.FieldInfos;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.MultiFields;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.core.SolrCore;
import org.apache.solr.schema.IndexSchema;
import org.apache.solr.schema.SchemaField;
import org.apache.solr.search.SolrIndexSearcher;
import org.apache.solr.util.RefCounted;
import org.apache.solr.util.SolrPluginUtils;

public class SolrTools {
	public String queryEscape(String q) {
		return ClientUtils.escapeQueryChars(q);
	}

	public boolean enabled(String s) {
		return (s.startsWith("true")) || (s.startsWith("on")) || (s.startsWith("yes"));
	}

	public boolean isNamedList(Object object) {
		return object instanceof NamedList;
	}

	public boolean isInstanceOf(Object object, String className) throws ClassNotFoundException {
		Class<?> theClass = Class.forName(className);
		return theClass.isInstance(object);
	}

	public Map<String, Float> parseQF(String qf) {
		return SolrPluginUtils.parseFieldBoosts(qf);
	}

	public String getSystemProperty(String key) {
		return System.getProperty(key);
	}

	public static SchemaField getSchemaField(SolrCore core, IndexSchema schema, String fieldName) {
		Map<String, SchemaField> schemaFields = schema.getFields();
		Iterator<Entry<String, SchemaField>> it = schemaFields.entrySet().iterator();
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
			Collection<String> names = getAllFieldNames(((SolrIndexSearcher) rcsis.get()).getIndexReader());
			Iterator<String> it2 = names.iterator();
			while (it2.hasNext()) {
				String s = (String) it2.next();
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

	public static Collection<String> getAllFieldNames(IndexReader r) {
		Set<String> fieldNames = new HashSet<String>();
		FieldInfos infos = MultiFields.getMergedFieldInfos(r);
		for (FieldInfo info : infos) {
			fieldNames.add(info.name);
		}
		return fieldNames;
	}
}
