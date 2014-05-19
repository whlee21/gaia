package gaia.api;

import gaia.admin.collection.CollectionManager;
import gaia.parser.gaia.GaiaQueryParserParams;
import gaia.parser.gaia.LikeDocParams;
import gaia.similarity.GaiaMultiLenNormSimilarity;
import gaia.utils.SolrTools;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;

import javax.ws.rs.core.Response;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.io.IOUtils;
import org.apache.lucene.search.similarities.Similarity;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.core.CoreContainer;
import org.apache.solr.core.SolrCore;
import org.apache.solr.schema.FieldType;
import org.apache.solr.schema.IndexSchema;
import org.apache.solr.schema.SchemaField;
import org.apache.solr.search.SolrIndexSearcher;
import org.apache.solr.util.RefCounted;
import org.apache.solr.util.SolrPluginUtils;
import org.codehaus.jackson.map.ObjectMapper;
import org.quartz.SchedulerException;
import org.restlet.data.Status;
import org.restlet.representation.InputRepresentation;
import org.restlet.resource.Delete;
import org.restlet.resource.Get;
import org.restlet.resource.Post;
import org.restlet.resource.ResourceException;
import org.xml.sax.SAXException;

import com.google.inject.Inject;

public class FieldsServerResource extends AbstractFieldsResource implements FieldsResource {
	static final Set<String> VALID_KEYS_NEW;
	static final Set<String> VALID_KEYS_UPDATE;
	private FieldAttributeReader fieldAttribReader;

	public String getEntityLabel() {
		return "field";
	}

	@Inject
	public FieldsServerResource(CollectionManager cm, CoreContainer cores) {
		super(cm, cores);
	}

	public void doInit() throws ResourceException {
		super.doInit();
		if (null != solrCore)
			fieldAttribReader = new FieldAttributeReader(solrCore, cm.getUpdateChain());
	}

	public FieldAttributeReader getAttributeReader() {
		return fieldAttribReader;
	}

	@Post("json")
	public Object add(InputRepresentation object) throws IOException, ParserConfigurationException, SAXException,
			SolrServerException, SchedulerException {
		FieldAttribs fieldAttribs = new FieldAttribs(cores, solrCore, cm.getUpdateChain(), cm.getGaiaSearchHandler());

		boolean bulk = true;

		String data = IOUtils.toString(object.getReader());

		ObjectMapper mapper = new ObjectMapper();
		Object o = null;
		if (data.startsWith("["))
			o = mapper.readValue(data, List.class);
		else if (data.startsWith("{"))
			o = mapper.readValue(data, Map.class);
		else
			o = data;
		List<Map<String, Object>> fieldsToAdd;
		if ((o instanceof List)) {
			fieldsToAdd = (List) o;
		} else {
			if ((o instanceof Map)) {
				bulk = false;
				fieldsToAdd = Collections.singletonList((Map<String, Object>) o);
			} else {
				throw ErrorUtils.statusExp(422, "Entity was neither a single field, or a map of fields, to add");
			}
		}
		List<Map<String, Object>> newFields = bulkAdd(fieldsToAdd, fieldAttribs);
		Object returnObject;
		if (bulk) {
			returnObject = newFields;
		} else {
			Map<String, Object> newField = (Map) newFields.get(0);
			String fieldName = (String) newField.get(AbstractFieldAttributeReader.NAME);
			getResponse().setLocationRef("fields/" + URLEncoder.encode(fieldName, "UTF-8"));

			returnObject = newField;
		}

		setStatus(Status.SUCCESS_CREATED);
		return returnObject;
	}

