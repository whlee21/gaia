package gaia.api;

import gaia.parser.gaia.GaiaQueryParserParams;
import gaia.parser.gaia.LikeDocParams;
import gaia.similarity.GaiaMultiLenNormSimilarity;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.lucene.search.similarities.Similarity;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.core.SolrCore;
import org.apache.solr.schema.IndexSchema;
import org.apache.solr.schema.SchemaField;
import org.apache.solr.update.processor.SignatureUpdateProcessorFactory;
import org.apache.solr.update.processor.UpdateRequestProcessorChain;
import org.apache.solr.update.processor.UpdateRequestProcessorFactory;
import org.apache.solr.util.SolrPluginUtils;

public class FieldAttributeReader extends AbstractFieldAttributeReader {
	public static final int SOLR_DEFAULT_FACET_LIMIT = 100;
	public static final String INCLUDE_IN_RESULTS = "include_in_results";
	public static final String DEDUPE = "use_for_deduplication";
	public static final String EDITABLE = "editable";
	public static final String DYNAMIC_BASE = "dynamic_base";
	public static final String USE_IN_FIND_SIMILAR = "use_in_find_similar";
	public static final String FACET = "facet";
	public static final String NUM_FACETS = "num_facets";
	public static final String HIGHLIGHT = "highlight";
	public static final String STOPWORDS = "query_time_stopword_handling";
	public static final String SYNONYMS = "synonym_expansion";
	public static final String SHORT_FIELD_BOOST = "short_field_boost";
	public static final String SEARCH_BY_DEFAULT = "search_by_default";
	public static final String DEFAULT_BOOST = "default_boost";
	public static final String DEFAULT_VALUE = "default_value";
	public static final String FIELD_NAME = "name";
	private static final Integer DEFAULT_NUM_FACETS_PER_FIELD = Integer.valueOf(5);

	private static Set<String> UNEDITABLE_FIELDS = new HashSet<String>(2);
	private Set<String> dedupeFields;
	private final String updateChain;
	private static Pattern LOOKS_LIKE_FUNCTION = Pattern.compile("\\([^\\)]*,[^\\(]*\\)");
	private static Pattern COMMA = Pattern.compile(",");

	public FieldAttributeReader(SolrCore solrCore, String updateChain) {
		super(solrCore);
		this.updateChain = updateChain;
	}

	public Map<String, Object> getAttributes(String fieldName) {
		IndexSchema schema = solrCore.getLatestSchema();
		SchemaField field = schema.getFieldOrNull(fieldName);

		return getAttributes(field);
	}

	public Map<String, Object> getAttributes(SchemaField field) {
		Map<String, Object> attribs = super.getAttributes(field);
		if (null == attribs)
			return null;

		IndexSchema schema = solrCore.getLatestSchema();

		String fieldName = field.getName();

		SolrParams gaiaParams = FieldAttribs.getGaiaParams(solrCore);

		Set<String> gaiaFl = parseFlParam(gaiaParams.getParams("fl"));

		if (schema.isDynamicField(fieldName))
			attribs.put(DYNAMIC_BASE, schema.getDynamicPattern(fieldName));
		else {
			attribs.put(DYNAMIC_BASE, null);
		}

		Similarity sim = schema.getSimilarity();

		if (field.omitNorms())
			attribs.put(SHORT_FIELD_BOOST, "none");
		else if (((sim instanceof GaiaMultiLenNormSimilarity))
				&& (((GaiaMultiLenNormSimilarity) sim).getSimilarityMap().containsKey(fieldName))) {
			attribs.put(SHORT_FIELD_BOOST, "moderate");
		} else
			attribs.put(SHORT_FIELD_BOOST, "high");

		addListContainsAttrib(attribs, SYNONYMS, gaiaParams.get(GaiaQueryParserParams.SYNONYMS_FIELDS), fieldName);

		addListContainsAttrib(attribs, STOPWORDS, gaiaParams.get(GaiaQueryParserParams.STOPWORDS_FIELDS), fieldName);

		addListContainsAttrib(attribs, HIGHLIGHT, gaiaParams.get("hl.fl"), fieldName);

		addListContainsAttrib(attribs, FACET, gaiaParams.getParams("facet.field"), fieldName);

		addListContainsAttrib(attribs, USE_IN_FIND_SIMILAR, gaiaParams.get(LikeDocParams.FL_PARAM), fieldName);

		attribs.put(INCLUDE_IN_RESULTS, Boolean.valueOf(gaiaFl.contains(fieldName)));

		attribs.put(EDITABLE, Boolean.valueOf(!UNEDITABLE_FIELDS.contains(fieldName)));

		if (dedupeFields == null) {
			dedupeFields = getDedupeFields();
		}

		attribs.put(DEDUPE, Boolean.valueOf(dedupeFields.contains(fieldName)));

		attribs.put(SEARCH_BY_DEFAULT,
				Boolean.valueOf(SolrPluginUtils.parseFieldBoosts(gaiaParams.get("qf")).containsKey(fieldName)));

		Float defaultBoost = (Float) SolrPluginUtils.parseFieldBoosts(gaiaParams.get("qf")).get(fieldName);

		if (defaultBoost == null) {
			defaultBoost = Float.valueOf(1.0F);
		}
		attribs.put(DEFAULT_BOOST, defaultBoost);

		attribs.put(DEFAULT_VALUE, field.getDefaultValue());

		String fieldLimit = gaiaParams.get("f." + fieldName + ".facet.limit");

		if (fieldLimit != null) {
			attribs.put(NUM_FACETS, Integer.valueOf(Integer.parseInt(fieldLimit)));
		} else {
			String limit = gaiaParams.get("facet.limit");
			if (limit != null)
				attribs.put(NUM_FACETS, Integer.valueOf(Integer.parseInt(limit)));
			else {
				attribs.put(NUM_FACETS, Integer.valueOf(SOLR_DEFAULT_FACET_LIMIT));
			}
		}

		return attribs;
	}

	private synchronized Set<String> getDedupeFields() {
		UpdateRequestProcessorChain chain = solrCore.getUpdateProcessingChain(updateChain);

		SignatureUpdateProcessorFactory sigFactory = null;
		for (UpdateRequestProcessorFactory fac : chain.getFactories())
			if ((fac instanceof SignatureUpdateProcessorFactory)) {
				sigFactory = (SignatureUpdateProcessorFactory) fac;
				break;
			}
		Set<String> dedupeFields;
		if (sigFactory != null) {
			List<String> fields = sigFactory.getSigFields();
			if (fields != null) {
				dedupeFields = new HashSet<String>(fields.size());
				dedupeFields.addAll(fields);
			} else {
				dedupeFields = new HashSet<String>(0);
			}
		} else {
			dedupeFields = new HashSet<String>(0);
		}

		return dedupeFields;
	}

	public static LinkedHashSet<String> parseFlParam(String[] fl) {
		if ((null == fl) || (0 == fl.length)) {
			return new LinkedHashSet<String>();
		}

		if (1 < fl.length) {
			return new LinkedHashSet<String>(Arrays.asList(fl));
		}

		LinkedHashSet<String> result = new LinkedHashSet<String>();

		String single = fl[0];

		Matcher m = LOOKS_LIKE_FUNCTION.matcher(single);
		if (m.find()) {
			result.add(single);
			return result;
		}

		result.addAll(Arrays.asList(COMMA.split(single)));
		return result;
	}

	static {
		UNEDITABLE_FIELDS.add("id");
		UNEDITABLE_FIELDS.add("timestamp");
	}
}
