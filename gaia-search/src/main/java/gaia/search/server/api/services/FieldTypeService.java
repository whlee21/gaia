package gaia.search.server.api.services;

import gaia.admin.collection.CollectionManager;
import gaia.admin.editor.EditableSchemaConfig;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.util.List;
import java.util.Map;

import gaia.api.APIUtils;
import gaia.api.Error;
import gaia.api.ErrorUtils;
import gaia.api.ObjectSerializer;
import gaia.search.server.api.services.parsers.BodyParseException;
import gaia.search.server.api.services.parsers.RequestBodyParser;
import gaia.search.server.configuration.Configuration;

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

import org.apache.solr.core.CoreContainer;
import org.apache.solr.core.SolrCore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;
import org.xnap.commons.i18n.I18nFactory;

public class FieldTypeService  extends BaseService {
	
	private static org.xnap.commons.i18n.I18n i18n = I18nFactory.getI18n(FieldTypeService.class);
	private static final Logger LOG = LoggerFactory.getLogger(FieldTypeService.class);
	private CoreContainer cores;
	private CollectionManager cm;
	private String collection;
	private EditableSchemaConfig esc;
	private SolrCore solrCore;
	private String typeName;
	private Map<String, Object> type;
	private volatile boolean existing = true;
	private Configuration configuration;

	//public FieldTypeService(String collection, CollectionManagecollectionainer cores) {
	public FieldTypeService(ObjectSerializer serializer, RequestBodyParser bodyParser, String collectionName, CollectionManager cm
									, CoreContainer cores, Configuration configuration) {
		super(serializer, bodyParser);
		this.collection = collectionName;
		this.cm = cm;
		this.cores = cores;
		this.configuration = configuration;
		
		this.solrCore = this.cores.getCore(this.collection);
		this.esc = new EditableSchemaConfig(this.solrCore, this.cores.getZkController());
		

		setExisting(null != this.solrCore);
	}

	public void setExisting(boolean exists) {
		this.existing = exists;
	}
	
	public boolean isExisting() {
		return this.existing;
	}
	
	@GET
	@Produces("text/plain")
	public Response getFieldTypes(@Context HttpHeaders headers, @Context UriInfo ui) {


		if (!isExisting())
			return null;

		LOG.debug("#### getFieldTypes ..");
//		return handleRequest(headers, null, ui, Request.Type.GET, createFieldTypeResource(collectionName, null));
		List<Map<String, Object>> fieldTypes = this.esc.getFieldTypes();
		LOG.debug("#### getFieldTypes ..fieldTypes:"+fieldTypes);
		LOG.debug("#### getFieldTypes ..fieldTypes:"+fieldTypes.size());
		return buildResponse(Response.Status.OK, fieldTypes);
	}

	@GET
	@Path("{fieldtype}")
	@Produces("text/plain")
	public Response getFieldType(@Context HttpHeaders headers, @Context UriInfo ui,
			@PathParam("fieldtype") String fieldType) {
		LOG.debug("whlee21 getFieldType (collectionName, fieldType) = (" + collection+ ", " + fieldType + ")");
		
		this.typeName = fieldType;
		
		this.type = this.esc.getFieldType(this.typeName);
		LOG.debug("#### getFieldTypes ..this.type: "+this.type);
		
		return buildResponse(Response.Status.OK, this.type);
	}