	private void addField(Map<String, Object> m, FieldAttribs fieldAttribs) throws ParserConfigurationException,
			IOException, SAXException, SolrServerException, SchedulerException, UnsupportedEncodingException {
		if (!isExisting()) {
			throw ErrorUtils.statusExp(Response.Status.NOT_FOUND, "URI not found");
		}

		if (m.size() == 0) {
			throw ErrorUtils.statusExp(422, "No input content found");
		}

		IndexSchema schema = solrCore.getLatestSchema();
		String fieldName = getNameProp(m);

		FieldType fieldType = getFieldTypeProp(schema, m);
		Map<String, Object> defaultProps = null;
		try {
			defaultProps = getAttributeReader().getAttributes(
					new SchemaField(fieldName, fieldType, parseProperties(fieldName, fieldType, m), null));
		} catch (ResourceException e1) {
			throw e1;
		} catch (RuntimeException ignored) {
			defaultProps = getAttributeReader().getAttributes(new SchemaField(fieldName, fieldType));
		}

		addOrUpdateField(cm, cores, solrCore, collection, fieldName, m, VALID_KEYS_NEW,
				AbstractFieldsResource.AddUpdateMode.NEW, defaultProps, fieldAttribs);

		AuditLogger.log("added field");
	}

	private List<Map<String, Object>> bulkAdd(List<Map<String, Object>> list, FieldAttribs fieldAttribs)
			throws UnsupportedEncodingException, ParserConfigurationException, IOException, SAXException,
			SolrServerException, SchedulerException {
		for (Map<String, Object> m : list) {
			addField(m, fieldAttribs);
		}

		fieldAttribs.save();

		APIUtils.reloadCore(collection, cores);

		List<Map<String, Object>> fields = new ArrayList<Map<String, Object>>();
		SolrCore core = cores.getCore(collection);
		try {
			FieldAttributeReader newAttrs = new FieldAttributeReader(core, cm.getUpdateChain());

			for (Map<String, Object> m : list)
				fields.add(newAttrs.getAttributes((String) m.get(AbstractFieldAttributeReader.NAME)));
		} finally {
			core.close();
		}

		return fields;
	}

