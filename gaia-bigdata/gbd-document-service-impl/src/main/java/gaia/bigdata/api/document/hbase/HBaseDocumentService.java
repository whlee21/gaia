package gaia.bigdata.api.document.hbase;

import gaia.bigdata.ResultType;
import gaia.bigdata.api.State;
import gaia.bigdata.api.Status;
import gaia.bigdata.api.document.DocServiceRequest;
import gaia.bigdata.api.document.DocumentService;
import gaia.bigdata.hbase.documents.Document;
import gaia.bigdata.hbase.documents.DocumentTable;
import gaia.bigdata.services.ServiceType;
import gaia.commons.api.Configuration;
import gaia.commons.services.BaseService;
import gaia.commons.services.ServiceLocator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;

public class HBaseDocumentService extends BaseService implements DocumentService {
	private static transient Logger log = LoggerFactory.getLogger(HBaseDocumentService.class);
	private final DocumentTable table;
	protected EnumSet<ResultType> supportedTypes;

	@Inject
	public HBaseDocumentService(Configuration config, ServiceLocator locator) {
		super(config, locator);
		String zkConnect = config.getProperties().getProperty("hbase.zk.connect");
		if (zkConnect == null) {
			throw new IllegalArgumentException("Missing required config value: hbase.zk.connect");
		}
		table = new DocumentTable(zkConnect);
		supportedTypes = EnumSet.of(ResultType.DOCUMENTS, ResultType.DUPLICATES);
	}

	public EnumSet<ResultType> getSupportedResultTypes() {
		return supportedTypes;
	}

	public String getType() {
		return ServiceType.DOCUMENT.name();
	}

	public JSONObject retrieve(String collection, String id, Map<String, Object> request) {
		return retrieve(collection, id);
	}

	public JSONObject retrieve(String collection, String id) {
		try {
			Document doc = table.getDocument(id, collection, 5);

			if (doc == null) {
				return null;
			}
			JSONObject json = new JSONObject();
			json.put("id", doc.id);
			json.put("text", doc.text);
			json.put("fields", doc.fields);
			json.put("boosts", doc.boosts);
			return json;
		} catch (JSONException e) {
			log.error("Could not construct JSONObject from doc " + id, e);
			throw new RuntimeException(e);
		} catch (IOException e) {
			log.error("Could not retrieve doc " + id, e);
			throw new RuntimeException(e);
		}
	}

	public JSONObject retrieve(String collection, DocServiceRequest request) {
		JSONObject json;
		try {
			Map<String, Double> record = table.listSimilarDocuments(request.request.get("id").toString(), collection);
			json = sortedJsonListFromSortedJsonMap(record);
		} catch (Throwable e) {
			log.error("Exception", e);
			throw new RuntimeException(e);
		}
		return json;
	}

	protected static JSONObject sortedJsonListFromSortedJsonMap(Map<String, Double> record) throws JSONException {
		if (record == null)
			return null;
		JSONObject json = new JSONObject();
		JSONArray docs = new JSONArray();
		for (Map.Entry<String, Double> entry : record.entrySet()) {
			JSONObject jsonObject = new JSONObject();
			jsonObject.put((String) entry.getKey(), entry.getValue());
			docs.put(jsonObject);
		}
		json.put(ResultType.DUPLICATES.toString(), docs);
		return json;
	}

	public State add(String collection, JSONArray documents, JSONObject options, JSONObject boosts)
			throws UnsupportedOperationException {
		List<String> ok = new ArrayList<String>();
		Map<String, Exception> errors = new HashMap<String, Exception>();

		for (int i = 0; i < documents.length(); i++) {
			Map<String, Object> obj;
			try {
				obj = jsonToMap(documents.getJSONObject(i));
			} catch (JSONException e) {
				log.error("Could not read JSON object", e);
				throw new RuntimeException(e);
			}

			String id = (String) obj.get("id");
			Document doc = new Document(id, collection);
			for (String key : obj.keySet()) {
				Object value = obj.get(key);
				if (!key.equals("id")) {
					if (key.equals("text")) {
						if ((value instanceof String))
							doc.text = ((String) value);
						else
							log.warn("Expected 'text' to be a String, got " + value.getClass().toString());
					} else
						doc.fields.put(key, value);
				}
			}
			try {
				if ((options != null) && (!options.isNull("boost"))) {
					doc.boost = Double.valueOf(options.getDouble("boost"));
				}

				if ((boosts != null) && (!boosts.isNull(id))) {
					JSONObject fieldBoosts = boosts.getJSONObject(id);
					for (String field : JSONObject.getNames(fieldBoosts))
						doc.boosts.put(field, Double.valueOf(fieldBoosts.getDouble(field)));
				}
			} catch (JSONException e) {
				log.error("Could not read boosts from JSON object", e);
				throw new RuntimeException(e);
			}
			try {
				table.putDocument(doc);
				ok.add(doc.id);
			} catch (IOException e) {
				log.error("Failed to add doc {}: {}", doc.id, e.getMessage());
				errors.put(doc.id, e);
			}
		}

		State result = new State(collection, collection);
		if (errors.size() == 0) {
			result.setStatus(Status.SUCCEEDED);
		} else if (ok.size() > 0)
			result.setStatus(Status.INCOMPLETE);
		else if (ok.size() == 0) {
			result.setStatus(Status.FAILED);
		}

		result.addProperty("ids", ok);
		result.addProperty("errors", errors);
		return result;
	}

