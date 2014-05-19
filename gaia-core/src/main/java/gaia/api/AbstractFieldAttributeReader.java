package gaia.api;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.solr.core.SolrCore;
import org.apache.solr.schema.CopyField;
import org.apache.solr.schema.IndexSchema;
import org.apache.solr.schema.SchemaField;

public abstract class AbstractFieldAttributeReader {
	public static final String AUTOCOMPLETE_COPY_FIELD = "autocomplete";
	public static final String SPELL_COPY_FIELD = "spell";
	public static final String OMIT_TF = "omit_tf";
	public static final String OMIT_POSITIONS = "omit_positions";
	public static final String TERM_VECTORS = "term_vectors";
	public static final String TYPE_NAME = "field_type";
	public static final String STORED = "stored";
	public static final String INDEXED = "indexed";
	public static final String MULTI_VALUED = "multi_valued";
	public static final String INDEX_FOR_SPELLCHECK = "index_for_spellcheck";
	public static final String INDEX_FOR_AUTOCOMPLETE = "index_for_autocomplete";
	public static final String NAME = "name";
	public static final String COPY_FIELDS = "copy_fields";
	protected final SolrCore solrCore;

	protected AbstractFieldAttributeReader(SolrCore solrCore) {
		this.solrCore = solrCore;
	}

	protected void addListContainsAttrib(Map<String, Object> attribs, String attribName, String list, String fieldName) {
		if (list == null) {
			return;
		}

		attribs.put(attribName, Boolean.valueOf(getSet(list.split(",")).contains(fieldName)));
	}

	protected void addListContainsAttrib(Map<String, Object> attribs, String attribName, String[] lists, String fieldName) {
		if (lists == null) {
			return;
		}
		StringBuilder sb = new StringBuilder();
		for (String list : lists) {
			if (sb.length() > 0) {
				sb.append(",");
			}
			sb.append(list);
		}

		attribs.put(attribName, Boolean.valueOf(getSet(sb.toString().split(",")).contains(fieldName)));
	}

	public abstract Map<String, Object> getAttributes(String paramString);

	public Map<String, Object> getAttributes(SchemaField field) {
		IndexSchema schema = solrCore.getLatestSchema();
		if (field == null) {
			return null;
		}
		String fieldName = field.getName();

		Map<String, Object> attribs = new HashMap<String, Object>();

		boolean indexed = field.indexed();

		attribs.put(NAME, fieldName);
		attribs.put(TYPE_NAME, field.getType().getTypeName());
		attribs.put(STORED, Boolean.valueOf(field.stored()));
		attribs.put(INDEXED, Boolean.valueOf(indexed));
		attribs.put(MULTI_VALUED, Boolean.valueOf(field.multiValued()));
		attribs.put(TERM_VECTORS, Boolean.valueOf(field.storeTermVector()));

		attribs.put(OMIT_TF, Boolean.valueOf((!indexed) || (field.omitTermFreqAndPositions())));
		attribs.put(OMIT_POSITIONS,
				Boolean.valueOf((!indexed) || (field.omitTermFreqAndPositions()) || (field.omitPositions())));

		attribs.put(INDEX_FOR_SPELLCHECK, Boolean.valueOf(false));
		List<CopyField> copyFields = schema.getCopyFieldsList(fieldName);
		for (CopyField copyField : copyFields) {
			if (copyField.getDestination().getName().equals(SPELL_COPY_FIELD)) {
				attribs.put(INDEX_FOR_SPELLCHECK, Boolean.valueOf(true));
				break;
			}

		}

		attribs.put(INDEX_FOR_AUTOCOMPLETE, Boolean.valueOf(false));
		copyFields = schema.getCopyFieldsList(fieldName);
		for (CopyField copyField : copyFields) {
			if (copyField.getDestination().getName().equals(AUTOCOMPLETE_COPY_FIELD)) {
				attribs.put(INDEX_FOR_AUTOCOMPLETE, Boolean.valueOf(true));
				break;
			}

		}

		List<String> cfs = new ArrayList<String>();
		copyFields = schema.getCopyFieldsList(fieldName);
		for (CopyField copyField : copyFields) {
			String dest = copyField.getDestination().getName();
			cfs.add(dest);
		}
		attribs.put(COPY_FIELDS, cfs);

		return attribs;
	}

	public static Set<String> getSet(String[] strings) {
		Set<String> set = new HashSet<String>(strings.length);
		for (String string : strings) {
			if (!"".equals(string))
				set.add(string);
		}
		return set;
	}
}
