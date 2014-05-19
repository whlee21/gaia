package gaia.search.server.api.services;

import gaia.admin.collection.CollectionManager;
import gaia.api.APIUtils;
import gaia.api.AbstractFieldAttributeReader;
import gaia.api.Error;
import gaia.api.ErrorUtils;
import gaia.api.FieldAttribs;
import gaia.api.FieldAttributeReader;
import gaia.api.ObjectSerializer;
import gaia.parser.gaia.GaiaQueryParserParams;
import gaia.parser.gaia.LikeDocParams;
import gaia.search.server.api.services.parsers.BodyParseException;
import gaia.search.server.api.services.parsers.RequestBodyParser;
import gaia.search.server.configuration.Configuration;
import gaia.similarity.GaiaMultiLenNormSimilarity;
import gaia.utils.SolrTools;
import gaia.utils.StringUtils;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.lucene.search.similarities.Similarity;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.core.CoreContainer;
import org.apache.solr.core.SolrCore;
import org.apache.solr.schema.FieldSchemaExposer;
import org.apache.solr.schema.FieldType;
import org.apache.solr.schema.IndexSchema;
import org.apache.solr.schema.SchemaField;
import org.apache.solr.search.SolrIndexSearcher;
import org.apache.solr.util.RefCounted;
import org.apache.solr.util.SolrPluginUtils;
import org.codehaus.jackson.map.ObjectMapper;
import org.quartz.SchedulerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;
import org.xnap.commons.i18n.I18nFactory;

public class FieldService extends BaseService {

	private static org.xnap.commons.i18n.I18n i18n = I18nFactory.getI18n(FieldService.class);
	private static final Logger LOG = LoggerFactory.getLogger(FieldService.class);

	private static final Set<String> VALID_KEYS_NEW;
	private static final Set<String> VALID_KEYS_UPDATE;
	private FieldAttributeReader fieldAttribReader;
	public static final String INCLUDE_DYNAMIC_PARAM = "include_dynamic";
	static final Set<String> tmp;
	static final Set<String> COMMON_KEYS;
	private CoreContainer cores;
	private CollectionManager cm;
	private String collection;
	private SolrCore solrCore;
	static final String[] propertyNames = FieldSchemaExposer.getPropertyNames();
	static final Map<String, Integer> propertyMap = FieldSchemaExposer.getPropertyMap();

	private Configuration configuration;
	private boolean existing;

	public FieldService(ObjectSerializer serializer, RequestBodyParser bodyParser, String collectionName,
			CollectionManager cm, CoreContainer cores, Configuration configuration) {
		super(serializer, bodyParser);
		this.collection = collectionName;
		this.cm = cm;
		this.cores = cores;
		this.configuration = configuration;

		this.solrCore = this.cores.getCore(this.collection);
		if (null != solrCore)
			fieldAttribReader = new FieldAttributeReader(solrCore, cm.getUpdateChain());

		LOG.debug("solrCor" + solrCore);
		setExisting(solrCore != null);
	}

	public FieldAttributeReader getAttributeReader() {
		return fieldAttribReader;
	}

	static {
		tmp = new HashSet<String>();
		tmp.add(FieldAttributeReader.COPY_FIELDS);
		tmp.add(FieldAttributeReader.NAME);
		tmp.add(FieldAttributeReader.INDEX_FOR_AUTOCOMPLETE);
		tmp.add(FieldAttributeReader.INDEX_FOR_SPELLCHECK);
		tmp.add(FieldAttributeReader.INDEXED);
		tmp.add(FieldAttributeReader.MULTI_VALUED);
		tmp.add(FieldAttributeReader.OMIT_TF);
		tmp.add(FieldAttributeReader.OMIT_POSITIONS);
		tmp.add(FieldAttributeReader.STORED);
		tmp.add(FieldAttributeReader.TERM_VECTORS);
		tmp.add(FieldAttributeReader.TYPE_NAME);
		COMMON_KEYS = Collections.unmodifiableSet(tmp);
	}

	public static enum AddUpdateMode {
		NEW, UPDATE, REMOVE;
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
		tmpU.remove(FieldAttributeReader.FIELD_NAME);
		VALID_KEYS_UPDATE = Collections.unmodifiableSet(tmpU);
	}

