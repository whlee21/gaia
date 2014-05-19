package gaia.api;

import gaia.admin.collection.CollectionManager;
import gaia.utils.StringUtils;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.ws.rs.core.Response;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.io.IOUtils;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.core.CoreContainer;
import org.apache.solr.core.SolrCore;
import org.apache.solr.schema.FieldType;
import org.apache.solr.schema.IndexSchema;
import org.apache.solr.schema.SchemaField;
import org.codehaus.jackson.map.ObjectMapper;
import org.json.JSONException;
import org.quartz.SchedulerException;
import org.restlet.data.Status;
import org.restlet.representation.InputRepresentation;
import org.restlet.resource.Delete;
import org.restlet.resource.Get;
import org.restlet.resource.Post;
import org.restlet.resource.ResourceException;
import org.xml.sax.SAXException;

import com.google.inject.Inject;

public class DynamicFieldsServerResource extends AbstractFieldsResource implements DynamicFieldsResource {
	static final Set<String> VALID_KEYS_NEW;
	static final Set<String> VALID_KEYS_UPDATE;// =
																							// Collections.unmodifiableSet(tmpU);
	private DynamicFieldAttributeReader attribReader;
	private static final Pattern VALID_DYN_FIELD_NAME = Pattern.compile("\\*|\\*" + APIUtils.ALPHANUM + "|"
			+ APIUtils.ALPHANUM + "\\*");

	public String getEntityLabel() {
		return "dynamic field";
	}

	@Inject
	public DynamicFieldsServerResource(CollectionManager cm, CoreContainer cores) {
		super(cm, cores);
	}

	public void doInit() throws ResourceException {
		super.doInit();

		if (null != solrCore)
			attribReader = new DynamicFieldAttributeReader(solrCore);
	}

	public DynamicFieldAttributeReader getAttributeReader() {
		return attribReader;
	}

