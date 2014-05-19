package gaia.solr.behemoth;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.MapWritable;
import org.apache.hadoop.io.Writable;
import org.apache.hadoop.util.Progressable;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.CloudSolrServer;
import org.apache.solr.client.solrj.impl.ConcurrentUpdateSolrServer;
import org.apache.solr.client.solrj.request.UpdateRequest;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.params.ModifiableSolrParams;

import com.digitalpebble.behemoth.Annotation;
import com.digitalpebble.behemoth.BehemothDocument;

public class GaiaSearchWriter {
	private static final Log LOG = LogFactory.getLog(GaiaSearchWriter.class);
	private SolrServer solr;
	protected Map<String, Map<String, String>> fieldMapping = new HashMap<String, Map<String, String>>();
	private Progressable progress;
	private boolean includeMetadata = false;
	protected boolean includeAnnotations = false;
	protected boolean includeAllAnnotations = false;
	protected ModifiableSolrParams params = null;

	public GaiaSearchWriter(Progressable progress) {
		this.progress = progress;
	}

	public void open(Configuration conf) throws IOException {
		String zkHost = conf.get("solr.zkhost");
		if ((zkHost != null) && (!zkHost.equals(""))) {
			String collection = conf.get("solr.zk.collection", "collection1");
			LOG.info("Indexing to collection: " + collection + " w/ ZK host: " + zkHost);
			solr = new CloudSolrServer(zkHost);
			((CloudSolrServer) solr).setDefaultCollection(collection);
		} else {
			String solrURL = conf.get("solr.server.url");
			int queueSize = conf.getInt("solr.client.queue.size", 100);
			int threadCount = conf.getInt("solr.client.threads", 1);
			solr = new ConcurrentUpdateSolrServer(solrURL, queueSize, threadCount);
			LOG.info("Indexing to Solr URL: " + solrURL);
		}
		String paramsString = conf.get("solr.params");
		if (paramsString != null) {
			params = new ModifiableSolrParams();
			String[] pars = paramsString.trim().split("\\&");
			for (String kvs : pars) {
				String[] kv = kvs.split("=");
				if (kv.length < 2) {
					LOG.warn("Invalid Solr param " + kvs + ", skipping...");
				} else
					params.add(kv[0], new String[] { kv[1] });
			}
			LOG.info("Using Solr params: " + params.toString());
		}
		includeMetadata = conf.getBoolean("gaia.metadata", false);
		includeAnnotations = conf.getBoolean("gaia.annotations", false);
		populateSolrFieldMappingsFromBehemothAnnotationsTypesAndFeatures(conf);
	}

	protected void populateSolrFieldMappingsFromBehemothAnnotationsTypesAndFeatures(Configuration conf) {
		Iterator<Map.Entry<String, String>> iterator = conf.iterator();
		while (iterator.hasNext()) {
			Map.Entry<String, String> entry = iterator.next();
			if (((String) entry.getKey()).startsWith("solr.f.")) {
				String solrFieldName = ((String) entry.getKey()).substring("solr.f.".length());
				populateMapping(solrFieldName, (String) entry.getValue());
			}
		}
		String list = conf.get("gaia.annotations.list");
		if ((list == null) || (list.trim().length() == 0)) {
			return;
		}
		String[] names = list.split("\\s+");
		for (String name : names) {
			if (name.equals("*")) {
				includeAllAnnotations = true;
			} else {
				String solrFieldName = "annotation_" + name;
				populateMapping(solrFieldName, name);
			}
		}
	}

	private void populateMapping(String solrFieldName, String value) {
		String[] toks = value.split("\\.");
		String annotationName = null;
		String featureName = null;
		if (toks.length == 1) {
			annotationName = toks[0];
		} else if (toks.length == 2) {
			annotationName = toks[0];
			featureName = toks[1];
		} else {
			LOG.warn("Invalid annotation field mapping: " + value);
		}

		Map<String, String> featureMap = (Map) fieldMapping.get(annotationName);
		if (featureMap == null) {
			featureMap = new HashMap<String, String>();
		}

		if (featureName == null) {
			featureName = "*";
		}
		featureMap.put(featureName, solrFieldName);
		fieldMapping.put(annotationName, featureMap);

		LOG.info("Adding mapping for annotation " + annotationName + ", feature '" + featureName + "' to  Solr field '"
				+ solrFieldName + "'");
	}

	public void write(BehemothDocument doc) throws IOException {
		SolrInputDocument inputDoc = convertToSOLR(doc);
		try {
			progress.progress();
			if (params == null) {
				solr.add(inputDoc);
			} else {
				UpdateRequest req = new UpdateRequest();
				req.setParams(params);
				req.add(inputDoc);
				solr.request(req);
			}
		} catch (SolrServerException e) {
			throw makeIOException(e);
		}
	}

	protected SolrInputDocument convertToSOLR(BehemothDocument doc) {
		SolrInputDocument inputDoc = new SolrInputDocument();

		inputDoc.setField("id", doc.getUrl());
		inputDoc.setField("text", doc.getText());

		LOG.debug("Adding field : id\t" + doc.getUrl());

		MapWritable metadata = doc.getMetadata();
		if ((includeMetadata) && (metadata != null)) {
			for (Map.Entry entry : metadata.entrySet()) {
				inputDoc.addField(((Writable) entry.getKey()).toString(), ((Writable) entry.getValue()).toString());
			}

		}

		if (includeAnnotations) {
			Iterator<Annotation> iterator = doc.getAnnotations().iterator();
			Annotation current;
			while (iterator.hasNext()) {
				current = (Annotation) iterator.next();

				Map<String, Object> featureField = (Map) fieldMapping.get(current.getType());

				if ((featureField != null) || (includeAllAnnotations)) {
					if (!includeAllAnnotations) {
						for (String targetFeature : featureField.keySet()) {
							String SOLRFieldName = (String) featureField.get(targetFeature);
							String value = null;

							if ("*".equals(targetFeature)) {
								value = doc.getText().substring((int) current.getStart(), (int) current.getEnd());
							} else {
								value = (String) current.getFeatures().get(targetFeature);
							}
							LOG.debug("Adding field : " + SOLRFieldName + "\t" + value);

							if (value != null)
								inputDoc.addField(SOLRFieldName, value);
						}
					} else
						for (Map.Entry<String, String> e : current.getFeatures().entrySet()) {
							inputDoc.addField("annotation_" + current.getType() + "." + (String) e.getKey(), e.getValue());
						}
				}
			}
		}

		float boost = 1.0F;
		inputDoc.setDocumentBoost(boost);
		return inputDoc;
	}

	public void close() throws IOException {
		try {
			solr.commit(true, false);
			if ((solr instanceof ConcurrentUpdateSolrServer)) {
				((ConcurrentUpdateSolrServer) solr).blockUntilFinished();
			}
			solr.shutdown();
		} catch (SolrServerException e) {
			throw makeIOException(e);
		}
	}

	public static IOException makeIOException(SolrServerException e) {
		IOException ioe = new IOException();
		ioe.initCause(e);
		return ioe;
	}
}