	public boolean isExisting() {
		return this.existing;
	}

	public void setExisting(boolean exists) {
		this.existing = exists;
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

	private Response getFields(boolean includeDynamic) {
		Set<String> fields = getAllFields(solrCore, includeDynamic);
		List<Map<String, Object>> allFieldAttribs = new ArrayList<Map<String, Object>>(fields.size());
		for (String field : fields) {
			allFieldAttribs.add(getAttributeReader().getAttributes(field));
		}

		return buildResponse(Response.Status.OK, allFieldAttribs);
	}

	@GET
	@Produces("text/plain")
	public Response getFields(String body, @Context HttpHeaders headers, @Context UriInfo ui,
			@QueryParam(INCLUDE_DYNAMIC_PARAM) String includeDynamic) {

		LOG.debug("**** getFields collectionName:" + collection);
		LOG.debug("hhokyung getFields  query param(includeDynamic): " + includeDynamic);
		if (!isExisting()) {
			throw ErrorUtils.statusExp(Response.Status.NOT_FOUND, i18n.tr("URI not found"));
		}

		boolean includeDynamicBool = false;

		if (includeDynamic != null) {
			if (includeDynamic.equals("true")) {
				includeDynamicBool = true;
			}
		}

		return getFields(includeDynamicBool);

	}

	@GET
	@Path("{fieldName}")
	@Produces("text/plain")
	public Response getField(@Context HttpHeaders headers, @Context UriInfo ui, @PathParam("fieldName") String fieldName,
			@QueryParam(INCLUDE_DYNAMIC_PARAM) String includeDynamic) {

		System.out.println("**** getField fieldName:" + fieldName);
		Map<String, Object> attrField = null;
		if (isExisting()) {
			IndexSchema schema = solrCore.getLatestSchema();

			if (schema.getField(fieldName) != null) {
				setExisting((null != schema.getFieldOrNull(fieldName)));
				if (isExisting()) {
					// SchemaField fieldObj = schema.getField(fieldName);
					attrField = getAttributeReader().getAttributes(fieldName);
					Iterator<String> keys = attrField.keySet().iterator();
					while (keys.hasNext()) {
						String key = (String) keys.next();
						LOG.debug("attribute key: " + key + " values: " + attrField.get(key));
					}
				}
			} else {
				LOG.debug("##### isExisting() is false");
			}
		}

		return buildResponse(Response.Status.OK, attrField);
	}

	
	private void addField(Map<String, Object> m, FieldAttribs fieldAttribs) throws ParserConfigurationException,
			IOException, SAXException, SolrServerException, SchedulerException, UnsupportedEncodingException,
			URISyntaxException {
		if (!isExisting()) {
			throw ErrorUtils.statusExp(Response.Status.NOT_FOUND, i18n.tr("URI not found"));
		}

		if (m.size() == 0) {
			throw ErrorUtils.statusExp(422, i18n.tr("No input content found"));
		}

		IndexSchema schema = solrCore.getLatestSchema();
		String fieldName = getNameProp(m);

		FieldType fieldType = getFieldTypeProp(schema, m);
		Map<String, Object> defaultProps = null;
		try {
			defaultProps = getAttributeReader().getAttributes(
					new SchemaField(fieldName, fieldType, parseProperties(fieldName, fieldType, m), null));
		} catch (RuntimeException ignored) {
			defaultProps = getAttributeReader().getAttributes(new SchemaField(fieldName, fieldType));
		}

		addOrUpdateField(cm, cores, solrCore, collection, fieldName, m, VALID_KEYS_NEW, AddUpdateMode.NEW, defaultProps,
				fieldAttribs);

		LOG.debug("added field");
		Map<String, Object> attrField = null;
		attrField = getAttributeReader().getAttributes(fieldName);

		
	}

	private List<Map<String, Object>> bulkAdd(List<Map<String, Object>> list, FieldAttribs fieldAttribs)
			throws UnsupportedEncodingException, ParserConfigurationException, IOException, SAXException,
			SolrServerException, SchedulerException, URISyntaxException {
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
	
	@POST
	@Produces("text/plain")
	public Response createField(String body, @Context HttpHeaders headers, @Context UriInfo ui)
			throws UnsupportedEncodingException, ParserConfigurationException, IOException, SAXException,
			SolrServerException, SchedulerException, URISyntaxException {

		System.out.println("**** createField collectionName:" + collection);
		FieldAttribs fieldAttribs = new FieldAttribs(cores, solrCore, cm.getUpdateChain(), cm.getGaiaSearchHandler());
		boolean bulk = true;

		String data = body;

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
				throw ErrorUtils.statusExp(422, i18n.tr("Entity was neither a single field, or a map of fields, to add"));
			}
		}
		
		List<Map<String, Object>> newFields = bulkAdd(fieldsToAdd, fieldAttribs);
		Object returnObject;
		String fieldName = "";
		if (bulk) {
			returnObject = newFields;
		} else {
			Map<String, Object> newField = (Map) newFields.get(0);
			fieldName = (String) newField.get(AbstractFieldAttributeReader.NAME);
			//getResponse().setLocationRef("fields/" + URLEncoder.encode(fieldName, "UTF-8"));

			returnObject = newField;
		}

		URI seeOther = configuration.getCollectionUri(collection + "/fields/" + URLEncoder.encode(fieldName, "UTF-8"));
		Response response = buildResponse(Response.Status.CREATED, seeOther, returnObject);

		return response;
		
		
//		try {
//			RequestBody requestBody = getRequestBody(body);
//			return addField(requestBody.getProperties(), fieldAttribs);
//		} catch (BodyParseException e) {
//			throw ErrorUtils.statusExp(Response.Status.INTERNAL_SERVER_ERROR, new Error("body", Error.E_EXCEPTION,
//					"cannot parse body " + body));
//		}

	}