	@Post("json")
	public Object add(InputRepresentation obj) throws IOException, ParserConfigurationException, SAXException,
			SolrServerException, SchedulerException, JSONException {
		DynamicFieldAttribs fieldAttribs = new DynamicFieldAttribs(cores, solrCore);

		boolean bulk = true;

		String data = IOUtils.toString(obj.getReader());

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
						.statusExp(422, "Entity was neither a single dynamic field, or a map of dynamic fields, to add");
			}
		}
		List<Map<String, Object>> newFields = bulkAdd(fieldsToAdd, fieldAttribs);
		Object returnObject;
		if (bulk) {
			returnObject = newFields;
		} else {
			Map<String, Object> newField = (Map) newFields.get(0);
			String name = (String) newField.get("name");
			getResponse().setLocationRef("dynamicfields/" + URLEncoder.encode(name, "UTF-8"));

			returnObject = newField;
		}

		setStatus(Status.SUCCESS_CREATED);
		return returnObject;
	}

	private void addDynamicField(Map<String, Object> m, DynamicFieldAttribs fieldAttribs)
			throws ParserConfigurationException, IOException, SAXException, SolrServerException, SchedulerException,
			UnsupportedEncodingException {
		if (!isExisting()) {
			throw ErrorUtils.statusExp(Response.Status.NOT_FOUND, "URI not found");
		}

		if (m.size() == 0) {
			throw ErrorUtils.statusExp(422, "No input content found");
		}

		IndexSchema schema = solrCore.getLatestSchema();
		String name = getNameProp(m);

		FieldType fieldType = getFieldTypeProp(schema, m);
		Map<String, Object> defaultProps = null;
		try {
			defaultProps = getAttributeReader().getAttributes(
					new SchemaField(name, fieldType, parseProperties(name, fieldType, m), null));
		} catch (ResourceException e1) {
			throw e1;
		} catch (RuntimeException ignored) {
			defaultProps = getAttributeReader().getAttributes(new SchemaField(name, fieldType));
		}

		addOrUpdateDynamicField(cm, cores, solrCore, collection, name, m, VALID_KEYS_NEW,
				AbstractFieldsResource.AddUpdateMode.NEW, defaultProps, fieldAttribs);

		AuditLogger.log("added dynamic field");
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
			String name, Map<String, Object> params, Set<String> validKeys, AbstractFieldsResource.AddUpdateMode mode,
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

		if (mode == AbstractFieldsResource.AddUpdateMode.NEW) {
			Map<String, SchemaField> existing = DynamicFieldAttributeReader.getDynamicFieldPrototypes(schema);

			if (existing.containsKey(name)) {
				throw ErrorUtils.statusExp(422, new Error("name", Error.E_FORBIDDEN_VALUE,
						"An dynamicfield already exists with the name:" + name));
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
							"Copy field '" + entry + "' is not defined in schema"));
				}

				fieldAttribs.esc.addCopyField(name, entry);
			}

		}

		int properties = 0;
		if (AbstractFieldsResource.AddUpdateMode.REMOVE != mode) {
			try {
				properties = parseProperties(name, getFieldTypeProp(schema, params), params);
			} catch (ResourceException e1) {
				throw e1;
			} catch (RuntimeException e) {
				throw ErrorUtils.statusExp(422, new Error("", Error.E_EXCEPTION, e.getMessage()));
			}

		}

		String spellcheckField = "spell";
		if ((StringUtils.getBoolean(params.get(AbstractFieldAttributeReader.INDEX_FOR_SPELLCHECK)).booleanValue())
				&& (mode != AbstractFieldsResource.AddUpdateMode.REMOVE)) {
			SchemaField dstField = schema.getFieldOrNull(spellcheckField);
			if (dstField == null) {
				throw ErrorUtils.statusExp(422, new Error(AbstractFieldAttributeReader.INDEX_FOR_SPELLCHECK,
						Error.E_FORBIDDEN_VALUE, "Field '" + spellcheckField + "' is not defined in schema"));
			}

			fieldAttribs.esc.addCopyField(name, spellcheckField);
		} else {
			fieldAttribs.esc.removeCopyField(name, spellcheckField);
		}

		String autoCompleteField = "autocomplete";
		if ((StringUtils.getBoolean(params.get(AbstractFieldAttributeReader.INDEX_FOR_AUTOCOMPLETE)).booleanValue())
				&& (mode != AbstractFieldsResource.AddUpdateMode.REMOVE)) {
			SchemaField dstField = schema.getFieldOrNull(autoCompleteField);
			if (dstField == null) {
				throw ErrorUtils.statusExp(422, new Error(AbstractFieldAttributeReader.INDEX_FOR_AUTOCOMPLETE,
						Error.E_FORBIDDEN_VALUE, "Field '" + autoCompleteField + "' is not defined in schema"));
			}

			fieldAttribs.esc.addCopyField(name, autoCompleteField);
		} else {
			fieldAttribs.esc.removeCopyField(name, autoCompleteField);
		}

		if (mode != AbstractFieldsResource.AddUpdateMode.REMOVE) {
			FieldType fieldType = getFieldTypeProp(schema, params);

			SchemaField field = new SchemaField(name, fieldType, properties, null);

			fieldAttribs.updatedFields.put(name, field);
		}

		if (AbstractFieldsResource.AddUpdateMode.REMOVE == mode) {
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

	static List<Error> validateParams(Map<String, Object> params, AbstractFieldsResource.AddUpdateMode mode) {
		if (AbstractFieldsResource.AddUpdateMode.REMOVE == mode)
			return Collections.emptyList();

		List<Error> errors = new ArrayList<Error>(5);

		if (mode == AbstractFieldsResource.AddUpdateMode.NEW) {
			String name = getNameProp(params);
			Matcher m = VALID_DYN_FIELD_NAME.matcher(name);
			if (!m.matches()) {
				errors.add(new Error("name", Error.E_INVALID_VALUE,
						"name must consist of only A-Z a-z 0-9 - _ and either begin or end (but not both) with *"));
			}

		}

		validateCommonParams(params, mode, errors);

		return errors;
	}

	@Get("json")
	public List<Map<String, Object>> retrieve() {
		if (!isExisting()) {
			throw ErrorUtils.statusExp(Response.Status.NOT_FOUND, "URI not found");
		}

		getAttributeReader();
		Map<String, SchemaField> allDynFields = DynamicFieldAttributeReader.getDynamicFieldPrototypes(solrCore
				.getLatestSchema());

		List<Map<String, Object>> results = new ArrayList<Map<String, Object>>(allDynFields.size());
		for (String name : allDynFields.keySet()) {
			results.add(getAttributeReader().getAttributes(name));
		}

		return results;
	}

	@Delete("json")
	public void remove() throws IOException, ParserConfigurationException, SAXException, SolrServerException,
			SchedulerException {
		if (!isExisting()) {
			throw ErrorUtils.statusExp(Response.Status.NOT_FOUND, "URI not found");
		}

		DynamicFieldAttribs attribs = new DynamicFieldAttribs(cores, solrCore);

		String[] names = getQuery().getValuesArray("name");

		for (String name : names) {
			addOrUpdateDynamicField(cm, cores, solrCore, collection, name, Collections.<String, Object> emptyMap(),
					Collections.<String> emptySet(), AbstractFieldsResource.AddUpdateMode.REMOVE, null, attribs);

			AuditLogger.log("removed dynamic field");
		}

		attribs.save();

		APIUtils.reloadCore(collection, cores);
		setStatus(Status.SUCCESS_NO_CONTENT);
	}

	static {
		Set<String> tmpN = new HashSet<String>(COMMON_KEYS);
		VALID_KEYS_NEW = Collections.unmodifiableSet(tmpN);

		Set<String> tmpU = new HashSet<String>(VALID_KEYS_NEW);
		tmpU.remove("name");
		VALID_KEYS_UPDATE = Collections.unmodifiableSet(tmpU);
	}
}
