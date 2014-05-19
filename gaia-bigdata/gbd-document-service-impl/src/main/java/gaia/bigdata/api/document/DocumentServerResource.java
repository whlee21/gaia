package gaia.bigdata.api.document;

import gaia.bigdata.ResultType;
import gaia.bigdata.api.State;
import gaia.commons.api.Configuration;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Map;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.restlet.ext.json.JsonRepresentation;
import org.restlet.resource.Delete;
import org.restlet.resource.Get;
import org.restlet.resource.Put;
import org.restlet.resource.ResourceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;

public class DocumentServerResource extends BaseDocumentServerResource implements DocumentResource {
	private static transient Logger log = LoggerFactory.getLogger(DocumentServerResource.class);
	private DocumentService docService;
	private String id;

	@Inject
	public DocumentServerResource(Configuration configuration, Set<DocumentService> docServices) {
		super(configuration, docServices, null);
		for (DocumentService service : docServices) {
			if (service.getSupportedResultTypes().contains(ResultType.DOCUMENTS)) {
				docService = service;
				break;
			}
		}
		if (docService == null)
			throw new RuntimeException("None of the DocumentServices specified support the " + ResultType.DOCUMENTS
					+ " result type");
	}

	protected void doInit() throws ResourceException {
		collection = getRequest().getAttributes().get("collection").toString();
		try {
			id = URLDecoder.decode(getRequest().getAttributes().get("id").toString(), "UTF-8");
		} catch (UnsupportedEncodingException e) {
			log.error("Exception", e);
			throw new ResourceException(org.restlet.data.Status.CLIENT_ERROR_NOT_ACCEPTABLE,
					"The ID must be URLEncoded using UTF-8", e);
		}
	}

	@Get
	public Object retrieve(Map<String, Object> request) {
		JSONObject retrieve = docService.retrieve(collection, id, request);
		if (retrieve == null) {
			setStatus(org.restlet.data.Status.CLIENT_ERROR_NOT_FOUND);
			return null;
		}
		return retrieve.toString();
	}

	@Delete
	public State remove() {
		JSONArray ids = new JSONArray();
		ids.put(id);
		try {
			return docService.delete(collection, ids, null);
		} catch (JSONException e) {
			setStatus(org.restlet.data.Status.CLIENT_ERROR_BAD_REQUEST, e);
			State result = new State();
			result.setStatus(gaia.bigdata.api.Status.FAILED);
			return result;
		}
	}

	@Put
	public State update(JsonRepresentation document) {
		try {
			String idField = configuration.getProperties().getProperty("solr.id.field");
			if ((idField != null) && (!idField.isEmpty())) {
				JSONArray docs = document.getJsonArray();
				for (int i = 0; i < docs.length(); i++) {
					JSONObject jsonObject = docs.getJSONObject(i);
					jsonObject.put(idField, id);
				}
				return docService.update(collection, docs, null, null);
			}
			setStatus(org.restlet.data.Status.SERVER_ERROR_INTERNAL);
			State result = new State();
			result.setStatus(gaia.bigdata.api.Status.FAILED);
			return result;
		} catch (JSONException e) {
			setStatus(org.restlet.data.Status.CLIENT_ERROR_BAD_REQUEST, e);
			State result = new State();
			result.setStatus(gaia.bigdata.api.Status.FAILED);
			return result;
		}
	}
}