	public Response updateField(String filedName, Map<String, Object> m) throws ParserConfigurationException,
			IOException, SAXException, SolrServerException, SchedulerException {
		if (!isExisting()) {
			throw ErrorUtils.statusExp(Response.Status.NOT_FOUND, i18n.tr("URI not found"));
		}
		if (m.size() == 0) {
			throw ErrorUtils.statusExp(422, i18n.tr("No input content found"));
		}
		Map<String, Object> existingAttrs = getAttributeReader().getAttributes(filedName);

		FieldAttribs fieldAttribs = new FieldAttribs(cores, solrCore, cm.getUpdateChain(), cm.getGaiaSearchHandler());

		addOrUpdateField(cm, cores, solrCore, collection, filedName, m, VALID_KEYS_UPDATE, AddUpdateMode.UPDATE,
				existingAttrs, fieldAttribs);

		fieldAttribs.save();

		APIUtils.reloadCore(collection, cores);

		LOG.debug("updated field");

		return buildResponse(Response.Status.NO_CONTENT);
	}

	@PUT
	@Path("{fieldName}")
	@Produces("text/plain")
	public Response updateField(String body, @Context HttpHeaders headers, @Context UriInfo ui,
			@PathParam("fieldName") String fieldName) throws ParserConfigurationException, IOException, SAXException,
			SolrServerException, SchedulerException {
		try {
			RequestBody requestBody = getRequestBody(body);

			LOG.debug("#### fieldName: " + fieldName);

			return updateField(fieldName, requestBody.getProperties());
		} catch (BodyParseException e) {
			throw ErrorUtils.statusExp(Response.Status.INTERNAL_SERVER_ERROR, new Error("body", Error.E_EXCEPTION,
					i18n.tr("cannot parse body " + body)));
		}
	}

