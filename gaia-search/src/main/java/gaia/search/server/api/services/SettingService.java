package gaia.search.server.api.services;

import gaia.admin.collection.CollectionManager;
import gaia.admin.editor.EditableSchemaConfig;
import gaia.admin.editor.EditableSolrConfig;
import gaia.api.APIUtils;
import gaia.api.AbstractFieldAttributeReader;
import gaia.api.AuditLogger;
import gaia.api.Error;
import gaia.api.ErrorUtils;
import gaia.api.FieldAttribs;
import gaia.api.ObjectSerializer;
import gaia.api.SettingsReader;
import gaia.api.SettingsWriter;
import gaia.search.server.api.services.parsers.BodyParseException;
import gaia.search.server.api.services.parsers.RequestBodyParser;
import gaia.search.server.configuration.Configuration;
import gaia.utils.MasterConfUtil;
import gaia.utils.ReSTClient;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.core.CoreContainer;
import org.apache.solr.core.SolrCore;
import org.apache.solr.schema.FieldType;
import org.json.JSONException;
import org.quartz.SchedulerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;
import org.xnap.commons.i18n.I18nFactory;

public class SettingService   extends BaseService {

	private static org.xnap.commons.i18n.I18n i18n = I18nFactory.getI18n(SettingService.class);
	private static final Logger LOG = LoggerFactory.getLogger(SettingService.class);
	static final String SELF = "self";
	static final String SERVER_LIST = "server_list";
	static final String CLICK_TERMS = "_terms";
	private static final String ENABLE_SSL_KEY = "ssl";
	
	
	private SettingsReader settingsReader = new SettingsReader();
	private String collection;
	private SolrCore solrCore;
	private CoreContainer cores;
	private CollectionManager cm;
	private Configuration configuration;
	private volatile boolean existing = true;
	
	public SettingService(ObjectSerializer serializer, RequestBodyParser bodyParser, String collectionName,
			CollectionManager cm, CoreContainer cores, Configuration configuration) {
		super(serializer, bodyParser);
		this.collection = collectionName;
		this.cm = cm;
		this.cores = cores;
		this.configuration = configuration;
		
		solrCore = cores.getCore(collection);
		setExisting(solrCore != null);
	}
	

	public void setExisting(boolean exists) {
		this.existing = exists;
	}
	public boolean isExisting() {
		return this.existing;
	}
	