	public State update(String collection, JSONArray documents, JSONObject options, JSONObject boosts)
			throws UnsupportedOperationException {
		List<String> ok = new ArrayList<String>();
		Map<String, Exception> errors = new HashMap<String, Exception>();

		for (int i = 0; i < documents.length(); i++) {
			Map<String, Object> obj;
			try {
				obj = jsonToMap(documents.getJSONObject(i));
			} catch (JSONException e) {
				log.error("Could not read JSON object", e);
				throw new RuntimeException(e);
			}
			String id = (String) obj.get("id");
			Document doc;
			try {
				doc = table.getDocument(id, collection, 5);
			} catch (IOException e) {
				log.error("Document {} cannot be updated since it does not exist", id);
				errors.put(id, e);
				continue;
			}
			for (String key : obj.keySet()) {
				Object value = obj.get(key);
				if (!key.equals("id")) {
					if (key.equals("text")) {
						if ((value instanceof String))
							doc.text = ((String) value);
						else
							log.warn("Expected 'text' to be a String, got " + value.getClass().toString());
					} else
						doc.fields.put(key, value);
				}
			}
			try {
				if ((options != null) && (!options.isNull("boost"))) {
					doc.boost = Double.valueOf(options.getDouble("boost"));
				}

				if ((boosts != null) && (!boosts.isNull(id))) {
					JSONObject fieldBoosts = boosts.getJSONObject(id);
					for (String field : JSONObject.getNames(fieldBoosts))
						doc.boosts.put(field, Double.valueOf(fieldBoosts.getDouble(field)));
				}
			} catch (JSONException e) {
				log.error("Could not read boosts from JSON object", e);
				throw new RuntimeException(e);
			}
			try {
				table.putDocument(doc);
				ok.add(doc.id);
			} catch (IOException e) {
				log.error("Failed to update doc {}: {}", doc.id, e.getMessage());
				errors.put(doc.id, e);
			} catch (ConcurrentModificationException e) {
				log.error("Failed to update doc {}: {}", doc.id, e.getMessage());
				errors.put(doc.id, e);
			}
		}

		State result = new State(collection, collection);
		if (errors.size() == 0) {
			result.setStatus(Status.SUCCEEDED);
		} else if (ok.size() > 0)
			result.setStatus(Status.INCOMPLETE);
		else if (ok.size() == 0) {
			result.setStatus(Status.FAILED);
		}

		result.addProperty("ids", ok);
		result.addProperty("errors", errors);
		return result;
	}

	public State delete(String collection, JSONArray toDelete, JSONObject options) throws UnsupportedOperationException,
			JSONException {
		if ((options != null) && (options.getBoolean("deleteAll") == true)) {
			return deleteAll(collection);
		}

		List<String> ok = new ArrayList<String>();
		Map<String, Exception> errors = new HashMap<String, Exception>();

		for (int i = 0; i < toDelete.length(); i++) {
			String id = (String) toDelete.get(i);
			try {
				table.deleteDocument(id, collection);
				ok.add(id);
			} catch (IOException e) {
				log.error("Document {} cannot be deleted since it does not exist", id);
				errors.put(id, e);
			}

		}

		State result = new State(collection, collection);
		if (errors.size() == 0) {
			result.setStatus(Status.SUCCEEDED);
		} else if (ok.size() > 0)
			result.setStatus(Status.INCOMPLETE);
		else if (ok.size() == 0) {
			result.setStatus(Status.FAILED);
		}

		result.addProperty("ids", ok);
		result.addProperty("errors", errors);
		return result;
	}

	public State deleteAll(String collection) {
		log.info("Deleting all docs from {}", collection);
		State result = new State(collection, collection);
		result.addProperty("service-impl", getClass().getSimpleName());
		try {
			table.deleteAllDocuments(collection);
			result.setStatus(Status.SUCCEEDED);
		} catch (IOException e) {
			log.error("Failed to delete all documents", e);
			result.setStatus(Status.FAILED);
		}
		return result;
	}

	private Map<String, Object> jsonToMap(JSONObject json) throws JSONException {
		Map<String, Object> map = new HashMap<String, Object>();
		for (String key : JSONObject.getNames(json)) {
			map.put(key, json.get(key));
		}
		return map;
	}

	private State getNoOp(String collection) {
		State result = new State(collection, collection);
		result.setStatus(Status.NOT_SUPPORTED);
		result.addProperty("service-impl", getClass().getSimpleName());
		return result;
	}
}