	@DELETE
	@Path("{fieldName}")
	@Produces("text/plain")
	public Response deleteField(@Context HttpHeaders headers, @Context UriInfo ui,
			@PathParam("fieldName") String fieldName) throws ParserConfigurationException, IOException, SAXException,
			SolrServerException, SchedulerException {
		System.out.println("**** updateField fieldName:" + fieldName);
		//
		if (!isExisting()) {
			throw ErrorUtils.statusExp(Response.Status.NOT_FOUND, i18n.tr("URI not found"));
		}
		FieldAttribs fieldAttribs = new FieldAttribs(cores, solrCore, cm.getUpdateChain(), cm.getGaiaSearchHandler());

		addOrUpdateField(cm, cores, solrCore, collection, fieldName, Collections.<String, Object> emptyMap(),
				Collections.<String> emptySet(), AddUpdateMode.REMOVE, null, fieldAttribs);

		LOG.debug("removed field");

		fieldAttribs.save();

		APIUtils.reloadCore(collection, cores);
		return buildResponse(Response.Status.OK);
	}

	protected static List<Error> checkForValidFields(Map<String, Object> params, Set<String> validKeys) {
		List<Error> errors = new ArrayList<Error>();
		Set<String> keys = params.keySet();
		for (String key : keys) {
			if (!validKeys.contains(key)) {
				errors.add(new Error(key, Error.E_FORBIDDEN_KEY, i18n.tr("Unknown or dissallowed key found:" + key)));
			}
		}
		return errors;
	}

	protected static void validateCommonParams(Map<String, Object> params, AddUpdateMode mode, List<Error> errors) {
		if (AddUpdateMode.REMOVE == mode)
			return;

		Boolean indexed = StringUtils.getBoolean(params.get(FieldAttributeReader.INDEXED), false);

		Boolean omitTf = StringUtils.getBoolean(params.get(FieldAttributeReader.OMIT_TF), false);

		Boolean omitPositions = StringUtils.getBoolean(params.get(FieldAttributeReader.OMIT_POSITIONS), false);

		Boolean vectors = StringUtils.getBoolean(params.get(FieldAttributeReader.TERM_VECTORS), false);

		if (null == indexed) {
			errors.add(new Error(FieldAttributeReader.INDEXED, Error.E_MISSING_VALUE,
					i18n.tr("indexed was not specified, or determined from the fieldType")));
		} else if (!indexed.booleanValue()) {
			if ((null != omitTf) && (!omitTf.booleanValue())) {
				errors.add(new Error(FieldAttributeReader.OMIT_TF, Error.E_INVALID_VALUE,
						i18n.tr("omit_tf cannot be false if indexed is false")));
			}
			if ((null != omitPositions) && (!omitPositions.booleanValue())) {
				errors.add(new Error(FieldAttributeReader.OMIT_POSITIONS, Error.E_INVALID_VALUE,
						i18n.tr("omit_positions cannot be false if indexed is false")));
			}
			if ((null != vectors) && (vectors.booleanValue())) {
				errors.add(new Error(FieldAttributeReader.TERM_VECTORS, Error.E_INVALID_VALUE,
						i18n.tr("term_vectors cannot be true if indexed is false")));
			}
		}

		if (null == omitTf) {
			errors.add(new Error(FieldAttributeReader.OMIT_TF, Error.E_MISSING_VALUE,
					i18n.tr("omit_tf was not specified, or determined from the fieldType")));
		}
		if (null == omitPositions) {
			errors.add(new Error(FieldAttributeReader.OMIT_POSITIONS, Error.E_MISSING_VALUE,
					i18n.tr("omit_positions was not specified, or determined from the fieldType")));
		}

		if ((null != omitTf) && (null != omitPositions) && (omitTf.booleanValue()) && (!omitPositions.booleanValue())) {
			errors.add(new Error(FieldAttributeReader.OMIT_POSITIONS, Error.E_INVALID_VALUE,
					i18n.tr("omit_positions cannot be false if omit_tf is true")));
		}
	}

	public static String getNameProp(Map<String, Object> m) {
		String name = getString(m, FieldAttributeReader.FIELD_NAME);

		if ((name == null) || (name.trim().length() == 0)) {
			throw ErrorUtils.statusExp(422, new Error(FieldAttributeReader.FIELD_NAME, Error.E_MISSING_VALUE,
					i18n.tr("name must be specified")));
		}

		return name;
	}