	private Response createFieldType(Map<String, Object> m) throws Exception {
		if (!isExisting()) {
			throw ErrorUtils.statusExp(Response.Status.NOT_FOUND, i18n.tr("URI not found"));
		}

		if (!m.containsKey("name")) {
			throw ErrorUtils.statusExp(422, new Error("name", Error.E_MISSING_VALUE, i18n.tr("name must be specified")));
		}

		if (!m.containsKey("class")) {
			throw ErrorUtils.statusExp(422, new Error("name", Error.E_MISSING_VALUE, i18n.tr("class must be specified")));
		}

		String name = gaia.utils.StringUtils.getString(m.get("name"));
		String clazz = gaia.utils.StringUtils.getString(m.get("class"));

		if (org.apache.commons.lang.StringUtils.isBlank(name)) {
			throw ErrorUtils.statusExp(422, new Error("name", Error.E_EMPTY_VALUE, i18n.tr("A non-blank name must be specified")));
		}

		if (org.apache.commons.lang.StringUtils.isBlank(clazz)) {
			throw ErrorUtils.statusExp(422, new Error("name", Error.E_EMPTY_VALUE, i18n.tr("A non-blank class must be specified")));
		}

		if (null != esc.getFieldType(name)) {
			throw ErrorUtils.statusExp(422, new Error("name", Error.E_EXISTS,
					i18n.tr("An explicit field type already exists with the name:" + name)));
		}

		try {
			esc.addFieldType(m);
		} catch (IllegalArgumentException userError) {
			throw ErrorUtils.statusExp(422, new Error("", Error.E_EXCEPTION, i18n.tr(userError.getMessage())));
		}

		try {
			esc.save();
		} catch (IOException e) {
			throw ErrorUtils.statusExp(e);
		}

		APIUtils.reloadCore(collection, cores);

		Map<String, Object> data = esc.getFieldType(name);
		
		URI seeOther = configuration.getCollectionUri(collection + "/fieldtypes/" + URLEncoder.encode(name, "UTF-8"));
		Response response = buildResponse(Response.Status.CREATED, seeOther, data);
		
		return response;
	}
	@POST
	@Produces("text/plain")
	public Response createFieldType(String body, @Context HttpHeaders headers, @Context UriInfo ui) throws Exception {
//		return handleRequest(headers, body, ui, Request.Type.POST, createFieldTypeResource(collection, null));
		try {
			RequestBody requestBody = getRequestBody(body);
			return createFieldType(requestBody.getProperties());
		} catch (BodyParseException e) {
			throw ErrorUtils.statusExp(Response.Status.INTERNAL_SERVER_ERROR, new Error("body", Error.E_EXCEPTION,
					i18n.tr("cannot parse body " + body)));
		}
	}

	
	private Response updateFieldType(Map<String, Object> m) throws Exception {
		
		if (!isExisting()) {
			throw ErrorUtils.statusExp(Response.Status.NOT_FOUND, i18n.tr("URI not found"));
		}
		if (m.size() == 0) {
			throw ErrorUtils.statusExp(422, i18n.tr("No input content found"));
		}

		for (Map.Entry<String, Object> pair : m.entrySet()) {
			if ("name".equals(pair.getKey())) {
				throw ErrorUtils.statusExp(422, new Error("name", Error.E_FORBIDDEN_KEY,
						i18n.tr("The name of a FieldType can not be modified")));
			}

			if (null == pair.getValue()) {
				this.type.remove(pair.getKey());
			} else {
				LOG.debug("key: "+pair.getKey()+" value: "+pair.getValue());
				this.type.put(pair.getKey(), pair.getValue());
			}
		}

		try {
			esc.addFieldType(type);
		} catch (IllegalArgumentException userError) {
			throw ErrorUtils.statusExp(422, new Error("", Error.E_EXCEPTION, i18n.tr(userError.getMessage())));
		}

		try {
			esc.save();
		} catch (IOException e) {
			throw ErrorUtils.statusExp(e);
		}

		APIUtils.reloadCore(collection, cores);
		
		return buildResponse(Response.Status.NO_CONTENT);
		
	}
	@PUT
	@Path("{fieldtype}")
	@Produces("text/plain")
	public Response updateFieldType(String body, @Context HttpHeaders headers, @Context UriInfo ui,
			@PathParam("fieldtype") String fieldType) throws Exception {

		try {
			RequestBody requestBody = getRequestBody(body);
			
			LOG.debug("#### fieldType: "+fieldType);

			type = esc.getFieldType(fieldType);

			setExisting(null != type);
			
			return updateFieldType(requestBody.getProperties());
		} catch (BodyParseException e) {
			throw ErrorUtils.statusExp(Response.Status.INTERNAL_SERVER_ERROR, new Error("body", Error.E_EXCEPTION,
					i18n.tr("cannot parse body " + body)));
		} 
		
	}

	@DELETE
	@Path("{fieldtype}")
	@Produces("text/plain")
	public Response deleteFieldType(@Context HttpHeaders headers, @Context UriInfo ui,
			@PathParam("fieldtype") String fieldType) throws ParserConfigurationException, IOException, SAXException {
		
		if (!isExisting()) {
			throw ErrorUtils.statusExp(Response.Status.NOT_FOUND, i18n.tr("URI not found"));
		}

		if (esc.isFieldTypeInUse(typeName)) {
			throw ErrorUtils.statusExp(Response.Status.CONFLICT, new Error("name", Error.E_INVALID_VALUE,
					i18n.tr("Field Type currently in use: " + typeName)));
		}

		esc.removeFieldType(typeName);
		try {
			esc.save();
		} catch (IOException e) {
			throw ErrorUtils.statusExp(e);
		}

		APIUtils.reloadCore(collection, cores);
		
		return buildResponse(Response.Status.NO_CONTENT);
	}

}
