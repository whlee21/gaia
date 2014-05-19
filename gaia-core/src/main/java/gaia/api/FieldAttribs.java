package gaia.api;

import gaia.admin.editor.EditableSchemaConfig;
import gaia.admin.editor.EditableSolrConfig;
import gaia.similarity.GaiaMultiLenNormSimilarity;

import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.commons.lang.StringUtils;
import org.apache.lucene.search.similarities.Similarity;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.core.CoreContainer;
import org.apache.solr.core.SolrCore;
import org.apache.solr.handler.RequestHandlerBase;
import org.apache.solr.schema.SchemaField;
import org.apache.solr.update.processor.SignatureUpdateProcessorFactory;
import org.apache.solr.update.processor.UpdateRequestProcessorChain;
import org.apache.solr.update.processor.UpdateRequestProcessorFactory;

public class FieldAttribs {
	public static final String GAIA_REQ_HANDLER = "/gaia";
	public final Set<String> highlightedFields = new TreeSet<String>();
	public Set<String> facetFields = new TreeSet<String>();
	final Set<String> numFacetsFields = new TreeSet<String>();
	public final Set<String> mltFields = new TreeSet<String>();
	public final Set<String> qfFieldsBoosts = new TreeSet<String>();
	public Set<String> gaiaSimilarityFields = new TreeSet<String>();
	public Set<String> synonymsFields = new TreeSet<String>();
	public Set<String> stopwordsFields = new TreeSet<String>();
	public Set<String> dedupeFields = new TreeSet<String>();
	public Set<String> returnedFields = new LinkedHashSet<String>();

	public Map<String, SchemaField> updatedFields = new TreeMap<String, SchemaField>();
	public ModifiableSolrParams gaiaParams;

	SolrCore core;
	EditableSolrConfig ecc;
	public EditableSchemaConfig esc;
	public boolean dedupeEnabled;
	public boolean overwriteDupes;
	public String signatureFieldName = "signatureField";
	SignatureUpdateProcessorFactory sigFactory;
	public String gaiaReqHandler;

	public FieldAttribs(CoreContainer cores, SolrCore core, String updateChain, String gaiaReqHandler) {
		this.gaiaReqHandler = gaiaReqHandler;
		this.core = core;
		this.ecc = new EditableSolrConfig(core, updateChain, cores.getZkController());
		this.esc = new EditableSchemaConfig(core, cores.getZkController());

		this.updatedFields.putAll(core.getLatestSchema().getFields());

		this.gaiaParams = getGaiaParams(core);

		this.returnedFields.addAll(FieldAttributeReader.parseFlParam(this.gaiaParams.getParams("fl")));

		UpdateRequestProcessorChain chain = core.getUpdateProcessingChain(updateChain);

		this.sigFactory = null;
		for (UpdateRequestProcessorFactory fac : chain.getFactories()) {
			if ((fac instanceof SignatureUpdateProcessorFactory)) {
				this.sigFactory = ((SignatureUpdateProcessorFactory) fac);
			}
		}
		this.dedupeEnabled = true;
		this.overwriteDupes = false;
		if (this.sigFactory != null) {
			if (this.sigFactory.getOverwriteDupes()) {
				this.overwriteDupes = true;
			}
			List<String> sigFields = this.sigFactory.getSigFields();
			if (sigFields != null) {
				this.dedupeFields.addAll(sigFields);
			}
			this.signatureFieldName = this.sigFactory.getSignatureField();
		}

		if ((this.sigFactory == null) || (!this.sigFactory.isEnabled()))
			this.dedupeEnabled = false;
	}

	public void save() {
		this.esc.replaceFields(this.updatedFields.values());
		this.ecc.replaceHandlerParams(this.gaiaReqHandler, this.gaiaParams);

		Similarity sim = this.core.getLatestSchema().getSimilarity();
		if (((sim instanceof GaiaMultiLenNormSimilarity)) || (this.gaiaSimilarityFields.size() > 0)) {
			this.esc.setSimilarityFactorySpecialFields(StringUtils.join(this.gaiaSimilarityFields.iterator(), ","));
		}

		if ((this.sigFactory != null) || (this.dedupeFields.size() > 0)) {
			this.ecc.setDedupeUpdateProcess(this.dedupeFields, this.dedupeEnabled, this.overwriteDupes);

			SettingsWriter.verifySignatureField(this.signatureFieldName, this.updatedFields, this.core, this.esc);
		}

		try {
			this.ecc.save();
			this.esc.save();
		} catch (IOException e) {
			throw ErrorUtils.statusExp(e);
		}
	}

	public static ModifiableSolrParams getGaiaParams(SolrCore solrCore) {
		RequestHandlerBase reqHandler = (RequestHandlerBase) solrCore.getRequestHandler(GAIA_REQ_HANDLER);

		if (reqHandler == null) {
			throw new IllegalStateException("Couldn't find request handler:/gaia");
		}

		NamedList defaultsParams = (NamedList) reqHandler.getInitArgs().get("defaults");
		if (defaultsParams == null)
			defaultsParams = new NamedList();
		ModifiableSolrParams gaiaParams = new ModifiableSolrParams(SolrParams.toSolrParams(defaultsParams));

		return gaiaParams;
	}
}
