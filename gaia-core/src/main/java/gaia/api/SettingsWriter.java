package gaia.api;

import gaia.admin.collection.CollectionManager;
import gaia.admin.editor.EditableSchemaConfig;
import gaia.admin.editor.EditableSolrConfig;
import gaia.admin.editor.ElevateEditor;
import gaia.feedback.FeedbackComponent;
import gaia.parser.gaia.GaiaQueryParserParams;
import gaia.solr.click.ClickDeletionPolicy;
import gaia.solr.click.ClickIndexReaderFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.solr.cloud.ZkController;
import org.apache.solr.common.SolrException;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.core.IndexReaderFactory;
import org.apache.solr.core.SolrCore;
import org.apache.solr.handler.component.SpellCheckComponent;
import org.apache.solr.request.SolrRequestHandler;
import org.apache.solr.schema.FieldType;
import org.apache.solr.schema.SchemaField;
import org.apache.solr.schema.StrField;
import org.apache.solr.schema.TextField;
import org.apache.solr.search.LuceneQParserPlugin;
import org.apache.solr.search.QParserPlugin;
import org.apache.solr.update.processor.SignatureUpdateProcessorFactory;
import org.apache.solr.update.processor.UpdateRequestProcessorChain;
import org.apache.solr.update.processor.UpdateRequestProcessorFactory;
import org.apache.solr.util.SolrPluginUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SettingsWriter {
	private static transient Logger LOG = LoggerFactory.getLogger(SettingsWriter.class);
	private SolrCore core;
	private EditableSolrConfig ecc;
	private ModifiableSolrParams gaiaHandlerParams;
	private List<String> qfFieldBoosts;
	private EditableSchemaConfig esc;
	private Map<String, Object> m;
	private SettingsReader settingsReader = new SettingsReader();
	private HashSet<String> qfFields;
	private Map<String, Float> qfMap;
	private Set<String> returnedFields;
	private boolean dedupeInited;
	private SignatureUpdateProcessorFactory sigFactory;
	private CollectionManager cm;

	public SettingsWriter(CollectionManager cm, SolrCore core, Map<String, Object> m, EditableSolrConfig ecc,
			EditableSchemaConfig esc, ModifiableSolrParams gaiaHandlerParams, List<String> qfFieldsBoosts) {
		this.cm = cm;
		this.core = core;
		this.m = m;
		this.ecc = ecc;
		this.esc = esc;
		this.gaiaHandlerParams = gaiaHandlerParams;
		this.qfFieldBoosts = qfFieldsBoosts;

		qfMap = SolrPluginUtils.parseFieldBoosts(gaiaHandlerParams.get(GaiaQueryParserParams.QF));
		qfFields = new HashSet<String>();
		qfFields.addAll(qfMap.keySet());

		returnedFields = FieldAttributeReader.parseFlParam(gaiaHandlerParams.getParams("fl"));
	}

	private void initDedupeFactory(SolrCore solrCore) {
		if (dedupeInited) {
			return;
		}
		String updateChain = cm.getUpdateChain();
		UpdateRequestProcessorChain chain = solrCore.getUpdateProcessingChain(updateChain);

		sigFactory = null;
		for (UpdateRequestProcessorFactory fac : chain.getFactories()) {
			if ((fac instanceof SignatureUpdateProcessorFactory)) {
				sigFactory = ((SignatureUpdateProcessorFactory) fac);
				break;
			}
		}

		dedupeInited = true;
	}

	public void save() throws IOException {
		for (String field : qfFields) {
			Float boost = (Float) qfMap.get(field);
			if (boost != null)
				qfFieldBoosts.add(field + "^" + boost);
			else {
				qfFieldBoosts.add(field);
			}
		}

		gaiaHandlerParams.set(GaiaQueryParserParams.QF,
				new String[] { org.apache.commons.lang.StringUtils.join(qfFieldBoosts.iterator(), " ") });

		gaiaHandlerParams.set("fl", (String[]) returnedFields.toArray(new String[0]));

		ecc.replaceHandlerParams(cm.getGaiaSearchHandler(), gaiaHandlerParams);

		AuditLogger.log("updated settings");

		ecc.save();
		esc.save();
	}

	public void setBoostRecent() {
		Boolean val = gaia.utils.StringUtils.getBoolean(m.remove(SettingsReader.BOOST_RECENT_KEY), false);

		if (val == null) {
			return;
		}

		if (val.booleanValue()) {
			Map<String, Object> settings = new HashMap<String, Object>();
			settingsReader.getBoostRecentDocs(settings, gaiaHandlerParams);
			if (!gaia.utils.StringUtils.getBoolean(settings.get(SettingsReader.BOOST_RECENT_KEY)).booleanValue())
				gaiaHandlerParams.add(SettingsReader.BOOST_RECENT_PARAM, new String[] { SettingsReader.BOOST_RECENT_EQ });
		} else {
			gaiaHandlerParams.remove(SettingsReader.BOOST_RECENT_PARAM, SettingsReader.BOOST_RECENT_EQ);
		}
	}

	public void setBoosts() {
		Object val = m.remove(SettingsReader.BOOSTS_KEY);
		if (val != null) {
			List<String> vals = gaia.utils.StringUtils.getList(String.class, val);

			Map<String, Object> settings = new HashMap<String, Object>();
			settingsReader.getBoosts(settings, gaiaHandlerParams);
			List<String> boosts = (List) settings.get(SettingsReader.BOOSTS_KEY);

			if (boosts != null) {
				for (String boost : boosts) {
					gaiaHandlerParams.remove(SettingsReader.BOOSTS_PARAM, boost);
				}
			}
			if (vals.size() > 0)
				gaiaHandlerParams.add(SettingsReader.BOOSTS_PARAM, (String[]) vals.toArray(new String[0]));
		}
	}

	public void setDisplayFields() {
		Object val = m.remove(SettingsReader.DISPLAY_FIELDS_KEY);
		if (null == val)
			return;
		try {
			List<Object> newFl = (List) val;
			returnedFields.clear();

			for (Object o : newFl) {
				returnedFields.add(o.toString());
			}
		} catch (ClassCastException e) {
			throw ErrorUtils.statusExp(422, new Error(SettingsReader.DISPLAY_FIELDS_KEY, Error.E_INVALID_VALUE,
					"display_fields must be a list"));
		} catch (NullPointerException e2) {
			throw ErrorUtils.statusExp(422, new Error(SettingsReader.DISPLAY_FIELDS_KEY, Error.E_INVALID_VALUE,
					"display_fields list may not contain null values"));
		}
	}

	public void setIndexSettings() {
		Map<String, String> settings = new HashMap<String, String>();
		for (Map.Entry<String, String> entry : SettingsReader.MAIN_INDEX_SETTINGS.entrySet()) {
			String restApiKey = entry.getKey();
			String solrKey = entry.getValue();
			Object value = m.remove(restApiKey);
			if (value != null) {
				if (solrKey.equals("useCompoundFile")) {
					settings.put(solrKey, gaia.utils.StringUtils.getBoolean(value).toString());
				} else if ((solrKey.equals("mergeFactor")) || (solrKey.equals("maxBufferedDocs"))
						|| (solrKey.equals("maxMergeDocs")) || (solrKey.equals("writeLockTimeout"))
						|| (solrKey.equals("termIndexInterval"))) {
					settings.put(solrKey, validateIntParam(restApiKey, value.toString()).toString());
				} else if (solrKey.equals("lockType")) {
					String[] valueTypes = { "simple", "native", "single", "none" };
					if (!Arrays.asList(valueTypes).contains(value.toString().toLowerCase(Locale.ENGLISH).trim())) {
						throw ErrorUtils.statusExp(422, new Error(restApiKey, Error.E_INVALID_VALUE, restApiKey
								+ " must be one of the " + Arrays.toString(valueTypes)));
					}

					settings.put(solrKey, value.toString());
				} else if (solrKey.equals("ramBufferSizeMB")) {
					try {
						settings.put(solrKey, new Double(value.toString()).toString());
					} catch (NumberFormatException e) {
						throw ErrorUtils.statusExp(422, new Error(restApiKey, Error.E_INVALID_VALUE, restApiKey
								+ " must be valid double number"));
					}
				}
			}
		}
		ecc.setIndexSettings(settings);
	}

	private static Integer validateIntParam(String key, Object value) {
		try {
			return Integer.valueOf(Integer.parseInt(value.toString()));
		} catch (NumberFormatException e) {
		}
		throw ErrorUtils.statusExp(422, new Error(key, Error.E_INVALID_VALUE, key + " must be valid integer"));
	}

	public void setUpdateHandlerSettings() {
		Object autoCommitMaxTime = m.get(SettingsReader.UPDATE_HANDLER_AUTOCOMMIT_MAX_TIME);
		Object autoCommitMaxDocs = m.get(SettingsReader.UPDATE_HANDLER_AUTOCOMMIT_MAX_DOCS);
		Object autoCommitOpenSearcher = m.get(SettingsReader.UPDATE_HANDLER_AUTOCOMMIT_OPEN_SEARCHER);
		Object autoSoftCommitMaxTime = m.get(SettingsReader.UPDATE_HANDLER_AUTOSOFTCOMMIT_MAX_TIME);
		Object autoSoftCommitMaxDocs = m.get(SettingsReader.UPDATE_HANDLER_AUTOSOFTCOMMIT_MAX_DOCS);

		if ((!m.containsKey(SettingsReader.UPDATE_HANDLER_AUTOCOMMIT_MAX_TIME))
				&& (!m.containsKey(SettingsReader.UPDATE_HANDLER_AUTOCOMMIT_MAX_DOCS))
				&& (!m.containsKey(SettingsReader.UPDATE_HANDLER_AUTOCOMMIT_OPEN_SEARCHER))
				&& (!m.containsKey(SettingsReader.UPDATE_HANDLER_AUTOSOFTCOMMIT_MAX_TIME))
				&& (!m.containsKey(SettingsReader.UPDATE_HANDLER_AUTOSOFTCOMMIT_MAX_DOCS))) {
			return;
		}

		EditableSolrConfig.UpdateHandlerSettings updateHandlerSettings = ecc.getUpdateHandlerSettings();

		if (m.containsKey(SettingsReader.UPDATE_HANDLER_AUTOCOMMIT_MAX_TIME)) {
			if ((autoCommitMaxTime != null) && (!"".equals(autoCommitMaxTime)))
				updateHandlerSettings.autoCommitMaxTime = validateIntParam(SettingsReader.UPDATE_HANDLER_AUTOCOMMIT_MAX_TIME,
						autoCommitMaxTime.toString());
			else {
				updateHandlerSettings.autoCommitMaxTime = null;
			}
		}
		if (m.containsKey(SettingsReader.UPDATE_HANDLER_AUTOCOMMIT_MAX_DOCS)) {
			if ((autoCommitMaxDocs != null) && (!"".equals(autoCommitMaxDocs)))
				updateHandlerSettings.autoCommitMaxDocs = validateIntParam(SettingsReader.UPDATE_HANDLER_AUTOCOMMIT_MAX_DOCS,
						autoCommitMaxDocs.toString());
			else {
				updateHandlerSettings.autoCommitMaxDocs = null;
			}
		}
		if (m.containsKey(SettingsReader.UPDATE_HANDLER_AUTOCOMMIT_OPEN_SEARCHER)) {
			if ((autoCommitOpenSearcher != null) && (!"".equals(autoCommitOpenSearcher)))
				updateHandlerSettings.autoCommitOpenSearcher = gaia.utils.StringUtils.getBoolean(autoCommitOpenSearcher
						.toString());
			else {
				updateHandlerSettings.autoCommitOpenSearcher = null;
			}
		}
		if (m.containsKey(SettingsReader.UPDATE_HANDLER_AUTOSOFTCOMMIT_MAX_TIME)) {
			if ((autoSoftCommitMaxTime != null) && (!"".equals(autoSoftCommitMaxTime)))
				updateHandlerSettings.autoSoftCommitMaxTime = validateIntParam(
						SettingsReader.UPDATE_HANDLER_AUTOSOFTCOMMIT_MAX_TIME, autoSoftCommitMaxTime.toString());
			else {
				updateHandlerSettings.autoSoftCommitMaxTime = null;
			}
		}
		if (m.containsKey(SettingsReader.UPDATE_HANDLER_AUTOSOFTCOMMIT_MAX_DOCS)) {
			if ((autoSoftCommitMaxDocs != null) && (!"".equals(autoSoftCommitMaxDocs)))
				updateHandlerSettings.autoSoftCommitMaxDocs = validateIntParam(
						SettingsReader.UPDATE_HANDLER_AUTOSOFTCOMMIT_MAX_DOCS, autoSoftCommitMaxDocs.toString());
			else {
				updateHandlerSettings.autoSoftCommitMaxDocs = null;
			}

		}

		ecc.setUpdateHandlerSettings(updateHandlerSettings);

		m.remove(SettingsReader.UPDATE_HANDLER_AUTOCOMMIT_MAX_TIME);
		m.remove(SettingsReader.UPDATE_HANDLER_AUTOCOMMIT_MAX_DOCS);
		m.remove(SettingsReader.UPDATE_HANDLER_AUTOCOMMIT_OPEN_SEARCHER);
		m.remove(SettingsReader.UPDATE_HANDLER_AUTOSOFTCOMMIT_MAX_TIME);
		m.remove(SettingsReader.UPDATE_HANDLER_AUTOSOFTCOMMIT_MAX_DOCS);
	}

	public void setClickSettings() {
		String docIdField = "id";

		String clickBoostDataLocation = (String) m.remove(SettingsReader.CLICK_BOOST_DATA_LOCATION_KEY);

		String clickBoostField = (String) m.remove(SettingsReader.CLICK_FIELD_PREFIX_KEY);

		Boolean clickEnabled = gaia.utils.StringUtils.getBoolean(m.remove(SettingsReader.CLICK_ENABLED_KEY), false);

		if ((clickBoostDataLocation == null) && (clickBoostField == null) && (clickEnabled == null)) {
			return;
		}

		IndexReaderFactory readerFactory = core.getIndexReaderFactory();
		String oldBoostDataLocation = null;
		String oldBoostField = null;
		Boolean oldIsEnabled = null;

		if ((!(readerFactory instanceof ClickIndexReaderFactory)) && (!clickEnabled.booleanValue())) {
			return;
		}

		if ((readerFactory instanceof ClickIndexReaderFactory)) {
			ClickIndexReaderFactory clickReaderFactory = (ClickIndexReaderFactory) readerFactory;
			oldBoostDataLocation = clickReaderFactory.getBoostData();
			oldBoostField = clickReaderFactory.getBoostField();
			oldIsEnabled = Boolean.valueOf(clickReaderFactory.isEnabled());
		}

		if (oldBoostDataLocation == null) {
			oldBoostDataLocation = "click-data";
		}
		if (oldBoostField == null) {
			oldBoostField = "click";
		}
		if (oldIsEnabled == null) {
			oldIsEnabled = Boolean.valueOf(false);
		}

		if (clickBoostDataLocation == null) {
			clickBoostDataLocation = oldBoostDataLocation;
		}
		if (clickBoostField == null) {
			clickBoostField = oldBoostField;
		}
		if (clickEnabled == null) {
			clickEnabled = oldIsEnabled;
		}

		String clickFieldTerms = clickBoostField + "_terms";
		String clickFieldVal = clickBoostField + "_val";
		Map<String, SchemaField> staticFields = core.getLatestSchema().getFields();
		if ((!clickBoostField.equals(oldBoostField)) || (staticFields.get(clickBoostField) == null)
				|| (staticFields.get(clickFieldTerms) == null) || (staticFields.get(clickFieldVal) == null)) {
			updateClickFieldsInSchema(clickBoostField, oldBoostField);
		}

		Set<String> removeFields = new HashSet<String>();
		for (String field : qfFields) {
			if (field.endsWith("_terms")) {
				removeFields.add(field);
			}
		}
		for (String field : removeFields) {
			qfFields.remove(field);
			returnedFields.remove(field);
		}
		if (clickEnabled.booleanValue()) {
			qfFields.add(clickBoostField + "_terms");
			qfMap.put(clickBoostField + "_terms", Float.valueOf(5.0F));
			returnedFields.add(clickBoostField + "_terms");
		}

		if (!(core.getDeletionPolicy().getWrappedDeletionPolicy() instanceof ClickDeletionPolicy)) {
			ecc.initClickDeletionPolicy();
		}

		if (core.getRequestHandler("/click") == null) {
			ecc.initClickAnalysisRequestHandler();
		}

		ecc.setClickIndexReaderFactorySettings(clickEnabled.booleanValue(), docIdField, clickBoostDataLocation,
				clickBoostField);

		AuditLogger.log("updated click settings");
	}

	private void updateClickFieldsInSchema(String prefix, String oldPrefix) {
		Map<String, SchemaField> currentFields = core.getLatestSchema().getFields();
		Map<String, SchemaField> updatedFields = new TreeMap<String, SchemaField>();
		updatedFields.putAll(currentFields);

		SchemaField old1 = (SchemaField) updatedFields.remove(oldPrefix);
		SchemaField old2 = (SchemaField) updatedFields.remove(oldPrefix + "_terms");
		SchemaField old3 = (SchemaField) updatedFields.remove(oldPrefix + "_val");

		Map<String, FieldType> fieldTypes = core.getLatestSchema().getFieldTypes();

		FieldType stringFieldType = null;
		for (FieldType fieldType : fieldTypes.values()) {
			if (((fieldType instanceof StrField)) && (new SchemaField("tmp", fieldType).indexed())) {
				stringFieldType = fieldType;
				break;
			}

		}

		FieldType textFieldType = fieldTypes.get("text_ws");
		if (textFieldType == null) {
			textFieldType = fieldTypes.get("text_en");
		}
		if (textFieldType == null)
			for (FieldType fieldType : fieldTypes.values())
				if (((fieldType instanceof TextField)) && (new SchemaField("tmp", fieldType).indexed())) {
					textFieldType = fieldType;
					break;
				}
		SchemaField new1;
		if (old1 != null)
			new1 = new SchemaField(old1, prefix);
		else
			new1 = new SchemaField(prefix, stringFieldType);
		SchemaField new2;
		if (old2 != null)
			new2 = new SchemaField(old2, prefix + "_terms");
		else
			new2 = new SchemaField(prefix + "_terms", textFieldType);
		SchemaField new3;
		if (old1 != null)
			new3 = new SchemaField(old3, prefix + "_val");
		else {
			new3 = new SchemaField(prefix + "_val", stringFieldType);
		}

		updatedFields.put(new1.getName(), new1);
		updatedFields.put(new2.getName(), new2);
		updatedFields.put(new3.getName(), new3);
		esc.replaceFields(updatedFields.values());
	}

	public static void verifySignatureField(String signatureFieldName, Map<String, SchemaField> fields, SolrCore core,
			EditableSchemaConfig esc) {
		Map<String, SchemaField> currentFields = fields;
		if (currentFields == null) {
			currentFields = core.getLatestSchema().getFields();
		}
		SchemaField signatureField = (SchemaField) currentFields.get(signatureFieldName);
		if ((signatureField != null) && (signatureField.indexed())) {
			return;
		}

		Map<String, SchemaField> updatedFields = new TreeMap<String, SchemaField>();
		updatedFields.putAll(currentFields);
		updatedFields.remove(signatureFieldName);

		Map<String, FieldType> fieldTypes = core.getLatestSchema().getFieldTypes();
		FieldType signatureFieldType = null;
		for (FieldType fieldType : fieldTypes.values()) {
			if (((fieldType instanceof StrField)) && (new SchemaField("tmp", fieldType).indexed())) {
				signatureFieldType = fieldType;
				break;
			}

		}

		SchemaField newField = new SchemaField(signatureFieldName, signatureFieldType);

		updatedFields.put(newField.getName(), newField);
		esc.replaceFields(new TreeMap<String, SchemaField>(updatedFields).values());
	}

	public void setDedupe() {
		String val = (String) m.remove(SettingsReader.DEDUPE_KEY);
		if (val != null) {
			initDedupeFactory(core);
			boolean overwriteDupes = false;
			boolean isEnabled = false;
			Set<String> fields = new HashSet<String>();
			if (sigFactory != null) {
				List<String> oldFields = sigFactory.getSigFields();
				if (oldFields != null) {
					fields.addAll(oldFields);
				}
			}

			if (val.equals("off")) {
				isEnabled = false;
			} else if (val.equals("tag")) {
				isEnabled = true;
				overwriteDupes = false;
			} else if (val.equals("overwrite")) {
				isEnabled = true;
				overwriteDupes = true;
			}

			if ((sigFactory != null) || (isEnabled)) {
				ecc.setDedupeUpdateProcess(fields, isEnabled, overwriteDupes);

				String signatureFieldName = "signatureField";
				if (sigFactory != null) {
					signatureFieldName = sigFactory.getSignatureField();
				}
				verifySignatureField(signatureFieldName, null, core, esc);
			}
		}
	}

	public void setDefaultSort() {
		String defaultSortKey = (String) m.remove(SettingsReader.DEFAULT_SORT_KEY);
		if (defaultSortKey != null) {
			if (defaultSortKey.equals(SettingsReader.RELEVANCE))
				defaultSortKey = SettingsReader.SCORE_DESC;
			else if (defaultSortKey.equals(SettingsReader.DATE))
				defaultSortKey = SettingsReader.LAST_MODIFIED_DESC;
			else if (defaultSortKey.equals(SettingsReader.RANDOM))
				defaultSortKey = SettingsReader.RANDOM_1_DESC;
			else {
				throw ErrorUtils.statusExp(422, new Error(SettingsReader.DEFAULT_SORT_KEY, Error.E_INVALID_VALUE,
						"Invalid sort type found:" + defaultSortKey));
			}

			gaiaHandlerParams.set(SettingsReader.DEFAULT_SORT_PARAM, new String[] { defaultSortKey });
		}
	}

	public void setDisplayFacets() {
		Boolean val = gaia.utils.StringUtils.getBoolean(m.remove(SettingsReader.DISPLAY_FACET_KEY), false);

		if (val != null)
			gaiaHandlerParams.set(SettingsReader.DISPLAY_FACET_PARAM, val.booleanValue());
	}

	public void setElevations(ZkController zkController) throws IOException {
		Map<String, List<Map<String, Object>>> elevations = (Map) m.remove(SettingsReader.ELEVATIONS_KEY);

		if (elevations != null) {
			ElevateEditor ee = new ElevateEditor(core, zkController);
			ee.addElevations(elevations);
			ee.save();

			ecc.enableQueryElevation();

			returnedFields.add("[elevated]");
			returnedFields.add("[excluded]");
		}
	}

	public void setEnableAutocomplete() {
		Boolean val = gaia.utils.StringUtils.getBoolean(m.remove(SettingsReader.ENABLE_AUTOCOMPLETE_KEY), false);

		if (val != null) {
			if (val.booleanValue() == true) {
				SolrRequestHandler requestHandler = core.getRequestHandler("/autocomplete");
				if (requestHandler == null) {
					throw ErrorUtils.statusExp(422, new Error(SettingsReader.ENABLE_AUTOCOMPLETE_KEY, Error.E_FORBIDDEN_VALUE,
							"solrconfig.xml is missing '/autocomplete' request handler"));
				}

			}

			gaiaHandlerParams.set(SettingsReader.ENABLE_AUTOCOMPLETE_PARAM, val.booleanValue());
		}
	}

	public void setEnableSpellcheck() {
		Boolean val = gaia.utils.StringUtils.getBoolean(m.remove(SettingsReader.ENABLE_SPELLCHECK_KEY), false);

		if (val != null) {
			if (val.booleanValue() == true) {
				boolean spellcheckCompPresent = SettingsReader.isComponentRegistered(core, cm.getGaiaSearchHandler(),
						SpellCheckComponent.class);

				if (!spellcheckCompPresent) {
					throw ErrorUtils.statusExp(422, new Error(SettingsReader.ENABLE_SPELLCHECK_KEY, Error.E_FORBIDDEN_VALUE,
							"spellcheck component is not registered for '" + cm.getGaiaSearchHandler() + "' request handler"));
				}

				if (gaiaHandlerParams.get("spellcheck.collate") == null) {
					gaiaHandlerParams.set("spellcheck.collate", true);
				}
				if (gaiaHandlerParams.get("spellcheck.onlyMorePopular") == null) {
					gaiaHandlerParams.set("spellcheck.onlyMorePopular", true);
				}
			}
			gaiaHandlerParams.set(SettingsReader.ENABLE_SPELLCHECK_KEY, val.booleanValue());
		}
	}

	public void setQueryParser() {
		String val = (String) m.remove(SettingsReader.QUERY_PARSER_KEY);
		if (val != null) {
			gaiaHandlerParams.set(SettingsReader.QUERY_PARSER_PARAM, new String[] { val });

			QParserPlugin qParserPlugin = null;
			try {
				qParserPlugin = core.getQueryPlugin(val);
			} catch (SolrException e) {
			}
			if (qParserPlugin == null) {
				throw ErrorUtils.statusExp(422, new Error(SettingsReader.QUERY_PARSER_KEY, Error.E_FORBIDDEN_VALUE,
						"unknown query parser '" + val + "'"));
			}

			if ((!LuceneQParserPlugin.NAME.equals(val)) && (gaiaHandlerParams.get(GaiaQueryParserParams.ALTQ) == null))
				gaiaHandlerParams.set(GaiaQueryParserParams.ALTQ, new String[] { GaiaQueryParserParams.ALTQDefault });
		}
	}

	public void setQueryStopWordsEnabled() {
		Boolean val = gaia.utils.StringUtils.getBoolean(m.remove(SettingsReader.QUERY_STOPWORDS_ENABLED_KEY), false);

		if (val != null)
			gaiaHandlerParams.set(SettingsReader.QUERY_STOPWORDS_ENABLED_PARAM, val.booleanValue());
	}

	public void setSearchServerList() {
		Object val = m.remove(SettingsReader.SEARCH_SERVER_LIST_KEY);
		if (val != null) {
			List<String> shards = gaia.utils.StringUtils.getList(String.class, val);
			if (shards.size() > 0) {
				String shardString = gaia.utils.StringUtils.listToString(shards);
				gaiaHandlerParams.set(SettingsReader.SEARCH_SERVER_LIST_PARAM, new String[] { shardString });
			} else {
				gaiaHandlerParams.set(SettingsReader.SEARCH_SERVER_LIST_PARAM, new String[] { (String) null });
			}
		}
	}

	public void setShowFindSimiliar() {
		Boolean val = gaia.utils.StringUtils.getBoolean(m.remove(SettingsReader.SHOW_FIND_SIMILAR_LINKS_KEY), false);

		if (val != null)
			gaiaHandlerParams.set(SettingsReader.SHOW_FIND_SIMILAR_LINKS_PARAM, val.booleanValue());
	}

	public void setStopWordsList() {
		List<String> stopwords = gaia.utils.StringUtils.getList(String.class, m.remove(SettingsReader.STOPWORDS_LIST_KEY));

		if (stopwords != null) {
			String name = "stopwords.txt";
			File file;
			if (ecc.getZkController() == null) {
				String configDir = core.getResourceLoader().getConfigDir();
				file = new File(configDir, name);
			} else {
				file = new File(name);
			}
			ecc.writeConfigFile(stopwords, file);
		}
	}

	public void setUnsupervisedFeedback() {
		Boolean val = gaia.utils.StringUtils.getBoolean(m.remove(SettingsReader.UNSUPERVISED_FEEDBACK_KEY), false);

		if (val != null) {
			if (val.booleanValue() == true) {
				boolean feedbackCompRegistered = SettingsReader.isComponentRegistered(core, cm.getGaiaSearchHandler(),
						FeedbackComponent.class);
				if (!feedbackCompRegistered) {
					throw ErrorUtils.statusExp(422, new Error(SettingsReader.UNSUPERVISED_FEEDBACK_KEY, Error.E_FORBIDDEN_VALUE,
							"feedback component is not registered for '" + cm.getGaiaSearchHandler() + "' request handler"));
				}

			}

			gaiaHandlerParams.set(SettingsReader.UNSUPERVISED_FEEDBACK_PARAM, val.booleanValue());
		}
	}

	public void setUnsupervisedFeedbackEmphasis() {
		Object obj = m.remove(SettingsReader.UNSUPERVISED_FEEDBACK_EMPHASIS_KEY);
		if (obj == null)
			return;
		if (!(obj instanceof String)) {
			throw ErrorUtils.statusExp(422,
					new Error(SettingsReader.UNSUPERVISED_FEEDBACK_EMPHASIS_KEY, Error.E_INVALID_TYPE));
		}

		String val = (String) obj;
		if (SettingsReader.UNSUPERVISED_FEEDBACK_EMPHASIS_PARAM_RECALL_KEY.equalsIgnoreCase(val)) {
			gaiaHandlerParams.set(SettingsReader.UNSUPERVISED_FEEDBACK_EMPHASIS_PARAM,
					new String[] { SettingsReader.UNSUPERVISED_FEEDBACK_EMPHASIS_PARAM_RECALL_KEY });
			return;
		}
		if ("relevancy".equalsIgnoreCase(val)) {
			gaiaHandlerParams.set(SettingsReader.UNSUPERVISED_FEEDBACK_EMPHASIS_PARAM, new String[] { "relevancy" });
			return;
		}
		throw ErrorUtils
				.statusExp(422, new Error(SettingsReader.UNSUPERVISED_FEEDBACK_EMPHASIS_KEY, Error.E_INVALID_VALUE));
	}

	public void setSynonyms() {
		Boolean val = gaia.utils.StringUtils.getBoolean(m.remove(SettingsReader.SYNONYMS_ENABLED_KEY), false);

		if (val != null)
			gaiaHandlerParams.set(SettingsReader.SYNONYMS_ENABLED_PARAM, val.booleanValue());
	}

	public void setSynonymsList() {
		List<String> synonyms = (ArrayList) m.remove(SettingsReader.SYNONYM_LIST_KEY);
		if (synonyms != null) {
			String name = "synonyms.txt";
			File file;
			if (ecc.getZkController() == null) {
				String configDir = core.getResourceLoader().getConfigDir();
				file = new File(configDir, name);
			} else {
				file = new File(name);
			}
			ecc.writeConfigFile(synonyms, file);
		}
	}

	public void setUpdateServerSettings() {
		Map<String, Object> settings = (Map) m.remove(SettingsReader.UPDATE_SERVER_SETTINGS_KEY);

		if (settings != null) {
			List<String> shards = gaia.utils.StringUtils.getList(String.class, settings.get("server_list"));
			if (shards.size() > 0) {
				String shardsString = gaia.utils.StringUtils.listToString(shards);
				String self = (String) settings.get("self");
				ecc.setDistributedUpdate(self, shardsString);
			} else {
				ecc.setDistributedUpdate(null, null);
			}
		}
	}
}
