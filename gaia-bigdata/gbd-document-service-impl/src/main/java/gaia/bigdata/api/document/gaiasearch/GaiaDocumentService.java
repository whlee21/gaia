package gaia.bigdata.api.document.gaiasearch;

import gaia.api.ClickEventResource;
import gaia.bigdata.ResultType;
import gaia.bigdata.api.State;
import gaia.bigdata.api.Status;
import gaia.bigdata.api.document.DocServiceRequest;
import gaia.bigdata.api.document.DocumentService;
import gaia.bigdata.api.document.Utils;
import gaia.bigdata.services.ServiceType;
import gaia.commons.api.Configuration;
import gaia.commons.services.BaseService;
import gaia.commons.services.ServiceLocator;
import gaia.commons.services.URIPayload;
import gaia.commons.util.RestletContainer;
import gaia.commons.util.RestletUtil;
import gaia.crawl.api.DataSourcesResource;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.nio.charset.Charset;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import org.apache.solr.client.solrj.ResponseParser;
import org.apache.solr.client.solrj.SolrRequest;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.CloudSolrServer;
import org.apache.solr.client.solrj.request.QueryRequest;
import org.apache.solr.client.solrj.request.UpdateRequest;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.common.util.NamedList;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.restlet.data.MediaType;
import org.restlet.engine.io.ReaderInputStream;
import org.restlet.representation.InputRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class GaiaDocumentService extends BaseService implements DocumentService {
	private static transient Logger log = LoggerFactory.getLogger(GaiaDocumentService.class);
	protected CloudSolrServer server;
	protected String idFieldName;
	protected EnumSet<ResultType> supportedTypes;
	private final String zkHost;

	@Inject
	public GaiaDocumentService(Configuration config, ServiceLocator locator) throws MalformedURLException {
		super(config, locator);
		URIPayload gaiaSearchLocation = getServiceURI(ServiceType.GAIASEARCH.name());
		if ((gaiaSearchLocation != null) && (gaiaSearchLocation.uri != null)) {
			zkHost = config.getProperties().getProperty("solr.zk.connect");
			if (zkHost != null)
				createSolrServer();
			else {
				throw new RuntimeException("solr.zk.connect must be set");
			}

		} else {
			throw new RuntimeException("Unable to locate GaiaSearch");
		}
		idFieldName = config.getProperties().getProperty("solr.id.field", "id");
		supportedTypes = EnumSet.of(ResultType.QUERY, ResultType.STATISTICALLY_INTERESTING_PHRASES);
	}

	private void createSolrServer() throws MalformedURLException {
		server = new CloudSolrServer(zkHost);
		server.setDefaultCollection("collection1");
	}

	public EnumSet<ResultType> getSupportedResultTypes() {
		return supportedTypes;
	}

	public String getType() {
		return ServiceType.DOCUMENT.name();
	}

	public JSONObject retrieve(String collection, String id) {
		return null;
	}

	public JSONObject retrieve(String collection, String id, Map<String, Object> request) {
		return null;
	}

	protected ModifiableSolrParams mapParamsForSips(String collection, Map<String, Object> request) {
		ModifiableSolrParams params = new ModifiableSolrParams();

		params.add("collection", new String[] { collection + "_collocations" });
		if ((request != null) && (!request.isEmpty())) {
			fillParams(request, params);
		}

		if (params.get("q") == null) {
			params.add("q", new String[] { "*:*" });
		}
		return params;
	}

	private ModifiableSolrParams mapParams(String collection, Map<String, Object> request) {
		ModifiableSolrParams params = new ModifiableSolrParams();

		params.add("collection", new String[] { collection });
		if ((request != null) && (!request.isEmpty())) {
			fillParams(request, params);
		}
		String[] s = params.getParams("fl");
		boolean hasId = false;
		if ((s != null) && (s.length > 0)) {
			hasId = false;
			for (int i = 0; i < s.length; i++) {
				String field = s[i];
				if ((field.equals("*")) || (field.equals(idFieldName))) {
					hasId = true;
					break;
				}
			}
		} else {
			params.add("fl", new String[] { "*,score" });
		}
		if (!hasId) {
			params.add("fl", new String[] { idFieldName });
		}
		return params;
	}

	private void fillParams(Map<String, Object> request, ModifiableSolrParams params) {
		for (Map.Entry<String, Object> entry : request.entrySet()) {
			String[] vals;
			int i;
			if ((entry.getValue() instanceof Collection)) {
				Collection coll = (Collection) entry.getValue();
				vals = new String[coll.size()];
				i = 0;
				for (Iterator iter = coll.iterator(); iter.hasNext();) {
					Object o = iter.next();
					vals[(i++)] = o.toString();
				}
			} else {
				vals = new String[] { entry.getValue().toString() };
			}
			params.add((String) entry.getKey(), vals);
		}
	}

	public JSONObject retrieve(String collection, DocServiceRequest request) {
		// DOCUMENTS, QUERY, CLUSTERS, TOPICS, FACET, HIGHLIGHT, SUGGEST, SPELL,
		// MOST_POPULAR, STATISTICALLY_INTERESTING_PHRASES, DUPLICATES, NEVER_SEEN,
		// FAILED;
		JSONObject result = null;
		switch (request.resultType) {
		case DOCUMENTS:
			result = processQuery(collection, request);
			break;
		case QUERY:
			result = processQueryForSips(collection, request);
			break;
		}

		return result;
	}

	private JSONObject processQuery(String collection, DocServiceRequest request) {
		JSONObject result = null;
		try {
			ModifiableSolrParams params = mapParams(collection, request.request);
			params.add("wt", new String[] { "json" });
			SolrRequest solrRequest = new QueryRequest(params);
			RawStringCaptureResponseParser responseParser = new RawStringCaptureResponseParser();
			solrRequest.setResponseParser(responseParser);
			try {
				sendSolrRequest(solrRequest);
				result = new JSONObject(responseParser.raw);
				result.put("requestToken", submitClick(collection, params, result));
			} catch (IOException e) {
				log.error("Exception", e);
				throw new RuntimeException(e);
			} catch (JSONException e) {
				log.error("Exception parsing: " + responseParser.raw, e);
				throw new RuntimeException(e);
			} catch (ParseException e) {
				log.error("Exception parsing time: " + responseParser.raw, e);
				throw new RuntimeException(e);
			}
		} catch (SolrServerException e) {
			log.error("Exception", e);
			throw new RuntimeException(e);
		}
		return result;
	}

	private void sendSolrRequest(SolrRequest solrRequest) throws SolrServerException, IOException {
		int tries = 0;
		while (tries < 3) {
			try {
				server.request(solrRequest);
			} catch (SolrServerException e) {
				log.warn("Exception, sleep and retry: " + tries, e);
				try {
					Thread.sleep(5000L);
				} catch (InterruptedException e1) {
				}
				server.shutdown();
				createSolrServer();
				if (tries >= 2)
					throw e;
			} catch (IOException e) {
				log.warn("Exception, retry: " + tries, e);
				try {
					Thread.sleep(5000L);
				} catch (InterruptedException e1) {
				}
				server.shutdown();
				createSolrServer();
				if (tries >= 2) {
					throw e;
				}
			}
			tries++;
		}
	}

	private String submitClick(String collection, ModifiableSolrParams params, JSONObject result) throws JSONException,
			ParseException {
		String query = params.get("q");
		String requestToken = null;
		if ((query != null) && (result.has("response"))) {
			JSONObject rsp = null;
			try {
				rsp = result.getJSONObject("response");
				if (rsp != null) {
					int numberOfHits = rsp.getInt("numFound");
					Calendar calendar = Calendar.getInstance();
					calendar.setTimeZone(TimeZone.getTimeZone("UTC"));
					long timestamp = calendar.getTimeInMillis();
					requestToken = Utils.createRequestId("SDA_USER", timestamp, query);
					submitClick(collection, query, requestToken, timestamp, numberOfHits);
				}
			} catch (JSONException e) {
				log.debug("Exception", e);
			}
		}
		return requestToken;
	}

	private JSONObject processQueryForSips(String collection, DocServiceRequest request) {
		JSONObject result = null;
		try {
			ModifiableSolrParams params = mapParamsForSips(collection, request.request);
			params.add("wt", new String[] { "json" });
			SolrRequest solrRequest = new QueryRequest(params);
			RawStringCaptureResponseParser responseParser = new RawStringCaptureResponseParser();
			solrRequest.setResponseParser(responseParser);
			try {
				sendSolrRequest(solrRequest);
				result = new JSONObject(responseParser.raw);
			} catch (IOException e) {
				log.error("Exception", e);
				throw new RuntimeException(e);
			} catch (JSONException e) {
				log.error("Exception", e);
				throw new RuntimeException(e);
			}
		} catch (SolrServerException e) {
			log.error("Exception", e);
			throw new RuntimeException(e);
		}
		return result;
	}

	private void submitClick(String collectionName, String query, String requestID, long time, int numberOfHits) {
		RestletContainer<ClickEventResource> collResourceRc = RestletUtil.wrap(ClickEventResource.class,
				getServiceURI(ServiceType.GAIASEARCH.name()), "/collections/" + collectionName + "/click");
		ClickEventResource clickResource = (ClickEventResource) collResourceRc.getWrapped();
		if (clickResource != null) {
			try {
				JSONObject jsonObject = new JSONObject();
				jsonObject.put("type", "q");
				jsonObject.put("req", requestID);
				jsonObject.put("q", query);
				jsonObject.put("qt", time);
				jsonObject.put("hits", numberOfHits);
				InputRepresentation jsonRep = new InputRepresentation(new ReaderInputStream(new StringReader(
						jsonObject.toString())), MediaType.APPLICATION_JSON);
				clickResource.recordEvent(jsonRep);
			} catch (Throwable e) {
				log.error("Not able to log click for" + collectionName + ", query: " + query, e);
			} finally {
				RestletUtil.release(collResourceRc);
			}
		} else {
			log.error("No ClickEventResource for" + collectionName + ", query: " + query);
			throw new RuntimeException("Unable to connect properly to GaiaSearch");
		}
	}

	public State add(String collection, JSONArray documents, JSONObject options, JSONObject boosts) {
		State result = new State(collection, collection);
		result.setStatus(Status.NOT_SUPPORTED);
		result.addProperty("service-impl", getClass().getSimpleName());
		return result;
	}

	protected boolean mustSetDataSource(JSONObject options) {
		boolean mustSetDataSourceId = false;
		try {
			if (options != null) {
				if (((!options.isNull("data_source_id")) && (options.getString("data_source_id").equalsIgnoreCase("true")))
						|| (options.isNull("data_source_id"))) {
					mustSetDataSourceId = true;
				}
				options.remove("data_source_id");
			}
		} catch (JSONException e) {
			log.error("Exception", e);
			throw new RuntimeException(e);
		}
		return mustSetDataSourceId;
	}

	private void addOptions(JSONObject options, UpdateRequest ur) {
		if (options != null) {
			Iterator keys = options.keys();
			while (keys.hasNext()) {
				Object next = keys.next();
				String key = next.toString();
				String value = null;
				value = options.optString(key);
				if (value != null)
					ur.setParam(key, value);
			}
		}
	}

	private String getDataSourceId(String collection) {
		String result = null;
		RestletContainer<DataSourcesResource> dataSourcesResourceRc = RestletUtil.wrap(DataSourcesResource.class,
				getServiceURI(ServiceType.GAIASEARCH.name()), "/collections/" + collection + "/datasources");
		DataSourcesResource dataSourcesResource = (DataSourcesResource) dataSourcesResourceRc.getWrapped();
		try {
			if (dataSourcesResource != null) {
				List<Map<String, Object>> theSources = dataSourcesResource.retrieve();
				for (Iterator<Map<String, Object>> iter = theSources.iterator(); iter.hasNext();) {
					Object theSource = iter.next();
					if ((theSource instanceof Map)) {
						Map<String, Object> sources = (Map) theSource;
						Object name = sources.get("name");
						if ((name != null) && (name.toString().equals(collection + "_SDA_DS"))) {
							Object id = sources.get("id");
							if (id == null) {
								throw new RuntimeException("Can't find external data source");
							}
							result = id.toString();
							log.debug("Data Source Id: {} for collection {}");
							break;
						}
					}
				}
			}
		} catch (Throwable e) {
			throw new RuntimeException(e);
		} finally {
			RestletUtil.release(dataSourcesResourceRc);
		}
		return result;
	}

	protected SolrInputDocument objToSolrDocument(JSONObject value) throws JSONException {
		SolrInputDocument result = new SolrInputDocument();
		Iterator iterator = value.keys();
		while (iterator.hasNext()) {
			String next = iterator.next().toString();
			Object fldVal = value.get(next);
			if ((fldVal instanceof JSONObject)) {
				float boost = Float.parseFloat(((JSONObject) fldVal).get("boost").toString());
				result.addField(next, ((Map) fldVal).get("value"), boost);
			} else if ((fldVal instanceof JSONArray)) {
				JSONArray vals = (JSONArray) fldVal;
				for (int i = 0; i < vals.length(); i++)
					result.addField(next, vals.getString(i));
			} else {
				result.addField(next, fldVal);
			}
		}

		return result;
	}

	public State update(String collection, JSONArray documents, JSONObject options, JSONObject boosts) {
		State result = new State(collection, collection);
		result.setStatus(Status.NOT_SUPPORTED);
		result.addProperty("service-impl", getClass().getSimpleName());
		return result;
	}

	public State delete(String collection, JSONArray deletionIds, JSONObject options) throws JSONException {
		State result = new State(collection, collection);
		result.setStatus(Status.NOT_SUPPORTED);
		result.addProperty("service-impl", getClass().getSimpleName());
		return result;
	}

	static class RawStringCaptureResponseParser extends ResponseParser {
		String raw;
		String wt;

		RawStringCaptureResponseParser() {
			this("json");
		}

		RawStringCaptureResponseParser(String wt) {
			this.wt = wt;
		}

		public String getWriterType() {
			return wt;
		}

		public String getRaw() {
			return raw;
		}

		public NamedList<Object> processResponse(InputStream body, String encoding) {
			return processResponse(new InputStreamReader(body, Charset.forName(encoding)));
		}

		public NamedList<Object> processResponse(Reader reader) {
			StringBuilder bldr = new StringBuilder(1024);
			char[] readBuff = new char[1024];
			try {
				int len = 0;
				while ((len = reader.read(readBuff)) != -1) {
					bldr.append(readBuff, 0, len);
				}
				raw = bldr.toString();
			} catch (IOException e) {
				GaiaDocumentService.log.error("Exception", e);
			}

			NamedList<Object> result = new NamedList<Object>();
			result.add("raw", raw);
			return result;
		}
	}
}