	static void addOrUpdateField(CollectionManager cm, CoreContainer cores, SolrCore solrCore, String collection,
			String fieldName, Map<String, Object> params, Set<String> validKeys, AbstractFieldsResource.AddUpdateMode mode,
			Map<String, Object> existingAttrs, FieldAttribs fieldAttribs) throws ParserConfigurationException, IOException,
			SAXException, SolrServerException, SchedulerException {
		List<Error> errors = checkForValidFields(params, validKeys);

		boolean userSuppliedCopyFieldList = params.get(AbstractFieldAttributeReader.COPY_FIELDS) != null;

		if (existingAttrs != null) {
			existingAttrs.putAll(params);
			params = existingAttrs;
		}

		errors.addAll(validateParams(params, mode));

		if (errors.size() > 0) {
			throw ErrorUtils.statusExp(422, errors);
		}

		IndexSchema schema = solrCore.getLatestSchema();

		if (mode == AbstractFieldsResource.AddUpdateMode.NEW) {
			if (schema.getFields().containsKey(fieldName)) {
				throw ErrorUtils.statusExp(422, new Error(AbstractFieldAttributeReader.NAME, Error.E_FORBIDDEN_VALUE,
						"An explicit field already exists with the field name:" + fieldName));
			}

		}

		if (userSuppliedCopyFieldList) {
			Object cfsParam = params.get(AbstractFieldAttributeReader.COPY_FIELDS);
			List<String> cfs = gaia.utils.StringUtils.getList(String.class, cfsParam);

			fieldAttribs.esc.removeCopyField(fieldName, null);
			for (String entry : cfs) {
				SchemaField dstField = schema.getFieldOrNull(entry);
				if (dstField == null) {
					throw ErrorUtils.statusExp(422, new Error(AbstractFieldAttributeReader.COPY_FIELDS, Error.E_FORBIDDEN_VALUE,
							"Copy field '" + entry + "' is not defined in schema"));
				}

				fieldAttribs.esc.addCopyField(fieldName, entry);
			}

		}

		int properties = 0;
		if (AbstractFieldsResource.AddUpdateMode.REMOVE != mode) {
			try {
				properties = parseProperties(fieldName, getFieldTypeProp(schema, params), params);
			} catch (ResourceException e1) {
				throw e1;
			} catch (RuntimeException e) {
				throw ErrorUtils.statusExp(422, new Error("", Error.E_EXCEPTION, e.getMessage()));
			}

		}

		String norms = getString(params, FieldAttributeReader.SHORT_FIELD_BOOST);

		Similarity sim = schema.getSimilarity();

		if ((sim instanceof GaiaMultiLenNormSimilarity)) {
			fieldAttribs.gaiaSimilarityFields.addAll(((GaiaMultiLenNormSimilarity) sim).getSimilarityMap().keySet());
		}

		boolean indexed = gaia.utils.StringUtils.getBoolean(params.get(AbstractFieldAttributeReader.INDEXED))
				.booleanValue();

		if ((!indexed) && ("none".equalsIgnoreCase(norms))) {
			throw ErrorUtils.statusExp(422, new Error(AbstractFieldAttributeReader.INDEXED, Error.E_FORBIDDEN_VALUE,
					"short_field_boost cannot be set to none when !indexed"));
		}

		if ("none".equalsIgnoreCase(norms))
			properties |= ((Integer) propertyMap.get("omitNorms")).intValue();
		else {
			properties &= (((Integer) propertyMap.get("omitNorms")).intValue() ^ 0xFFFFFFFF);
		}
		if (("moderate".equalsIgnoreCase(norms)) && (mode != AbstractFieldsResource.AddUpdateMode.REMOVE))
			fieldAttribs.gaiaSimilarityFields.add(fieldName);
		else {
			fieldAttribs.gaiaSimilarityFields.remove(fieldName);
		}

		if ((gaia.utils.StringUtils.getBoolean(params.get(FieldAttributeReader.INCLUDE_IN_RESULTS)).booleanValue())
				&& (mode != AbstractFieldsResource.AddUpdateMode.REMOVE)) {
			fieldAttribs.returnedFields.add(fieldName);
		} else
			fieldAttribs.returnedFields.remove(fieldName);

		fieldAttribs.synonymsFields.addAll(FieldAttributeReader.getSet(fieldAttribs.gaiaParams.get(
				GaiaQueryParserParams.SYNONYMS_FIELDS, "").split(",")));
		if ((gaia.utils.StringUtils.getBoolean(params.get(FieldAttributeReader.SYNONYMS)).booleanValue())
				&& (mode != AbstractFieldsResource.AddUpdateMode.REMOVE)) {
			fieldAttribs.synonymsFields.add(fieldName);
		} else
			fieldAttribs.synonymsFields.remove(fieldName);

		fieldAttribs.stopwordsFields.addAll(FieldAttributeReader.getSet(fieldAttribs.gaiaParams.get(
				GaiaQueryParserParams.STOPWORDS_FIELDS, "").split(",")));
		if ((gaia.utils.StringUtils.getBoolean(params.get(FieldAttributeReader.STOPWORDS)).booleanValue())
				&& (mode != AbstractFieldsResource.AddUpdateMode.REMOVE)) {
			fieldAttribs.stopwordsFields.add(fieldName);
		} else
			fieldAttribs.stopwordsFields.remove(fieldName);

		fieldAttribs.highlightedFields.addAll(FieldAttributeReader.getSet(fieldAttribs.gaiaParams.get("hl.fl", "").split(
				",")));
		if ((gaia.utils.StringUtils.getBoolean(params.get(FieldAttributeReader.HIGHLIGHT)).booleanValue())
				&& (mode != AbstractFieldsResource.AddUpdateMode.REMOVE)) {
			fieldAttribs.highlightedFields.add(fieldName);
		} else
			fieldAttribs.highlightedFields.remove(fieldName);

		if (fieldAttribs.gaiaParams.getParams("facet.field") != null) {
			fieldAttribs.facetFields.addAll(FieldAttributeReader.getSet(fieldAttribs.gaiaParams.getParams("facet.field")));
		}

		if ((gaia.utils.StringUtils.getBoolean(params.get("facet")).booleanValue())
				&& (mode != AbstractFieldsResource.AddUpdateMode.REMOVE)) {
			fieldAttribs.facetFields.add(fieldName);
		} else
			fieldAttribs.facetFields.remove(fieldName);

		fieldAttribs.mltFields.addAll(FieldAttributeReader.getSet(fieldAttribs.gaiaParams.get(LikeDocParams.FL_PARAM, "")
				.split(",")));
		if ((gaia.utils.StringUtils.getBoolean(params.get("use_in_find_similar")).booleanValue())
				&& (mode != AbstractFieldsResource.AddUpdateMode.REMOVE)) {
			fieldAttribs.mltFields.add(fieldName);
		} else
			fieldAttribs.mltFields.remove(fieldName);

		boolean dedupeField = false;
		if ((gaia.utils.StringUtils.getBoolean(params.get(FieldAttributeReader.DEDUPE)).booleanValue())
				&& (mode != AbstractFieldsResource.AddUpdateMode.REMOVE)) {
			dedupeField = true;
		}

		String spellField = "spell";
		if ((gaia.utils.StringUtils.getBoolean(params.get(AbstractFieldAttributeReader.INDEX_FOR_SPELLCHECK))
				.booleanValue()) && (mode != AbstractFieldsResource.AddUpdateMode.REMOVE)) {
			SchemaField dstField = schema.getFieldOrNull(spellField);
			if (dstField == null) {
				throw ErrorUtils.statusExp(422, new Error(AbstractFieldAttributeReader.INDEX_FOR_SPELLCHECK,
						Error.E_FORBIDDEN_VALUE, "Field '" + spellField + "' is not defined in schema"));
			}

			fieldAttribs.esc.addCopyField(fieldName, spellField);
		} else {
			fieldAttribs.esc.removeCopyField(fieldName, spellField);
		}

		String autoCompleteField = "autocomplete";
		if ((gaia.utils.StringUtils.getBoolean(params.get(AbstractFieldAttributeReader.INDEX_FOR_AUTOCOMPLETE))
				.booleanValue()) && (mode != AbstractFieldsResource.AddUpdateMode.REMOVE)) {
			SchemaField dstField = schema.getFieldOrNull(autoCompleteField);
			if (dstField == null) {
				throw ErrorUtils.statusExp(422, new Error(AbstractFieldAttributeReader.INDEX_FOR_AUTOCOMPLETE,
						Error.E_FORBIDDEN_VALUE, "Field '" + autoCompleteField + "' is not defined in schema"));
			}

			fieldAttribs.esc.addCopyField(fieldName, autoCompleteField);
		} else {
			fieldAttribs.esc.removeCopyField(fieldName, autoCompleteField);
		}

		Map<String, Float> qfMap = SolrPluginUtils.parseFieldBoosts(fieldAttribs.gaiaParams.get("qf", ""));

		Set<String> qfFields = new HashSet<String>();
		qfFields.addAll(qfMap.keySet());

		if ((gaia.utils.StringUtils.getBoolean(params.get(FieldAttributeReader.SEARCH_BY_DEFAULT)).booleanValue())
				&& (mode != AbstractFieldsResource.AddUpdateMode.REMOVE)) {
			qfFields.add(fieldName);
		} else
			qfFields.remove(fieldName);

		Object boostParam = params.get(FieldAttributeReader.DEFAULT_BOOST);
		if (boostParam != null) {
			Float floatVal = (Float) params.get(FieldAttributeReader.DEFAULT_BOOST);
			if ((floatVal.floatValue() != 1.0F) && (!qfFields.contains(fieldName))) {
				throw ErrorUtils.statusExp(422, new Error(FieldAttributeReader.DEFAULT_BOOST, Error.E_FORBIDDEN_OP,
						"Cannot set default_boost for a field that is not set as search_by_default"));
			}

			qfMap.put(fieldName, floatVal);
		}

		for (String field : qfFields) {
			Float boost = (Float) qfMap.get(field);
			if ((boost != null) && (boost.floatValue() != 1.0F))
				fieldAttribs.qfFieldsBoosts.add(field + "^" + boost);
			else {
				fieldAttribs.qfFieldsBoosts.add(field);
			}

		}

		Object facetCntPerField = params.get(FieldAttributeReader.NUM_FACETS);
		if (facetCntPerField != null) {
			String limit = fieldAttribs.gaiaParams.get("facet.limit");
			if (limit == null) {
				limit = Integer.toString(100);
			}
			fieldAttribs.gaiaParams.set("f." + fieldName + ".facet.limit", new String[] { (String) null });
			if ((!limit.equals(facetCntPerField.toString())) && (mode != AbstractFieldsResource.AddUpdateMode.REMOVE)) {
				fieldAttribs.gaiaParams.add("f." + fieldName + ".facet.limit", new String[] { facetCntPerField.toString() });
			}

		}

		SchemaField field = null;
		if (mode != AbstractFieldsResource.AddUpdateMode.REMOVE) {
			FieldType fieldType = getFieldTypeProp(schema, params);

			String defaultValue = org.apache.commons.lang.StringUtils.stripToNull(getString(params,
					FieldAttributeReader.DEFAULT_VALUE));

			field = new SchemaField(fieldName, fieldType, properties, defaultValue);

			if (null != defaultValue) {
				try {
					Object trash = fieldType.createField(field, defaultValue, 1.0F);
				} catch (Exception e) {
					String type = getString(params, AbstractFieldAttributeReader.TYPE_NAME);

					throw ErrorUtils.statusExp(422, new Error(FieldAttributeReader.DEFAULT_VALUE, Error.E_INVALID_TYPE,
							"default_value is not valid in combination with field_type: " + type));
				}

			}

			fieldAttribs.updatedFields.put(fieldName, field);
		}

		fieldAttribs.gaiaParams.set("echoParams", new String[] { "all" });

		fieldAttribs.gaiaParams.set("fl", (String[]) fieldAttribs.returnedFields.toArray(new String[0]));
		fieldAttribs.gaiaParams.set("hl.fl",
				new String[] { org.apache.commons.lang.StringUtils.join(fieldAttribs.highlightedFields.iterator(), ",") });

		if (fieldAttribs.highlightedFields.size() > 0) {
			if (!fieldAttribs.gaiaParams.getBool("hl", false)) {
				fieldAttribs.gaiaParams.set("hl", new String[] { "true" });
			}
		}

		fieldAttribs.gaiaParams.set(GaiaQueryParserParams.SYNONYMS_FIELDS, new String[] { (String) null });
		if (fieldAttribs.synonymsFields.size() > 0) {
			fieldAttribs.gaiaParams.set(GaiaQueryParserParams.SYNONYMS_FIELDS,
					new String[] { org.apache.commons.lang.StringUtils.join(fieldAttribs.synonymsFields.iterator(), ",") });
		}

		fieldAttribs.gaiaParams.set(GaiaQueryParserParams.STOPWORDS_FIELDS, new String[] { (String) null });
		if (fieldAttribs.stopwordsFields.size() > 0) {
			fieldAttribs.gaiaParams.set(GaiaQueryParserParams.STOPWORDS_FIELDS,
					new String[] { org.apache.commons.lang.StringUtils.join(fieldAttribs.stopwordsFields.iterator(), ",") });
		}

		fieldAttribs.gaiaParams.set("qf",
				new String[] { org.apache.commons.lang.StringUtils.join(fieldAttribs.qfFieldsBoosts.iterator(), " ") });

		Set<String> pfFields = new HashSet<String>();
		for (String qfFieldName : qfFields) {
			SchemaField qfField;
			if (fieldName.equals(qfFieldName))
				qfField = field;
			else {
				qfField = schema.getFieldOrNull(qfFieldName);
			}
			if ((qfField != null) && (!qfField.omitPositions()) && (!qfField.omitTermFreqAndPositions())) {
				Float boost = (Float) qfMap.get(qfFieldName);
				if ((boost != null) && (boost.floatValue() != 1.0F))
					pfFields.add(qfFieldName + "^" + boost);
				else {
					pfFields.add(qfFieldName);
				}
			}
		}
		fieldAttribs.gaiaParams.set(GaiaQueryParserParams.PF,
				new String[] { org.apache.commons.lang.StringUtils.join(pfFields, " ") });

		fieldAttribs.gaiaParams.set("facet.field", new String[] { (String) null });
		for (String facetField : fieldAttribs.facetFields) {
			fieldAttribs.gaiaParams.add("facet.field", new String[] { facetField });
		}

		fieldAttribs.gaiaParams.set(LikeDocParams.FL_PARAM, new String[] { (String) null });
		if (fieldAttribs.mltFields.size() > 0) {
			fieldAttribs.gaiaParams.set(LikeDocParams.FL_PARAM,
					new String[] { org.apache.commons.lang.StringUtils.join(fieldAttribs.mltFields.iterator(), ",") });
		}

		if (dedupeField)
			fieldAttribs.dedupeFields.add(fieldName);
		else {
			fieldAttribs.dedupeFields.remove(fieldName);
		}

		if (AbstractFieldsResource.AddUpdateMode.REMOVE == mode) {
			String defaultSearchFieldName = schema.getDefaultSearchFieldName();

			if (fieldName.equals(defaultSearchFieldName)) {
				throw ErrorUtils.statusExp(422, new Error(AbstractFieldAttributeReader.NAME, Error.E_INVALID_OPERATION,
						"Field configured as 'defaultSearchField' in schema.xml can not be removed: " + fieldName));
			}

			fieldAttribs.updatedFields.remove(fieldName);
			fieldAttribs.esc.removeCopyField(fieldName, null);
			fieldAttribs.esc.removeCopyField(null, fieldName);
		}
	}