	public Response getSettings() throws IOException {
		if (!isExisting()) {
			throw ErrorUtils.statusExp(Response.Status.NOT_FOUND, "URI not found");
		}
		String gaiaReqHandler = cm.getGaiaSearchHandler();

		Map<String, Object> settings = new HashMap<String, Object>();

		ModifiableSolrParams gaiaHandlerParams = FieldAttribs.getGaiaParams(solrCore);

		EditableSolrConfig ecc = new EditableSolrConfig(solrCore, cm.getUpdateChain(), cores.getZkController());
		EditableSchemaConfig esc = new EditableSchemaConfig(solrCore, cores.getZkController());
		settingsReader.getDedupeMode(cm, solrCore, settings);
		settingsReader.getDefaultSort(settings, gaiaHandlerParams);
		settingsReader.getDisplayFacets(settings, gaiaHandlerParams);
		settingsReader.getQueryParser(settings, gaiaHandlerParams);
		settingsReader.getShowFindSimiliarLinks(settings, gaiaHandlerParams);
		settingsReader.getUnknownTypeHandling(solrCore, settings);
		settingsReader.getBoostRecentDocs(settings, gaiaHandlerParams);
		settingsReader.getBoosts(settings, gaiaHandlerParams);
		settingsReader.getQueryStopwordsEnabled(settings, gaiaHandlerParams);
		settingsReader.getSynonymsEnabled(settings, gaiaHandlerParams);
		settingsReader.getUnsupervisedFeedback(solrCore, settings, gaiaReqHandler, gaiaHandlerParams);
		settingsReader.getUnsupervisedFeedbackEmphasis(settings, gaiaHandlerParams);
		settingsReader.getUpdateServerList(cm, solrCore, settings);
		settingsReader.getSearchServerList(settings, gaiaHandlerParams);
		settingsReader.getSynonymsList(solrCore, settings, esc);
		settingsReader.getBoostDataLocation(solrCore, settings);
		settingsReader.getBoostField(solrCore, settings);
		settingsReader.getClickEnabled(solrCore, settings);
		settingsReader.getUpdateHandlerSettings(ecc, settings);
		settingsReader.getIndexSettings(solrCore, settings);
		settingsReader.getStopwordList(solrCore, settings, esc);
		settingsReader.getAutocompleteEnabled(solrCore, settings, gaiaHandlerParams);
		settingsReader.getSpellCheckEnabled(solrCore, settings, gaiaReqHandler, gaiaHandlerParams);
		settingsReader.getElevations(solrCore, settings, cores.getZkController());
		settingsReader.getDisplayFields(settings, gaiaHandlerParams);

		

		return buildResponse(Response.Status.OK, settings); 
	}
	@GET
	@Produces("text/plain")
	public Response getSettings(@Context HttpHeaders headers, @Context UriInfo ui) throws IOException {
		LOG.debug("hhokyung getSettings (collectionName) = (" + collection +")");

		return getSettings();
	}

	
	public Response getSetting(String settingName) throws IOException {
		if (!isExisting()) {
			throw ErrorUtils.statusExp(Response.Status.NOT_FOUND, "URI not found");
		}
		String gaiaReqHandler = cm.getGaiaSearchHandler();

		Map<String, Object> settings = new HashMap<String, Object>();

		ModifiableSolrParams gaiaHandlerParams = FieldAttribs.getGaiaParams(solrCore);

		EditableSolrConfig ecc = new EditableSolrConfig(solrCore, cm.getUpdateChain(), cores.getZkController());
		EditableSchemaConfig esc = new EditableSchemaConfig(solrCore, cores.getZkController());
		settingsReader.getDedupeMode(cm, solrCore, settings);
		settingsReader.getDefaultSort(settings, gaiaHandlerParams);
		settingsReader.getDisplayFacets(settings, gaiaHandlerParams);
		settingsReader.getQueryParser(settings, gaiaHandlerParams);
		settingsReader.getShowFindSimiliarLinks(settings, gaiaHandlerParams);
		settingsReader.getUnknownTypeHandling(solrCore, settings);
		settingsReader.getBoostRecentDocs(settings, gaiaHandlerParams);
		settingsReader.getBoosts(settings, gaiaHandlerParams);
		settingsReader.getQueryStopwordsEnabled(settings, gaiaHandlerParams);
		settingsReader.getSynonymsEnabled(settings, gaiaHandlerParams);
		settingsReader.getUnsupervisedFeedback(solrCore, settings, gaiaReqHandler, gaiaHandlerParams);
		settingsReader.getUnsupervisedFeedbackEmphasis(settings, gaiaHandlerParams);
		settingsReader.getUpdateServerList(cm, solrCore, settings);
		settingsReader.getSearchServerList(settings, gaiaHandlerParams);
		settingsReader.getSynonymsList(solrCore, settings, esc);
		settingsReader.getBoostDataLocation(solrCore, settings);
		settingsReader.getBoostField(solrCore, settings);
		settingsReader.getClickEnabled(solrCore, settings);
		settingsReader.getUpdateHandlerSettings(ecc, settings);
		settingsReader.getIndexSettings(solrCore, settings);
		settingsReader.getStopwordList(solrCore, settings, esc);
		settingsReader.getAutocompleteEnabled(solrCore, settings, gaiaHandlerParams);
		settingsReader.getSpellCheckEnabled(solrCore, settings, gaiaReqHandler, gaiaHandlerParams);
		settingsReader.getElevations(solrCore, settings, cores.getZkController());
		settingsReader.getDisplayFields(settings, gaiaHandlerParams);

		String requests = settingName;
		LOG.debug("request: "+settingName);
		if (requests != null) {
			Set<String> settingsRequested = new HashSet<String>();
			settingsRequested.addAll(Arrays.asList(requests.split(",")));

			for (String requested : settingsRequested) {

				LOG.debug("settingsRequested requested: "+requested);
				if (!settings.containsKey(requested)) {
					throw ErrorUtils.statusExp(Response.Status.NOT_FOUND, "settings " + requested + " not found");
				}
			}

			Map<String, Object> filtered = new HashMap<String, Object>();
			for (Map.Entry<String, Object> entry : settings.entrySet()) {

				LOG.debug("settings name: "+entry.getKey());
				LOG.debug("settingsRequested name: "+settingsRequested);
				
				if (settingsRequested.contains(entry.getKey())) {
					LOG.debug("entry.getKey(): "+entry.getKey()+" entry.getValue():"+entry.getValue());
					filtered.put(entry.getKey(), entry.getValue());
				}
			}
			LOG.debug("filtered : "+filtered.size());
			settings = filtered;
		}

		LOG.debug("settings.size: "+settings.size());

		return buildResponse(Response.Status.OK, settings); 
	}
	
