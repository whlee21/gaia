package gaia.api;

import gaia.admin.collection.CollectionManager;
import gaia.admin.editor.EditableSchemaConfig;
import gaia.admin.editor.EditableSolrConfig;
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

import javax.ws.rs.core.Response;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.core.CoreContainer;
import org.apache.solr.core.SolrCore;
import org.apache.solr.schema.FieldType;
import org.json.JSONException;
import org.quartz.SchedulerException;
import org.restlet.data.Form;
import org.restlet.data.Parameter;
import org.restlet.data.Status;
import org.restlet.resource.Get;
import org.restlet.resource.Put;
import org.restlet.resource.ServerResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import com.google.inject.Inject;

public class SettingsServerResource extends ServerResource implements SettingsResource {
	static final String SELF = "self";
	static final String SERVER_LIST = "server_list";
	static final String CLICK_TERMS = "_terms";
	private static final String ENABLE_SSL_KEY = "ssl";
	private static transient Logger log = LoggerFactory.getLogger(SettingsServerResource.class);

	private SettingsReader settingsReader = new SettingsReader();
	private String collection;
	private SolrCore solrCore;
	private CoreContainer cores;
	private CollectionManager cm;

	@Inject
	public SettingsServerResource(CollectionManager cm, CoreContainer cores) {
		this.cores = cores;
		this.cm = cm;
	}

	public void doInit() {
		collection = ((String) getRequestAttributes().get("coll_name"));
		solrCore = cores.getCore(collection);
		setExisting(solrCore != null);
	}

	public void doRelease() {
		if (solrCore != null)
			solrCore.close();
	}

	@Put("json")
	public void update(Map<String, Object> m) throws IOException, ParserConfigurationException, SAXException,
			SolrServerException, JSONException, SchedulerException {
		if (!isExisting()) {
			throw ErrorUtils.statusExp(Response.Status.NOT_FOUND, "URI not found");
		}

		Form form = getRequest().getResourceRef().getQueryAsForm();
		if (((m == null) || (m.size() == 0)) && ((form == null) || (form.size() == 0))) {
			throw ErrorUtils.statusExp(422, "No content found in request");
		}

		if (m == null) {
			m = new HashMap<String, Object>();
		}

		for (Parameter parameter : form) {
			m.put(parameter.getName(), parameter.getValue());
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
			log.info("Enabling SSL is no longer possible through collection settings");
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
		setStatus(Status.SUCCESS_NO_CONTENT);
	}

	@Get("json")
	public Map<String, Object> retrieve() throws IOException {
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

		String requests = (String) getRequestAttributes().get("name");
		if (requests != null) {
			Set<String> settingsRequested = new HashSet<String>();
			settingsRequested.addAll(Arrays.asList(requests.split(",")));

			for (String requested : settingsRequested) {
				if (!settings.containsKey(requested)) {
					throw ErrorUtils.statusExp(Response.Status.NOT_FOUND, "settings " + requested + " not found");
				}
			}

			Map<String, Object> filtered = new HashMap<String, Object>();
			for (Map.Entry<String, Object> entry : settings.entrySet()) {
				if (settingsRequested.contains(entry.getKey())) {
					filtered.put(entry.getKey(), entry.getValue());
				}
			}
			settings = filtered;
		}

		return settings;
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
				String baseUrl = MasterConfUtil.getGaiaSearchAddress() + "/api/collections/" + collection + "/dynamicfields";
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
				log.error("Could not update dynamic field '*':", e);
				throw ErrorUtils.statusExp(Response.Status.INTERNAL_SERVER_ERROR,
						"Could not update dynamic field '*':" + e.getMessage());
			}
		}
	}
}
