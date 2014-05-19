package gaia.search.server.api.services;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import gaia.admin.collection.CollectionManager;
import gaia.api.APIUtils;
import gaia.api.AbstractFieldAttributeReader;
import gaia.api.AbstractFieldsResource;
import gaia.api.AuditLogger;
import gaia.api.DynamicFieldAttribs;
import gaia.api.DynamicFieldAttributeReader;
import gaia.api.DynamicFieldsServerResource;
import gaia.api.Error;
import gaia.api.ErrorUtils;
import gaia.api.ObjectSerializer;
import gaia.search.server.api.services.parsers.BodyParseException;
import gaia.search.server.api.services.parsers.RequestBodyParser;
import gaia.search.server.configuration.Configuration;
import gaia.utils.StringUtils;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.io.IOUtils;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.core.CoreContainer;
import org.apache.solr.core.SolrCore;
import org.apache.solr.schema.FieldSchemaExposer;
import org.apache.solr.schema.FieldType;
import org.apache.solr.schema.IndexSchema;
import org.apache.solr.schema.SchemaField;
import org.codehaus.jackson.map.ObjectMapper;
import org.quartz.SchedulerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;
import org.xnap.commons.i18n.I18nFactory;

public class DynamicFieldService  extends BaseService {

	private static org.xnap.commons.i18n.I18n i18n = I18nFactory.getI18n(DynamicFieldService.class);
	private static final Logger LOG = LoggerFactory.getLogger(DynamicFieldService.class);
	
	private final String collection;
	private CoreContainer cores;
	private CollectionManager cm;
	private Configuration configuration;
	private SolrCore solrCore;
	
	private boolean existing;
	
	static final Set<String> VALID_KEYS_NEW;
	static final Set<String> VALID_KEYS_UPDATE;// =
																							// Collections.unmodifiableSet(tmpU);
	private DynamicFieldAttributeReader attribReader;
	private static final Pattern VALID_DYN_FIELD_NAME = Pattern.compile("\\*|\\*" + APIUtils.ALPHANUM + "|"
			+ APIUtils.ALPHANUM + "\\*");
	static final Set<String> COMMON_KEYS;
	static final Set<String> tmp;
	static final String[] propertyNames = FieldSchemaExposer.getPropertyNames();
	
	
	public DynamicFieldService(ObjectSerializer serializer, RequestBodyParser bodyParser, String collectionName,
			CollectionManager cm, CoreContainer cores, Configuration configuration) {
		super(serializer, bodyParser);
		this.collection = collectionName;
		this.cm = cm;
		this.cores = cores;
		this.configuration = configuration;
		this.solrCore = this.cores.getCore(this.collection);
		
		if (null != solrCore)
			attribReader = new DynamicFieldAttributeReader(solrCore);

		setExisting(solrCore != null);
		
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

	static {
		Set<String> tmpN = new HashSet<String>(COMMON_KEYS);
		VALID_KEYS_NEW = Collections.unmodifiableSet(tmpN);

		Set<String> tmpU = new HashSet<String>(VALID_KEYS_NEW);
		tmpU.remove("name");
		VALID_KEYS_UPDATE = Collections.unmodifiableSet(tmpU);
	}


	public static enum AddUpdateMode {
		NEW, UPDATE, REMOVE;
	}

	@GET
	@Produces("text/plain")
	public Response getDynamicFields(@Context HttpHeaders headers, @Context UriInfo ui) {

		if (!isExisting()) {
			throw ErrorUtils.statusExp(Response.Status.NOT_FOUND, i18n.tr("URI not found"));
		}

		getAttributeReader();
		Map<String, SchemaField> allDynFields = DynamicFieldAttributeReader.getDynamicFieldPrototypes(solrCore
				.getLatestSchema());

		List<Map<String, Object>> results = new ArrayList<Map<String, Object>>(allDynFields.size());
		for (String name : allDynFields.keySet()) {
			results.add(getAttributeReader().getAttributes(name));
		}

		return buildResponse(Response.Status.OK, results); 
	}

	@GET
	@Path("{fieldName}")
	@Produces("text/plain")
	public Response getDynamicField(@Context HttpHeaders headers, @Context UriInfo ui,
			@PathParam("fieldName") String fieldName) {
		LOG.debug("whlee21 getDynamicField (collectionName, fieldName) = (" + collection+ ", " + fieldName + ")");

		if (!isExisting()) {
			throw ErrorUtils.statusExp(Response.Status.NOT_FOUND, i18n.tr("URI not found"));
		}

		getAttributeReader();
		DynamicFieldAttributeReader reader = new DynamicFieldAttributeReader(this.solrCore);
		Map<String, Object> dynAttr = reader.getAttributes(fieldName);

		

		return buildResponse(Response.Status.OK, dynAttr); 
	}

	@POST
	@Produces("text/plain")
	public Response createDynamicField(String body, @Context HttpHeaders headers, @Context UriInfo ui) 
			throws UnsupportedEncodingException, ParserConfigurationException, IOException, SAXException, 
			SolrServerException, SchedulerException, URISyntaxException {

		DynamicFieldAttribs fieldAttribs = new DynamicFieldAttribs(cores, solrCore);

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
				throw ErrorUtils
						.statusExp(422, i18n.tr("Entity was neither a single dynamic field, or a map of dynamic fields, to add"));
			}
		}
		List<Map<String, Object>> newFields = bulkAdd(fieldsToAdd, fieldAttribs);
		Object returnObject;
		String filedName = "";
		if (bulk) {
			returnObject = newFields;
		} else {
			Map<String, Object> newField = (Map) newFields.get(0);
			filedName = (String) newField.get("name");
//			getResponse().setLocationRef("dynamicfields/" + URLEncoder.encode(filedName, "UTF-8"));

			returnObject = newField;
		}
		