	@GET
	@Path("{settingName}")
	@Produces("text/plain")
	public Response getSetting(@Context HttpHeaders headers, @Context UriInfo ui,
			@PathParam("settingName") String settingName) throws IOException {
		LOG.debug("hhokyung getSetting (collectionName, settingName) = (" + collection + ", " + settingName + ")");
//		return handleRequest(headers, null, ui, Request.Type.GET, createSettingResource(collection, settingName));
		
		return getSetting(settingName);
	}
	
	@PUT
	@Produces("text/plain")
	public Response updateSetting(String body, @Context HttpHeaders headers, @Context UriInfo ui) throws IOException, 
					ParserConfigurationException, SAXException, SolrServerException, JSONException, SchedulerException {
		LOG.debug("hhokyung getSetting (collection) = (" + collection + ")");
//		return handleRequest(headers, body, ui, Request.Type.PUT, createSettingResource(collection, null));
		try {
			LOG.debug("doby: "+body);
			RequestBody requestBody = getRequestBody(body);
			return updateSetting(requestBody.getProperties());
		} catch (BodyParseException e) {
			throw ErrorUtils.statusExp(Response.Status.INTERNAL_SERVER_ERROR, new Error("body", Error.E_EXCEPTION,
					"cannot parse body " + body));
		}
	}
	