	static List<Error> validateParams(Map<String, Object> params, AbstractFieldsResource.AddUpdateMode mode) {
		if (AbstractFieldsResource.AddUpdateMode.REMOVE == mode)
			return Collections.emptyList();

		List<Error> errors = new ArrayList<Error>(5);

		if (mode == AbstractFieldsResource.AddUpdateMode.NEW) {
			String name = getNameProp(params);
			Matcher m = APIUtils.ALPHANUM.matcher(name);
			if (!m.matches()) {
				errors.add(new Error(AbstractFieldAttributeReader.NAME, Error.E_INVALID_VALUE,
						"name must consist of only A-Z a-z 0-9 - _"));
			}

		}

		validateCommonParams(params, mode, errors);

		boolean searchByDefault = gaia.utils.StringUtils.getBoolean(params.get(FieldAttributeReader.SEARCH_BY_DEFAULT),
				true).booleanValue();

		boolean includeInResults = gaia.utils.StringUtils.getBoolean(params.get(FieldAttributeReader.INCLUDE_IN_RESULTS),
				true).booleanValue();

		boolean highlight = gaia.utils.StringUtils.getBoolean(params.get(FieldAttributeReader.HIGHLIGHT), true)
				.booleanValue();

		Boolean indexed = gaia.utils.StringUtils.getBoolean(params.get(AbstractFieldAttributeReader.INDEXED), false);

		Boolean stored = gaia.utils.StringUtils.getBoolean(params.get(AbstractFieldAttributeReader.STORED), false);

		if (null == indexed) {
			errors.add(new Error(AbstractFieldAttributeReader.INDEXED, Error.E_MISSING_VALUE,
					"indexed was not specified, or determined from the fieldType"));
		} else if ((!indexed.booleanValue()) && (searchByDefault)) {
			errors.add(new Error(FieldAttributeReader.SEARCH_BY_DEFAULT, Error.E_INVALID_VALUE,
					"search_by_default cannot be true if indexed is false"));
		}

		if (null == stored) {
			errors.add(new Error(AbstractFieldAttributeReader.STORED, Error.E_MISSING_VALUE,
					"stored was not specified, or determined from the fieldType"));
		} else if (!stored.booleanValue()) {
			if (includeInResults) {
				errors.add(new Error("include_in_results", Error.E_INVALID_VALUE,
						"include_in_results cannot be true if stored is false"));
			}
			if (highlight) {
				errors.add(new Error(FieldAttributeReader.HIGHLIGHT, Error.E_INVALID_VALUE,
						"highlight cannot be true if stored is false"));
			}

		}

		Object defaultBoost = params.get(FieldAttributeReader.DEFAULT_BOOST);
		if (defaultBoost != null) {
			try {
				params.put(FieldAttributeReader.DEFAULT_BOOST, Float.valueOf(Float.parseFloat(defaultBoost.toString())));
			} catch (NumberFormatException e) {
				errors.add(new Error(FieldAttributeReader.DEFAULT_BOOST, Error.E_INVALID_VALUE,
						"Could not parse default_boost as float:" + defaultBoost.toString()));
			}
		}

		return errors;
	}