		URI seeOther = configuration.getCollectionUri(collection + "/dynamicfields/" + URLEncoder.encode(filedName, "UTF-8"));
		Response response = buildResponse(Response.Status.CREATED, seeOther, returnObject);

		return response;
		
	}
	

	public DynamicFieldAttributeReader getAttributeReader() {
		return attribReader;
	}
	
	private void addDynamicField(Map<String, Object> m, DynamicFieldAttribs fieldAttribs)
			throws ParserConfigurationException, IOException, SAXException, SolrServerException, SchedulerException,
			UnsupportedEncodingException {
		if (!isExisting()) {
			throw ErrorUtils.statusExp(Response.Status.NOT_FOUND, i18n.tr("URI not found"));
		}

		if (m.size() == 0) {
			throw ErrorUtils.statusExp(422, i18n.tr("No input content found"));
		}

		IndexSchema schema = solrCore.getLatestSchema();
		String name = getNameProp(m);

		FieldType fieldType = getFieldTypeProp(schema, m);
		Map<String, Object> defaultProps = null;
		try {
			defaultProps = getAttributeReader().getAttributes(
					new SchemaField(name, fieldType, parseProperties(name, fieldType, m), null));
		} catch (RuntimeException ignored) {
			defaultProps = getAttributeReader().getAttributes(new SchemaField(name, fieldType));
		}

		addOrUpdateDynamicField(cm, cores, solrCore, collection, name, m, VALID_KEYS_NEW,
				AddUpdateMode.NEW, defaultProps, fieldAttribs);

		LOG.debug("added dynamic field");
	}

	private List<Map<String, Object>> bulkAdd(List<Map<String, Object>> list, DynamicFieldAttribs fieldAttribs)
			throws UnsupportedEncodingException, ParserConfigurationException, IOException, SAXException,
			SolrServerException, SchedulerException {
		for (Map<String, Object> m : list) {
			addDynamicField(m, fieldAttribs);
		}

		fieldAttribs.save();

		APIUtils.reloadCore(collection, cores);

		List<Map<String, Object>> fields = new ArrayList<Map<String, Object>>();
		SolrCore core = cores.getCore(collection);
		try {
			DynamicFieldAttributeReader newAttrs = new DynamicFieldAttributeReader(core);

			for (Map<String, Object> m : list)
				fields.add(newAttrs.getAttributes((String) m.get("name")));
		} finally {
			core.close();
		}

		return fields;
	}

	static void addOrUpdateDynamicField(CollectionManager cm, CoreContainer cores, SolrCore solrCore, String collection,
			String name, Map<String, Object> params, Set<String> validKeys, AddUpdateMode mode,
			Map<String, Object> existingAttrs, DynamicFieldAttribs fieldAttribs) {
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

		if (mode == AddUpdateMode.NEW) {
			Map<String, SchemaField> existing = DynamicFieldAttributeReader.getDynamicFieldPrototypes(schema);

			if (existing.containsKey(name)) {
				throw ErrorUtils.statusExp(422, new Error("name", Error.E_FORBIDDEN_VALUE,
						i18n.tr("An dynamicfield already exists with the name:" + name)));
			}

		}

		if (userSuppliedCopyFieldList) {
			Object cfsParam = params.get(AbstractFieldAttributeReader.COPY_FIELDS);
			List<String> cfs = StringUtils.getList(String.class, cfsParam);

			fieldAttribs.esc.removeCopyField(name, null);
			for (String entry : cfs) {
				SchemaField dstField = schema.getFieldOrNull(entry);
				if (dstField == null) {
					throw ErrorUtils.statusExp(422, new Error(AbstractFieldAttributeReader.COPY_FIELDS, Error.E_FORBIDDEN_VALUE,
							i18n.tr("Copy field '" + entry + "' is not defined in schema")));
				}

				fieldAttribs.esc.addCopyField(name, entry);
			}

		}

		int properties = 0;
		if (AddUpdateMode.REMOVE != mode) {
			try {
				properties = parseProperties(name, getFieldTypeProp(schema, params), params);
			} catch (RuntimeException e) {
				throw ErrorUtils.statusExp(422, new Error("", i18n.tr(Error.E_EXCEPTION, e.getMessage())));
			}

		}

		String spellcheckField = "spell";
		if ((StringUtils.getBoolean(params.get(AbstractFieldAttributeReader.INDEX_FOR_SPELLCHECK)).booleanValue())
				&& (mode != AddUpdateMode.REMOVE)) {
			SchemaField dstField = schema.getFieldOrNull(spellcheckField);
			if (dstField == null) {
				throw ErrorUtils.statusExp(422, new Error(AbstractFieldAttributeReader.INDEX_FOR_SPELLCHECK,
						i18n.tr(Error.E_FORBIDDEN_VALUE, "Field '" + spellcheckField + "' is not defined in schema")));
			}

			fieldAttribs.esc.addCopyField(name, spellcheckField);
		} else {
			fieldAttribs.esc.removeCopyField(name, spellcheckField);
		}

		String autoCompleteField = "autocomplete";
		if ((StringUtils.getBoolean(params.get(AbstractFieldAttributeReader.INDEX_FOR_AUTOCOMPLETE)).booleanValue())
				&& (mode != AddUpdateMode.REMOVE)) {
			SchemaField dstField = schema.getFieldOrNull(autoCompleteField);
			if (dstField == null) {
				throw ErrorUtils.statusExp(422, new Error(AbstractFieldAttributeReader.INDEX_FOR_AUTOCOMPLETE,
						i18n.tr(Error.E_FORBIDDEN_VALUE, "Field '" + autoCompleteField + "' is not defined in schema")));
			}

			fieldAttribs.esc.addCopyField(name, autoCompleteField);
		} else {
			fieldAttribs.esc.removeCopyField(name, autoCompleteField);
		}

		if (mode != AddUpdateMode.REMOVE) {
			FieldType fieldType = getFieldTypeProp(schema, params);

			SchemaField field = new SchemaField(name, fieldType, properties, null);

			fieldAttribs.updatedFields.put(name, field);
		}

		if (AddUpdateMode.REMOVE == mode) {
			fieldAttribs.updatedFields.remove(name);

			fieldAttribs.esc.removeCopyField(name, null);
			fieldAttribs.esc.removeCopyField(null, name);

			List<Map<String, String>> copyFields = fieldAttribs.esc.getCopyFields();
			for (Map<String, String> copyField : copyFields) {
				String copyFieldDest = (String) copyField.get("dest");
				if ((schema.isDynamicField(copyFieldDest)) && (name.equals(schema.getDynamicPattern(copyFieldDest))))
					fieldAttribs.esc.removeCopyField(null, copyFieldDest);
			}
		}
	}

	static List<Error> validateParams(Map<String, Object> params, AddUpdateMode mode) {
		if (AddUpdateMode.REMOVE == mode)
			return Collections.emptyList();

		List<Error> errors = new ArrayList<Error>(5);

		if (mode == AddUpdateMode.NEW) {
			String name = getNameProp(params);
			Matcher m = VALID_DYN_FIELD_NAME.matcher(name);
			if (!m.matches()) {
				errors.add(new Error("name", Error.E_INVALID_VALUE,
						i18n.tr("name must consist of only A-Z a-z 0-9 - _ and either begin or end (but not both) with *")));
			}

		}

		validateCommonParams(params, mode, errors);

		return errors;
	}

	@PUT
	@Path("{fieldName}")
	@Produces("text/plain")
	public Response updateDynamicField(String body, @Context HttpHeaders headers, @Context UriInfo ui,
			@PathParam("fieldName") String fieldName) throws ParserConfigurationException, IOException, SAXException, 
			SolrServerException, SchedulerException {
//		return handleRequest(headers, body, ui, Request.Type.PUT, createDynamicFieldResource(collectionName, fieldName));
		try {
			RequestBody requestBody = getRequestBody(body);

			LOG.debug("#### fieldName: " + fieldName);

			return updateDynamicField(fieldName, requestBody.getProperties());
		} catch (BodyParseException e) {
			throw ErrorUtils.statusExp(Response.Status.INTERNAL_SERVER_ERROR, new Error("body", i18n.tr(Error.E_EXCEPTION,
					"cannot parse body " + body)));
		}
	}
	
	public Response updateDynamicField(String fieldName, Map<String, Object> m) throws ParserConfigurationException, IOException, SAXException,
	SolrServerException, SchedulerException {
		if (!isExisting()) {
			throw ErrorUtils.statusExp(Response.Status.NOT_FOUND, i18n.tr("URI not found"));
		}
		if (m.size() == 0) {
			throw ErrorUtils.statusExp(422, i18n.tr("No input content found"));
		}
		
		Map<String, Object> existingAttrs = getAttributeReader().getAttributes(fieldName);
		
		DynamicFieldAttribs fieldAttribs = new DynamicFieldAttribs(cores, solrCore);
		
		addOrUpdateDynamicField(cm, cores, solrCore, collection, fieldName,
				m, VALID_KEYS_UPDATE, AddUpdateMode.UPDATE, existingAttrs,
				fieldAttribs);
		
		fieldAttribs.save();
		
		APIUtils.reloadCore(collection, cores);

		LOG.debug("updated dynamicField");
		
		return buildResponse(Response.Status.NO_CONTENT);
	}
	
	@DELETE
	@Path("{fieldName}")
	@Produces("text/plain")
	public Response deleteDynamicField(@Context HttpHeaders headers, @Context UriInfo ui,
			@PathParam("fieldName") String fieldName) throws ParserConfigurationException, IOException, SAXException {
//		return handleRequest(headers, null, ui, Request.Type.DELETE, createDynamicFieldResource(collectionName, fieldName));
		if (!isExisting()) {
			 throw ErrorUtils.statusExp(Response.Status.NOT_FOUND, i18n.tr("URI not found"));
		}
		
		DynamicFieldAttribs fieldAttribs = new DynamicFieldAttribs(cores, solrCore);
		
		addOrUpdateDynamicField(cm, cores, solrCore, collection, fieldName,
				Collections.<String, Object> emptyMap(), Collections.<String> emptySet(),
				AddUpdateMode.REMOVE, null, fieldAttribs);
		
		fieldAttribs.save();
		
		AuditLogger.log(i18n.tr("removed dynamicField"));
		
		APIUtils.reloadCore(collection, cores);
		return buildResponse(Response.Status.NO_CONTENT);
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
					i18n.tr("indexed was not specified, or determined from the fieldType")));
		} else if (!indexed.booleanValue()) {
			if ((null != omitTf) && (!omitTf.booleanValue())) {
				errors.add(new Error(AbstractFieldAttributeReader.OMIT_TF, Error.E_INVALID_VALUE,
						i18n.tr("omit_tf cannot be false if indexed is false")));
			}
			if ((null != omitPositions) && (!omitPositions.booleanValue())) {
				errors.add(new Error(AbstractFieldAttributeReader.OMIT_POSITIONS, Error.E_INVALID_VALUE,
						i18n.tr("omit_positions cannot be false if indexed is false")));
			}
			if ((null != vectors) && (vectors.booleanValue())) {
				errors.add(new Error(AbstractFieldAttributeReader.TERM_VECTORS, Error.E_INVALID_VALUE,
						i18n.tr("term_vectors cannot be true if indexed is false")));
			}
		}

		if (null == omitTf) {
			errors.add(new Error(AbstractFieldAttributeReader.OMIT_TF, Error.E_MISSING_VALUE,
					i18n.tr("omit_tf was not specified, or determined from the fieldType")));
		}
		if (null == omitPositions) {
			errors.add(new Error(AbstractFieldAttributeReader.OMIT_POSITIONS, Error.E_MISSING_VALUE,
					i18n.tr("omit_positions was not specified, or determined from the fieldType")));
		}

		if ((null != omitTf) && (null != omitPositions) && (omitTf.booleanValue()) && (!omitPositions.booleanValue())) {
			errors.add(new Error(AbstractFieldAttributeReader.OMIT_POSITIONS, Error.E_INVALID_VALUE,
					i18n.tr("omit_positions cannot be false if omit_tf is true")));
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
					i18n.tr("name must be specified")));
		}

		return name;
	}

	public static FieldType getFieldTypeProp(IndexSchema schema, Map<String, Object> m) {
		String type = getString(m, AbstractFieldAttributeReader.TYPE_NAME);
		if (type == null) {
			throw ErrorUtils.statusExp(422, new Error(AbstractFieldAttributeReader.TYPE_NAME, Error.E_MISSING_VALUE,
					i18n.tr("You must specify a field_type for: " + getString(m, AbstractFieldAttributeReader.NAME))));
		}

		FieldType fieldType = (FieldType) schema.getFieldTypes().get(type);
		if (fieldType == null) {
			throw ErrorUtils.statusExp(422, new Error(AbstractFieldAttributeReader.TYPE_NAME, Error.E_INVALID_VALUE,
					i18n.tr("field_type is not valid:" + type)));
		}

		return fieldType;
	}
	

	public boolean isExisting() {
		return this.existing;
	}

	public void setExisting(boolean exists) {
		this.existing = exists;
	}
	
	
}