	public Response updateSetting(Map<String, Object> m) throws IOException, ParserConfigurationException, SAXException,
	SolrServerException, JSONException, SchedulerException {
		if (!isExisting()) {
			throw ErrorUtils.statusExp(Response.Status.NOT_FOUND, "URI not found");
		}
		
		for (Map.Entry<String, Object> entry : m.entrySet()) {

			LOG.debug("settings name: "+entry.getKey());
			
			LOG.debug("entry.getKey(): "+entry.getKey()+" entry.getValue():"+entry.getValue());
		}
		
		// FIXME: by whlee21
//		Form form = null; //getRequest().getResourceRef().getQueryAsForm();
//		if (((m == null) || (m.size() == 0)) && ((form == null) || (form.size() == 0))) {
//			throw ErrorUtils.statusExp(422, "No content found in request");
//		}
		
		if (m == null) {
			m = new HashMap<String, Object>();
		}
		
		setUnknownType(m);
		
		ModifiableSolrParams gaiaHandlerParams = FieldAttribs.getGaiaParams(solrCore);
		
		EditableSolrConfig ecc = new EditableSolrConfig(solrCore, cm.getUpdateChain(), cores.getZkController());
		EditableSchemaConfig esc = new EditableSchemaConfig(solrCore, cores.getZkController());
		
		List<String> qfFieldsBoosts = new ArrayList<String>();
		
		SettingsWriter settingsWriter = new SettingsWriter(cm, solrCore, m, ecc, esc, gaiaHandlerParams, qfFieldsBoosts);
		
		settingsWriter.setDisplayFields();
		
		settingsWriter.setClickSettings();
		settingsWriter.setUpdateHandlerSettings();
		settingsWriter.setIndexSettings();
		settingsWriter.setDefaultSort();
		settingsWriter.setBoosts();
		settingsWriter.setBoostRecent();
		settingsWriter.setDisplayFacets();
		settingsWriter.setEnableAutocomplete();
		settingsWriter.setEnableSpellcheck();
		settingsWriter.setQueryParser();
		settingsWriter.setQueryStopWordsEnabled();
		settingsWriter.setStopWordsList();
		settingsWriter.setDedupe();
		settingsWriter.setSynonyms();
		settingsWriter.setUnsupervisedFeedback();
		settingsWriter.setUnsupervisedFeedbackEmphasis();
		settingsWriter.setShowFindSimiliar();
		settingsWriter.setSearchServerList();
		settingsWriter.setUpdateServerSettings();
		settingsWriter.setSynonymsList();
		settingsWriter.setElevations(cores.getZkController());
		
		if (m.containsKey(ENABLE_SSL_KEY)) {
			LOG.info("Enabling SSL is no longer possible through collection settings");
			m.remove(ENABLE_SSL_KEY);
		}
		
		List<Error> errors = new ArrayList<Error>();
		
		Set<String> keys = m.keySet();
		for (String key : keys) {
			errors.add(new Error(key, Error.E_FORBIDDEN_KEY, "Unknown or dissallowed key found:" + key));
		}
		
		if (errors.size() > 0) {
			throw ErrorUtils.statusExp(422, errors);
		}
		
		AuditLogger.log("updated settings");
		
		settingsWriter.save();
		
		APIUtils.reloadCore(collection, cores);
		return buildResponse(Response.Status.NO_CONTENT);
	}
	


	private void setUnknownType(Map<String, Object> m) {
		if (!m.containsKey(SettingsReader.UNKNOWN_TYPE_HANDLING_KEY)) {
			return;
		}
		String type = (String) m.remove(SettingsReader.UNKNOWN_TYPE_HANDLING_KEY);

		if (null != type) {
			FieldType fieldType = (FieldType) solrCore.getLatestSchema().getFieldTypes().get(type);
			if (fieldType == null) {
				throw ErrorUtils.statusExp(422, new Error(SettingsReader.UNKNOWN_TYPE_HANDLING_KEY, Error.E_INVALID_VALUE,
						"unknown_type_handling is not a valid field type:" + type));
			}

		}

		Map<String, Object> map = new HashMap<String, Object>();
		settingsReader.getUnknownTypeHandling(solrCore, map);
		String currentType = (String) map.get(SettingsReader.UNKNOWN_TYPE_HANDLING_KEY);

		if ((currentType != null) || (type != null)) {
			try {
				String baseUrl = MasterConfUtil.getGaiaSearchAddress() + "/api/v1/collections/" + collection + "/dynamicfields";
				if ((currentType != null) && (type == null)) {
					ReSTClient.deleteUrl(baseUrl + "/*");
				} else if ((currentType == null) && (type != null)) {
					Map<String, Object> data = new HashMap<String, Object>();
					data.put(AbstractFieldAttributeReader.NAME, "*");
					data.put(AbstractFieldAttributeReader.STORED, Boolean.valueOf(true));
					data.put(AbstractFieldAttributeReader.MULTI_VALUED, Boolean.valueOf(true));
					data.put(AbstractFieldAttributeReader.TYPE_NAME, type);
					data.put(AbstractFieldAttributeReader.INDEXED, Boolean.valueOf(true));
					ReSTClient.postUrl(baseUrl, data);
				} else {
					Map<String, Object> data = new HashMap<String, Object>();
					data.put(AbstractFieldAttributeReader.TYPE_NAME, type);
					ReSTClient.putUrl(baseUrl + "/*", data);
				}
			} catch (Exception e) {
				LOG.error("Could not update dynamic field '*':", e);
				throw ErrorUtils.statusExp(Response.Status.INTERNAL_SERVER_ERROR,
						"Could not update dynamic field '*':" + e.getMessage());
			}
		}
	}

}