	static List<Error> validateParams(Map<String, Object> params, AddUpdateMode mode) {
		if (AddUpdateMode.REMOVE == mode)
			return Collections.emptyList();

		List<Error> errors = new ArrayList<Error>(5);

		if (mode == AddUpdateMode.NEW) {
			String name = getNameProp(params);
			Matcher m = APIUtils.ALPHANUM.matcher(name);
			if (!m.matches()) {
				errors.add(new Error(FieldAttributeReader.FIELD_NAME, Error.E_INVALID_VALUE,
						i18n.tr("name must consist of only A-Z a-z 0-9 - _")));
			}

		}

		validateCommonParams(params, mode, errors);

		boolean searchByDefault = gaia.utils.StringUtils.getBoolean(params.get(FieldAttributeReader.SEARCH_BY_DEFAULT),
				true).booleanValue();

		boolean includeInResults = gaia.utils.StringUtils.getBoolean(params.get(FieldAttributeReader.INCLUDE_IN_RESULTS),
				true).booleanValue();

		boolean highlight = gaia.utils.StringUtils.getBoolean(params.get(FieldAttributeReader.HIGHLIGHT), true)
				.booleanValue();

		Boolean indexed = gaia.utils.StringUtils.getBoolean(params.get(FieldAttributeReader.INDEXED), false);

		Boolean stored = gaia.utils.StringUtils.getBoolean(params.get(FieldAttributeReader.STORED), false);

		if (null == indexed) {
			errors.add(new Error(FieldAttributeReader.INDEXED, Error.E_MISSING_VALUE,
					i18n.tr("indexed was not specified, or determined from the fieldType")));
		} else if ((!indexed.booleanValue()) && (searchByDefault)) {
			errors.add(new Error(FieldAttributeReader.SEARCH_BY_DEFAULT, Error.E_INVALID_VALUE,
					i18n.tr("search_by_default cannot be true if indexed is false")));
		}

		if (null == stored) {
			errors.add(new Error(FieldAttributeReader.STORED, Error.E_MISSING_VALUE,
					i18n.tr("stored was not specified, or determined from the fieldType")));
		} else if (!stored.booleanValue()) {
			if (includeInResults) {
				errors.add(new Error(FieldAttributeReader.INCLUDE_IN_RESULTS, Error.E_INVALID_VALUE,
						i18n.tr("include_in_results cannot be true if stored is false")));
			}
			if (highlight) {
				errors.add(new Error(FieldAttributeReader.HIGHLIGHT, Error.E_INVALID_VALUE,
						i18n.tr("highlight cannot be true if stored is false")));
			}

		}

		Object defaultBoost = params.get(FieldAttributeReader.DEFAULT_BOOST);
		if (defaultBoost != null) {
			try {
				params.put(FieldAttributeReader.DEFAULT_BOOST, Float.valueOf(Float.parseFloat(defaultBoost.toString())));
			} catch (NumberFormatException e) {
				errors.add(new Error(FieldAttributeReader.DEFAULT_BOOST, Error.E_INVALID_VALUE,
						i18n.tr("Could not parse default_boost as float:" + defaultBoost.toString())));
			}
		}

		return errors;
	}

	static String getString(Map<String, Object> params, String key) {
		Object value = params.get(key);

		return value == null ? null : value.toString();
	}

	public static FieldType getFieldTypeProp(IndexSchema schema, Map<String, Object> m) {
		String type = getString(m, FieldAttributeReader.TYPE_NAME);
		if (type == null) {
			throw ErrorUtils.statusExp(422, new Error(FieldAttributeReader.TYPE_NAME, Error.E_MISSING_VALUE,
					i18n.tr("You must specify a field_type for: " + getString(m, FieldAttributeReader.FIELD_NAME))));
		}

		FieldType fieldType = (FieldType) schema.getFieldTypes().get(type);
		if (fieldType == null) {
			throw ErrorUtils.statusExp(422, new Error(FieldAttributeReader.TYPE_NAME, Error.E_INVALID_VALUE,
					i18n.tr("field_type is not valid:" + type)));
		}

		return fieldType;
	}

