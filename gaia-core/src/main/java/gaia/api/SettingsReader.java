package gaia.api;

import gaia.admin.collection.CollectionManager;
import gaia.admin.editor.EditableSchemaConfig;
import gaia.admin.editor.EditableSolrConfig;
import gaia.admin.editor.ElevateEditor;
import gaia.feedback.FeedbackComponent;
import gaia.solr.click.ClickDeletionPolicy;
import gaia.solr.click.ClickIndexReaderFactory;
import gaia.update.DistributedUpdateProcessorFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.Response;

import org.apache.solr.cloud.ZkController;
import org.apache.solr.common.SolrException;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.core.IndexReaderFactory;
import org.apache.solr.core.SolrCore;
import org.apache.solr.handler.StandardRequestHandler;
import org.apache.solr.handler.component.SearchComponent;
import org.apache.solr.handler.component.SpellCheckComponent;
import org.apache.solr.request.SolrRequestHandler;
import org.apache.solr.schema.IndexSchema;
import org.apache.solr.schema.SchemaField;
import org.apache.solr.search.LuceneQParserPlugin;
import org.apache.solr.update.SolrIndexConfig;
import org.apache.solr.update.processor.SignatureUpdateProcessorFactory;
import org.apache.solr.update.processor.UpdateRequestProcessorChain;
import org.apache.solr.update.processor.UpdateRequestProcessorFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SettingsReader {
	private static transient Logger LOG = LoggerFactory.getLogger(SettingsReader.class);
	protected static final String RANDOM = "random";
	protected static final String DATE = "date";
	static final String RELEVANCE = "relevance";
	static final String SCORE_DESC = "score desc";
	static final String LAST_MODIFIED_DESC = "lastModified desc";
	static final String RANDOM_1_DESC = "random_1 desc";
	static final String DISPLAY_FIELDS_KEY = "display_fields";
	static final String SYNONYM_LIST_KEY = "synonym_list";
	static final String UPDATE_SERVER_SETTINGS_KEY = "update_server_list";
	static final String SEARCH_SERVER_LIST_KEY = "search_server_list";
	static final String DISPLAY_FACET_PARAM = "facet";
	static final String DISPLAY_FACET_KEY = "display_facets";
	static final String ENABLE_AUTOCOMPLETE_KEY = "auto_complete";
	static final String ENABLE_AUTOCOMPLETE_PARAM = "auto-complete";
	static final String ENABLE_SPELLCHECK_KEY = "spellcheck";
	static final String ENABLE_SPELLCHECK_PARAM = "spellcheck";
	static final String SHOW_FIND_SIMILAR_LINKS_PARAM = "showFindSimilarLinks";
	static final String SHOW_FIND_SIMILAR_LINKS_KEY = "show_similar";
	static final String BOOST_RECENT_PARAM = "boost";
	static final String BOOST_RECENT_KEY = "boost_recent";
	static final String BOOSTS_PARAM = "boost";
	static final String BOOSTS_KEY = "boosts";
	static final String QUERY_PARSER_PARAM = "defType";
	static final String QUERY_PARSER_KEY = "query_parser";
	static final String DEFAULT_SORT_PARAM = "sort";
	static final String DEFAULT_SORT_KEY = "default_sort";
	static final String QUERY_STOPWORDS_ENABLED_PARAM = "stopwords.enabled";
	static final String QUERY_STOPWORDS_ENABLED_KEY = "query_time_stopwords";
	static final String STOPWORDS_LIST_KEY = "stopword_list";
	static final String SYNONYMS_ENABLED_PARAM = "synonyms.enabled";
	static final String SYNONYMS_ENABLED_KEY = "query_time_synonyms";
	static final String DEDUPE_KEY = "de_duplication";
	public static final String UNKNOWN_TYPE_HANDLING_KEY = "unknown_type_handling";
	static final String UNSUPERVISED_FEEDBACK_KEY = "unsupervised_feedback";
	static final String UNSUPERVISED_FEEDBACK_PARAM = "feedback";
	static final String UNSUPERVISED_FEEDBACK_EMPHASIS_KEY = "unsupervised_feedback_emphasis";
	static final String UNSUPERVISED_FEEDBACK_EMPHASIS_PARAM = "feedback.emphasis";
	static final String UNSUPERVISED_FEEDBACK_EMPHASIS_PARAM_RELEVANCY_KEY = "relevancy";
	static final String UNSUPERVISED_FEEDBACK_EMPHASIS_PARAM_RECALL_KEY = "recall";
	static final String UNSUPERVISED_FEEDBACK_EMPHASIS_PARAM_DEFAULT_KEY = "relevancy";
	static final String SEARCH_SERVER_LIST_PARAM = "shards";
	static final String CLICK_ENABLED_KEY = "click_enabled";
	static final String CLICK_FIELD_PREFIX_KEY = "click_boost_field";
	static final String CLICK_BOOST_DATA_LOCATION_KEY = "click_boost_data";
	static final String UPDATE_HANDLER_AUTOCOMMIT_MAX_TIME = "update_handler_autocommit_max_time";
	static final String UPDATE_HANDLER_AUTOCOMMIT_MAX_DOCS = "update_handler_autocommit_max_docs";
	static final String UPDATE_HANDLER_AUTOCOMMIT_OPEN_SEARCHER = "update_handler_autocommit_open_searcher";
	static final String UPDATE_HANDLER_AUTOSOFTCOMMIT_MAX_TIME = "update_handler_autosoftcommit_max_time";
	static final String UPDATE_HANDLER_AUTOSOFTCOMMIT_MAX_DOCS = "update_handler_autosoftcommit_max_docs";
	static final Map<String, String> MAIN_INDEX_SETTINGS = new HashMap<String, String>();
	static final String BOOST_RECENT_EQ = "recip(rord(lastModified),1,1000,1000)";
	static final String ELEVATIONS_KEY = "elevations";
	private SignatureUpdateProcessorFactory sigFactory;
	private boolean dedupeInited;
	private DistributedUpdateProcessorFactory disFactory = null;
	private boolean distribUpdateInited;

	public void getBoostRecentDocs(Map<String, Object> settings, ModifiableSolrParams gaiaHandlerParams) {
		String[] params = gaiaHandlerParams.getParams(BOOST_RECENT_PARAM);
		boolean value = false;
		if (params != null) {
			for (String param : params) {
				if (param.equals(BOOST_RECENT_EQ)) {
					value = true;
					break;
				}
			}
		}
		settings.put(BOOST_RECENT_KEY, Boolean.valueOf(value));
	}

	public void getBoosts(Map<String, Object> settings, ModifiableSolrParams gaiaHandlerParams) {
		String[] params = gaiaHandlerParams.getParams(BOOSTS_PARAM);
		List<String> boosts = new ArrayList<String>();
		if (params != null) {
			for (String param : params) {
				boosts.add(param);
			}
		}
		settings.put(BOOSTS_KEY, boosts);
	}

	public void getClickEnabled(SolrCore solrCore, Map<String, Object> settings) {
		IndexReaderFactory readerFactory = solrCore.getIndexReaderFactory();
		if ((readerFactory instanceof ClickIndexReaderFactory)) {
			ClickIndexReaderFactory clickReaderFactory = (ClickIndexReaderFactory) readerFactory;
			Boolean enabled = Boolean.valueOf(clickReaderFactory.isEnabled());
			if (!(solrCore.getDeletionPolicy().getWrappedDeletionPolicy() instanceof ClickDeletionPolicy)) {
				LOG.warn("No special deletionPolicy 'ClickDeletionPolicy' in solrconfig - set 'click_enabled' api key to init correct default configuration");

				enabled = Boolean.valueOf(false);
			}
			if (solrCore.getRequestHandler("/click") == null) {
				LOG.warn("No special ClickAnalysisRequestHandler in solrconfig - set 'click_enabled' api key to init correct default configuration");

				enabled = Boolean.valueOf(false);
			}
			String clickFieldPrefix = clickReaderFactory.getBoostField();
			String clickFieldTerms = clickFieldPrefix + "_terms";
			String clickFieldVal = clickFieldPrefix + "_val";
			Map<String, SchemaField> staticFields = solrCore.getLatestSchema().getFields();
			if ((clickFieldPrefix == null) || (staticFields.get(clickFieldPrefix) == null)
					|| (staticFields.get(clickFieldTerms) == null) || (staticFields.get(clickFieldVal) == null)) {
				LOG.warn("No click fields defined in schema - set 'click_enabled' api key to init correct default configuration");

				enabled = Boolean.valueOf(false);
			}
			settings.put(CLICK_ENABLED_KEY, enabled);
		} else {
			settings.put(CLICK_ENABLED_KEY, Boolean.valueOf(false));
		}
	}

	public void getUpdateHandlerSettings(EditableSolrConfig ecc, Map<String, Object> settings) {
		EditableSolrConfig.UpdateHandlerSettings updateHandlerSettings = ecc.getUpdateHandlerSettings();
		settings.put(UPDATE_HANDLER_AUTOCOMMIT_MAX_TIME, updateHandlerSettings.autoCommitMaxTime);
		settings.put(UPDATE_HANDLER_AUTOCOMMIT_MAX_DOCS, updateHandlerSettings.autoCommitMaxDocs);
		settings.put(UPDATE_HANDLER_AUTOCOMMIT_OPEN_SEARCHER, updateHandlerSettings.autoCommitOpenSearcher);
		settings.put(UPDATE_HANDLER_AUTOSOFTCOMMIT_MAX_TIME, updateHandlerSettings.autoSoftCommitMaxTime);
		settings.put(UPDATE_HANDLER_AUTOSOFTCOMMIT_MAX_DOCS, updateHandlerSettings.autoSoftCommitMaxDocs);
	}

	public void getIndexSettings(SolrCore solrCore, Map<String, Object> settings) {
		SolrIndexConfig mainIndexConfig = solrCore.getSolrConfig().indexConfig;
		for (Map.Entry<String, String> entry : MAIN_INDEX_SETTINGS.entrySet()) {
			String key = (String) entry.getKey();
			String value = (String) entry.getValue();
			try {
				settings.put(key, mainIndexConfig.getClass().getField(value).get(mainIndexConfig));
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
	}

	public void getBoostField(SolrCore solrCore, Map<String, Object> settings) {
		IndexReaderFactory readerFactory = solrCore.getIndexReaderFactory();
		if ((readerFactory instanceof ClickIndexReaderFactory)) {
			ClickIndexReaderFactory clickReaderFactory = (ClickIndexReaderFactory) readerFactory;
			settings.put(CLICK_FIELD_PREFIX_KEY, clickReaderFactory.getBoostField());
		} else {
			settings.put(CLICK_FIELD_PREFIX_KEY, null);
		}
	}

	public void getBoostDataLocation(SolrCore solrCore, Map<String, Object> settings) {
		IndexReaderFactory readerFactory = solrCore.getIndexReaderFactory();
		if ((readerFactory instanceof ClickIndexReaderFactory)) {
			ClickIndexReaderFactory clickReaderFactory = (ClickIndexReaderFactory) readerFactory;
			settings.put(CLICK_BOOST_DATA_LOCATION_KEY, clickReaderFactory.getBoostData());
		} else {
			settings.put(CLICK_BOOST_DATA_LOCATION_KEY, null);
		}
	}

	public void getDedupeMode(CollectionManager cm, SolrCore solrCore, Map<String, Object> settings) {
		initDedupeFactory(cm, solrCore);

		boolean isSignatureFieldDefined = true;
		Map<String, SchemaField> staticFields = solrCore.getLatestSchema().getFields();
		String signatureFieldName = "signatureField";
		if (sigFactory != null) {
			signatureFieldName = sigFactory.getSignatureField();
		}
		SchemaField signatureField = (SchemaField) staticFields.get(signatureFieldName);
		if (signatureField == null) {
			if ((sigFactory != null) && (sigFactory.isEnabled())) {
				LOG.warn("No signature field deined in schema - reset 'de_duplication' api key to init correct default configuration");
			}
			isSignatureFieldDefined = false;
		}
		if ((signatureField != null) && (!signatureField.indexed())) {
			if ((sigFactory != null) && (sigFactory.isEnabled())) {
				LOG.warn("Signature field deined in schema marked as indexed=false - reset 'de_duplication' api key to init correct default configuration");
			}
			isSignatureFieldDefined = false;
		}

		if (sigFactory != null) {
			if ((sigFactory.isEnabled()) && ((sigFactory.getSigFields() == null) || (sigFactory.getSigFields().size() == 0))) {
				LOG.warn("No fields marked as use_for_deduplication");
			}
			if (sigFactory.getOverwriteDupes())
				settings.put(DEDUPE_KEY, "overwrite");
			else {
				settings.put(DEDUPE_KEY, "tag");
			}
		}

		if ((sigFactory == null) || (!sigFactory.isEnabled()) || (!isSignatureFieldDefined))
			settings.put(DEDUPE_KEY, "off");
	}

	public void getDefaultSort(Map<String, Object> settings, ModifiableSolrParams gaiaHandlerParams) {
		String sortValue = gaiaHandlerParams.get(DEFAULT_SORT_PARAM);
		if ((sortValue == null) || (sortValue.equals(SCORE_DESC)))
			sortValue = RELEVANCE;
		else if (sortValue.equals(LAST_MODIFIED_DESC))
			sortValue = DATE;
		else if (sortValue.equals(RANDOM_1_DESC))
			sortValue = RANDOM;
		else {
			throw new IllegalStateException("Invalid sort type found: " + sortValue);
		}
		settings.put(DEFAULT_SORT_KEY, sortValue);
	}

	public void getDisplayFacets(Map<String, Object> settings, ModifiableSolrParams gaiaHandlerParams) {
		settings.put(DISPLAY_FACET_KEY, Boolean.valueOf(getBooleanValue(gaiaHandlerParams, DISPLAY_FACET_PARAM)));
	}

	public void getAutocompleteEnabled(SolrCore solrCore, Map<String, Object> settings,
			ModifiableSolrParams gaiaHandlerParams) {
		boolean autocompleteEnabled = getBooleanValue(gaiaHandlerParams, ENABLE_AUTOCOMPLETE_PARAM);
		SolrRequestHandler requestHandler = solrCore.getRequestHandler("/autocomplete");
		if (requestHandler == null) {
			autocompleteEnabled = false;
		}

		settings.put(ENABLE_AUTOCOMPLETE_KEY, Boolean.valueOf(autocompleteEnabled));
	}

	public void getSpellCheckEnabled(SolrCore solrCore, Map<String, Object> settings, String gaiaReqHandlerName,
			ModifiableSolrParams gaiaHandlerParams) {
		boolean spellcheckEnabled = getBooleanValue(gaiaHandlerParams, ENABLE_SPELLCHECK_PARAM);

		boolean spellcheckCompPresent = isComponentRegistered(solrCore, gaiaReqHandlerName, SpellCheckComponent.class);
		if ((spellcheckEnabled) && (!spellcheckCompPresent)) {
			spellcheckEnabled = false;
		}

		settings.put(ENABLE_SPELLCHECK_KEY, Boolean.valueOf(spellcheckEnabled));
	}

	public void getDisplayFields(Map<String, Object> settings, ModifiableSolrParams gaiaHandlerParams) {
		settings.put(DISPLAY_FIELDS_KEY, FieldAttributeReader.parseFlParam(gaiaHandlerParams.getParams("fl")));
	}

	public void getQueryParser(Map<String, Object> settings, ModifiableSolrParams gaiaHandlerParams) {
		settings.put(QUERY_PARSER_KEY, gaiaHandlerParams.get(QUERY_PARSER_PARAM, LuceneQParserPlugin.NAME));
	}

	public void getSearchServerList(Map<String, Object> settings, ModifiableSolrParams gaiaHandlerParams) {
		String shardString = gaiaHandlerParams.get(SEARCH_SERVER_LIST_PARAM);
		List<String> shards;
		if ((shardString != null) && (shardString.trim().length() > 0))
			shards = Arrays.asList(shardString.split(","));
		else {
			shards = Collections.emptyList();
		}

		settings.put(SEARCH_SERVER_LIST_KEY, shards);
	}

	public void getShowFindSimiliarLinks(Map<String, Object> settings, ModifiableSolrParams gaiaHandlerParams) {
		settings.put(SHOW_FIND_SIMILAR_LINKS_KEY,
				Boolean.valueOf(getBooleanValue(gaiaHandlerParams, SHOW_FIND_SIMILAR_LINKS_PARAM)));
	}

	public void getQueryStopwordsEnabled(Map<String, Object> settings, ModifiableSolrParams gaiaHandlerParams) {
		settings.put(QUERY_STOPWORDS_ENABLED_KEY,
				Boolean.valueOf(getBooleanValue(gaiaHandlerParams, QUERY_STOPWORDS_ENABLED_PARAM)));
	}

	public void getStopwordList(SolrCore solrCore, Map<String, Object> settings, EditableSchemaConfig ecc)
			throws IOException {
		List<String> lines = ecc.getLinesFromConfigFile("stopwords.txt", true);

		settings.put(STOPWORDS_LIST_KEY, lines);
	}

	public void getUnsupervisedFeedback(SolrCore solrCore, Map<String, Object> settings, String gaiaReqHandlerName,
			ModifiableSolrParams gaiaHandlerParams) {
		boolean feedbackEnabled = getBooleanValue(gaiaHandlerParams, UNSUPERVISED_FEEDBACK_PARAM);
		if (feedbackEnabled) {
			boolean feedbackCompRegistered = isComponentRegistered(solrCore, gaiaReqHandlerName, FeedbackComponent.class);
			if (!feedbackCompRegistered) {
				feedbackEnabled = false;
			}
		}
		settings.put(UNSUPERVISED_FEEDBACK_KEY, Boolean.valueOf(feedbackEnabled));
	}

	public void getUnsupervisedFeedbackEmphasis(Map<String, Object> settings, ModifiableSolrParams gaiaHandlerParams) {
		settings.put(UNSUPERVISED_FEEDBACK_EMPHASIS_KEY,
				getStringValue(gaiaHandlerParams, UNSUPERVISED_FEEDBACK_EMPHASIS_PARAM, "relevancy"));
	}

	public void getSynonymsEnabled(Map<String, Object> settings, ModifiableSolrParams gaiaHandlerParams) {
		settings.put(SYNONYMS_ENABLED_KEY, Boolean.valueOf(getBooleanValue(gaiaHandlerParams, SYNONYMS_ENABLED_PARAM)));
	}

	public void getSynonymsList(SolrCore solrCore, Map<String, Object> settings, EditableSchemaConfig ecc)
			throws IOException {
		List<String> lines = ecc.getLinesFromConfigFile("synonyms.txt", true);

		settings.put(SYNONYM_LIST_KEY, lines);
	}

	public void getUnknownTypeHandling(SolrCore solrCore, Map<String, Object> settings) {
		IndexSchema schema = solrCore.getLatestSchema();

		String fieldTypeName = null;
		try {
			fieldTypeName = schema.getDynamicFieldType("*").getTypeName();
		} catch (SolrException e) {
		}
		settings.put(UNKNOWN_TYPE_HANDLING_KEY, fieldTypeName);
	}

	public void getUpdateServerList(CollectionManager cm, SolrCore solrCore, Map<String, Object> settings) {
		initDistribUpdateFactory(cm, solrCore);
		if ((disFactory != null) && (disFactory.getShards() != null)) {
			String shards = disFactory.getShardsString();
			String self = disFactory.getSelf();

			Map<String, Object> updateSettings = new HashMap<String, Object>();
			List<String> shardList = Arrays.asList(shards.split(","));
			updateSettings.put("server_list", shardList);
			updateSettings.put("self", self);
			settings.put(UPDATE_SERVER_SETTINGS_KEY, updateSettings);
		} else {
			settings.put(UPDATE_SERVER_SETTINGS_KEY, null);
		}
	}

	private void initDedupeFactory(CollectionManager cm, SolrCore solrCore) {
		if (dedupeInited) {
			return;
		}
		String updateChain = cm.getUpdateChain();
		UpdateRequestProcessorChain chain = null;
		try {
			chain = solrCore.getUpdateProcessingChain(updateChain);
		} catch (SolrException e) {
			throw ErrorUtils.statusExp(Response.Status.INTERNAL_SERVER_ERROR,
					"This collection does not have a required update chain in it's solrconfig.xml:" + updateChain);
		}
		sigFactory = null;
		for (UpdateRequestProcessorFactory fac : chain.getFactories()) {
			if ((fac instanceof SignatureUpdateProcessorFactory)) {
				sigFactory = ((SignatureUpdateProcessorFactory) fac);
				break;
			}
		}

		dedupeInited = true;
	}

	private void initDistribUpdateFactory(CollectionManager cm, SolrCore solrCore) {
		if (distribUpdateInited) {
			return;
		}

		String updateChain = cm.getUpdateChain();
		UpdateRequestProcessorChain chain = null;
		try {
			chain = solrCore.getUpdateProcessingChain(updateChain);
		} catch (SolrException e) {
			throw ErrorUtils.statusExp(Response.Status.INTERNAL_SERVER_ERROR,
					"This collection does not have a required update chain in it's solrconfig.xml:" + updateChain);
		}

		for (UpdateRequestProcessorFactory fac : chain.getFactories()) {
			if ((fac instanceof DistributedUpdateProcessorFactory)) {
				disFactory = ((DistributedUpdateProcessorFactory) fac);
				break;
			}

		}

		distribUpdateInited = true;
	}

	private boolean getBooleanValue(ModifiableSolrParams gaiaHandlerParams, String paramName) {
		String param = gaiaHandlerParams.get(paramName);
		boolean value;
		if (param == null)
			value = false;
		else {
			value = param.equals("true");
		}
		return value;
	}

	private String getStringValue(ModifiableSolrParams gaiaHandlerParams, String paramName, String defaultValue) {
		String param = gaiaHandlerParams.get(paramName);
		if ((param == null) || (param.length() == 0)) {
			return defaultValue;
		}
		return param;
	}

	public void getElevations(SolrCore solrCore, Map<String, Object> settings, ZkController zkController) {
		ElevateEditor ee = new ElevateEditor(solrCore, zkController);
		Map<String, List<Map<String, Object>>> elevations = ee.getElevations();
		settings.put(ELEVATIONS_KEY, elevations);
	}

	public static boolean isComponentRegistered(SolrCore solrCore, String gaiaReqHandlerName,
			Class<? extends SearchComponent> searchComponentClass) {
		boolean searchComponentEnabled = false;
		StandardRequestHandler gaiaReqHandler = (StandardRequestHandler) solrCore.getRequestHandler(gaiaReqHandlerName);
		List<SearchComponent> searchComponents = gaiaReqHandler.getComponents();
		for (SearchComponent searchComponent : searchComponents) {
			if (searchComponentClass.isInstance(searchComponent)) {
				searchComponentEnabled = true;
			}
		}
		return searchComponentEnabled;
	}

	static {
		MAIN_INDEX_SETTINGS.put("main_index_use_compound_file", "useCompoundFile");
		MAIN_INDEX_SETTINGS.put("main_index_merge_factor", "mergeFactor");
		MAIN_INDEX_SETTINGS.put("main_index_max_buffered_docs", "maxBufferedDocs");
		MAIN_INDEX_SETTINGS.put("main_index_ram_buffer_size_mb", "ramBufferSizeMB");
		MAIN_INDEX_SETTINGS.put("main_index_max_merge_docs", "maxMergeDocs");
		MAIN_INDEX_SETTINGS.put("main_index_write_lock_timeout", "writeLockTimeout");
		MAIN_INDEX_SETTINGS.put("main_index_lock_type", "lockType");
		MAIN_INDEX_SETTINGS.put("main_index_term_index_interval", "termIndexInterval");
	}
}