	private List<Map<String, Object>> getFields(boolean includeDynamic) {
		Set<String> fields = getAllFields(solrCore, includeDynamic);
		List<Map<String, Object>> allFieldAttribs = new ArrayList<Map<String, Object>>(fields.size());
		for (String field : fields) {
			allFieldAttribs.add(getAttributeReader().getAttributes(field));
		}

		return allFieldAttribs;
	}

	@Get("json")
	public List<Map<String, Object>> retrieve() {
		if (!isExisting()) {
			throw ErrorUtils.statusExp(Response.Status.NOT_FOUND, "URI not found");
		}

		boolean includeDynamic = gaia.utils.StringUtils.getBoolean(getQuery().getFirstValue("include_dynamic"))
				.booleanValue();

		return getFields(includeDynamic);
	}

	@Delete("json")
	public void remove() throws IOException, ParserConfigurationException, SAXException, SolrServerException,
			SchedulerException {
		if (!isExisting()) {
			throw ErrorUtils.statusExp(Response.Status.NOT_FOUND, "URI not found");
		}
		FieldAttribs fieldAttribs = new FieldAttribs(cores, solrCore, cm.getUpdateChain(), cm.getGaiaSearchHandler());

		String[] fieldNames = getQuery().getValuesArray(AbstractFieldAttributeReader.NAME);

		for (String fieldName : fieldNames) {
			addOrUpdateField(cm, cores, solrCore, collection, fieldName, Collections.<String, Object> emptyMap(),
					Collections.<String> emptySet(), AbstractFieldsResource.AddUpdateMode.REMOVE, null, fieldAttribs);

			AuditLogger.log("removed field");
		}

		fieldAttribs.save();

		APIUtils.reloadCore(collection, cores);
		setStatus(Status.SUCCESS_NO_CONTENT);
	}