	protected static int parseProperties(String name, FieldType fieldType, Map<String, Object> params) {
		Map<String, String> propMap = new HashMap<String, String>();
		for (String propertyName : propertyNames) {
			String paramName;
			if (propertyName.equals("termVectors")) {
				paramName = FieldAttributeReader.TERM_VECTORS;
			} else {
				if (propertyName.equals("omitTermFreqAndPositions")) {
					paramName = FieldAttributeReader.OMIT_TF;
				} else {
					if (propertyName.equals("omitPositions")) {
						paramName = FieldAttributeReader.OMIT_POSITIONS;
					} else {
						if (propertyName.equals("multiValued"))
							paramName = FieldAttributeReader.MULTI_VALUED;
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

	
	
	
	static void addOrUpdateField(CollectionManager cm, CoreContainer cores, SolrCore solrCore, String collection,
			String fieldName, Map<String, Object> params, Set<String> validKeys, AddUpdateMode mode,
			Map<String, Object> existingAttrs, FieldAttribs fieldAttribs) throws ParserConfigurationException, IOException,
			SAXException, SolrServerException, SchedulerException {
		List<Error> errors = checkForValidFields(params, validKeys);

		boolean userSuppliedCopyFieldList = params.get(FieldAttributeReader.COPY_FIELDS) != null;

		if (existingAttrs != null) {
			existingAttrs.putAll(params);
			params = existingAttrs;
		}

		errors.addAll(validateParams(params, mode));

		if (errors.size() > 0) {
			throw ErrorUtils.statusExp(422, errors);
		}

		IndexSchema schema = solrCore.getLatestSchema();

		if (mode == AddUpdateMode.NEW) {
			if (schema.getFields().containsKey(fieldName)) {
				throw ErrorUtils.statusExp(422, new Error(FieldAttributeReader.FIELD_NAME, Error.E_FORBIDDEN_VALUE,
						i18n.tr("An explicit field already exists with the field name:" + fieldName)));
			}

		}

		if (userSuppliedCopyFieldList) {
			Object cfsParam = params.get(FieldAttributeReader.COPY_FIELDS);
			List<String> cfs = gaia.utils.StringUtils.getList(String.class, cfsParam);

			fieldAttribs.esc.removeCopyField(fieldName, null);
			for (String entry : cfs) {
				SchemaField dstField = schema.getFieldOrNull(entry);
				if (dstField == null) {
					throw ErrorUtils.statusExp(422, new Error(FieldAttributeReader.COPY_FIELDS, Error.E_FORBIDDEN_VALUE,
							i18n.tr("Copy field '" + entry + "' is not defined in schema")));
				}

				fieldAttribs.esc.addCopyField(fieldName, entry);
			}

		}

		int properties = 0;
		if (AddUpdateMode.REMOVE != mode) {
			try {
				properties = parseProperties(fieldName, getFieldTypeProp(schema, params), params);
			} catch (RuntimeException e) {
				throw ErrorUtils.statusExp(422, new Error("", Error.E_EXCEPTION, i18n.tr(e.getMessage())));
			}

		}

		String norms = getString(params, FieldAttributeReader.SHORT_FIELD_BOOST);

		Similarity sim = schema.getSimilarity();

		if ((sim instanceof GaiaMultiLenNormSimilarity)) {
			fieldAttribs.gaiaSimilarityFields.addAll(((GaiaMultiLenNormSimilarity) sim).getSimilarityMap().keySet());
		}

		boolean indexed = gaia.utils.StringUtils.getBoolean(params.get(FieldAttributeReader.INDEXED)).booleanValue();

		if ((!indexed) && ("none".equalsIgnoreCase(norms))) {
			throw ErrorUtils.statusExp(422, new Error(FieldAttributeReader.INDEXED, Error.E_FORBIDDEN_VALUE,
					i18n.tr("short_field_boost cannot be set to none when !indexed")));
		}

		if ("none".equalsIgnoreCase(norms))
			properties |= ((Integer) propertyMap.get("omitNorms")).intValue();
		else {
			properties &= (((Integer) propertyMap.get("omitNorms")).intValue() ^ 0xFFFFFFFF);
		}
		if (("moderate".equalsIgnoreCase(norms)) && (mode != AddUpdateMode.REMOVE))
			fieldAttribs.gaiaSimilarityFields.add(fieldName);
		else {
			fieldAttribs.gaiaSimilarityFields.remove(fieldName);
		}

		if ((gaia.utils.StringUtils.getBoolean(params.get(FieldAttributeReader.INCLUDE_IN_RESULTS)).booleanValue())
				&& (mode != AddUpdateMode.REMOVE)) {
			fieldAttribs.returnedFields.add(fieldName);
		} else
			fieldAttribs.returnedFields.remove(fieldName);

		fieldAttribs.synonymsFields.addAll(FieldAttributeReader.getSet(fieldAttribs.gaiaParams.get(
				GaiaQueryParserParams.SYNONYMS_FIELDS, "").split(",")));
		if ((gaia.utils.StringUtils.getBoolean(params.get(FieldAttributeReader.SYNONYMS)).booleanValue())
				&& (mode != AddUpdateMode.REMOVE)) {
			fieldAttribs.synonymsFields.add(fieldName);
		} else
			fieldAttribs.synonymsFields.remove(fieldName);

		fieldAttribs.stopwordsFields.addAll(FieldAttributeReader.getSet(fieldAttribs.gaiaParams.get(
				GaiaQueryParserParams.STOPWORDS_FIELDS, "").split(",")));
		if ((gaia.utils.StringUtils.getBoolean(params.get(FieldAttributeReader.STOPWORDS)).booleanValue())
				&& (mode != AddUpdateMode.REMOVE)) {
			fieldAttribs.stopwordsFields.add(fieldName);
		} else
			fieldAttribs.stopwordsFields.remove(fieldName);

		fieldAttribs.highlightedFields.addAll(FieldAttributeReader.getSet(fieldAttribs.gaiaParams.get("hl.fl", "").split(
				",")));
		if ((gaia.utils.StringUtils.getBoolean(params.get(FieldAttributeReader.HIGHLIGHT)).booleanValue())
				&& (mode != AddUpdateMode.REMOVE)) {
			fieldAttribs.highlightedFields.add(fieldName);
		} else
			fieldAttribs.highlightedFields.remove(fieldName);

		if (fieldAttribs.gaiaParams.getParams("facet.field") != null) {
			fieldAttribs.facetFields.addAll(FieldAttributeReader.getSet(fieldAttribs.gaiaParams.getParams("facet.field")));
		}

		if ((gaia.utils.StringUtils.getBoolean(params.get(FieldAttributeReader.FACET)).booleanValue())
				&& (mode != AddUpdateMode.REMOVE)) {
			fieldAttribs.facetFields.add(fieldName);
		} else
			fieldAttribs.facetFields.remove(fieldName);

		fieldAttribs.mltFields.addAll(FieldAttributeReader.getSet(fieldAttribs.gaiaParams.get(LikeDocParams.FL_PARAM, "")
				.split(",")));
		if ((gaia.utils.StringUtils.getBoolean(params.get(FieldAttributeReader.USE_IN_FIND_SIMILAR)).booleanValue())
				&& (mode != AddUpdateMode.REMOVE)) {
			fieldAttribs.mltFields.add(fieldName);
		} else
			fieldAttribs.mltFields.remove(fieldName);

		boolean dedupeField = false;
		if ((gaia.utils.StringUtils.getBoolean(params.get(FieldAttributeReader.DEDUPE)).booleanValue())
				&& (mode != AddUpdateMode.REMOVE)) {
			dedupeField = true;
		}

		String spellField = FieldAttributeReader.SPELL_COPY_FIELD;
		if ((gaia.utils.StringUtils.getBoolean(params.get(FieldAttributeReader.INDEX_FOR_SPELLCHECK)).booleanValue())
				&& (mode != AddUpdateMode.REMOVE)) {
			SchemaField dstField = schema.getFieldOrNull(spellField);
			if (dstField == null) {
				throw ErrorUtils.statusExp(422, new Error(FieldAttributeReader.INDEX_FOR_SPELLCHECK, Error.E_FORBIDDEN_VALUE,
						i18n.tr("Field '" + spellField + "' is not defined in schema")));
			}

			fieldAttribs.esc.addCopyField(fieldName, spellField);
		} else {
			fieldAttribs.esc.removeCopyField(fieldName, spellField);
		}

		String autoCompleteField = FieldAttributeReader.AUTOCOMPLETE_COPY_FIELD;
		if ((gaia.utils.StringUtils.getBoolean(params.get(FieldAttributeReader.INDEX_FOR_AUTOCOMPLETE)).booleanValue())
				&& (mode != AddUpdateMode.REMOVE)) {
			SchemaField dstField = schema.getFieldOrNull(autoCompleteField);
			if (dstField == null) {
				throw ErrorUtils.statusExp(422, new Error(FieldAttributeReader.INDEX_FOR_AUTOCOMPLETE, Error.E_FORBIDDEN_VALUE,
						i18n.tr("Field '" + autoCompleteField + "' is not defined in schema")));
			}

			fieldAttribs.esc.addCopyField(fieldName, autoCompleteField);
		} else {
			fieldAttribs.esc.removeCopyField(fieldName, autoCompleteField);
		}

		Map<String, Float> qfMap = SolrPluginUtils.parseFieldBoosts(fieldAttribs.gaiaParams.get("qf", ""));

		Set<String> qfFields = new HashSet<String>();
		qfFields.addAll(qfMap.keySet());

		if ((gaia.utils.StringUtils.getBoolean(params.get(FieldAttributeReader.SEARCH_BY_DEFAULT)).booleanValue())
				&& (mode != AddUpdateMode.REMOVE)) {
			qfFields.add(fieldName);
		} else
			qfFields.remove(fieldName);

		Object boostParam = params.get(FieldAttributeReader.DEFAULT_BOOST);
		if (boostParam != null) {
			Float floatVal = (Float) params.get(FieldAttributeReader.DEFAULT_BOOST);
			if ((floatVal.floatValue() != 1.0F) && (!qfFields.contains(fieldName))) {
				throw ErrorUtils.statusExp(422, new Error(FieldAttributeReader.DEFAULT_BOOST, Error.E_FORBIDDEN_OP,
						i18n.tr("Cannot set default_boost for a field that is not set as search_by_default")));
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
				limit = Integer.toString(FieldAttributeReader.SOLR_DEFAULT_FACET_LIMIT);
			}
			fieldAttribs.gaiaParams.set("f." + fieldName + ".facet.limit", new String[] { (String) null });
			if ((!limit.equals(facetCntPerField.toString())) && (mode != AddUpdateMode.REMOVE)) {
				fieldAttribs.gaiaParams.add("f." + fieldName + ".facet.limit", new String[] { facetCntPerField.toString() });
			}

		}

		SchemaField field = null;
		if (mode != AddUpdateMode.REMOVE) {
			FieldType fieldType = getFieldTypeProp(schema, params);

			String defaultValue = org.apache.commons.lang.StringUtils.stripToNull(getString(params,
					FieldAttributeReader.DEFAULT_VALUE));

			field = new SchemaField(fieldName, fieldType, properties, defaultValue);

			if (null != defaultValue) {
				try {
					Object trash = fieldType.createField(field, defaultValue, 1.0F);
				} catch (Exception e) {
					String type = getString(params, FieldAttributeReader.TYPE_NAME);

					throw ErrorUtils.statusExp(422, new Error(FieldAttributeReader.DEFAULT_VALUE, Error.E_INVALID_TYPE,
							i18n.tr("default_value is not valid in combination with field_type: " + type)));
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
		fieldAttribs.gaiaParams.set("pf", new String[] { org.apache.commons.lang.StringUtils.join(pfFields, " ") });

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

		if (AddUpdateMode.REMOVE == mode) {
			String defaultSearchFieldName = schema.getDefaultSearchFieldName();

			if (fieldName.equals(defaultSearchFieldName)) {
				throw ErrorUtils.statusExp(422, new Error(FieldAttributeReader.FIELD_NAME, Error.E_INVALID_OPERATION,
						i18n.tr("Field configured as 'defaultSearchField' in schema.xml can not be removed: " + fieldName)));
			}

			fieldAttribs.updatedFields.remove(fieldName);
			fieldAttribs.esc.removeCopyField(fieldName, null);
			fieldAttribs.esc.removeCopyField(null, fieldName);
		}
	}
}