	private static Set<String> getAllFields(SolrCore solrCore, boolean includeDynamic) {
		IndexSchema schema = solrCore.getLatestSchema();

		Set<String> allFields = new LinkedHashSet<String>(schema.getFields().keySet());

		if (includeDynamic) {
			Collection<String> allInIndex = Collections.emptySet();
			RefCounted<SolrIndexSearcher> searcher = solrCore.getNewestSearcher(true);
			try {
				allInIndex = SolrTools.getAllFieldNames(((SolrIndexSearcher) searcher.get()).getIndexReader());
			} finally {
				searcher.decref();
			}

			for (String name : allInIndex) {
				if ((!allFields.contains(name)) && (schema.isDynamicField(name))) {
					allFields.add(name);
				}
			}
		}
		return allFields;
	}

	static {
		Set<String> tmpN = new HashSet<String>(COMMON_KEYS);
		tmpN.add(FieldAttributeReader.DEDUPE);
		tmpN.add(FieldAttributeReader.DEFAULT_BOOST);
		tmpN.add(FieldAttributeReader.DEFAULT_VALUE);
		tmpN.add(FieldAttributeReader.FACET);
		tmpN.add(FieldAttributeReader.NUM_FACETS);
		tmpN.add(FieldAttributeReader.HIGHLIGHT);
		tmpN.add(FieldAttributeReader.INCLUDE_IN_RESULTS);
		tmpN.add(FieldAttributeReader.SEARCH_BY_DEFAULT);
		tmpN.add(FieldAttributeReader.SHORT_FIELD_BOOST);
		tmpN.add(FieldAttributeReader.STOPWORDS);
		tmpN.add(FieldAttributeReader.SYNONYMS);
		tmpN.add(FieldAttributeReader.USE_IN_FIND_SIMILAR);
		VALID_KEYS_NEW = Collections.unmodifiableSet(tmpN);

		Set<String> tmpU = new HashSet<String>(VALID_KEYS_NEW);
		tmpU.remove(AbstractFieldAttributeReader.NAME);
		VALID_KEYS_UPDATE = Collections.unmodifiableSet(tmpU);
	}
}
