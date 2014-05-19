package gaia.bigdata.api.document;

import gaia.bigdata.ResultType;
import gaia.bigdata.api.State;
import gaia.commons.api.Configuration;
import gaia.commons.services.ServiceLocator;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.restlet.ext.json.JsonRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;

public class DocumentsServerResource extends BaseDocumentServerResource implements DocumentsResource {
	private static transient Logger log = LoggerFactory.getLogger(DocumentsServerResource.class);
	private ServiceLocator serviceLocator;

	@Inject
	public DocumentsServerResource(Configuration configuration, ServiceLocator serviceLocator,
			ExecutorService execService, Set<DocumentService> docServices) {
		super(configuration, docServices, execService);
		this.serviceLocator = serviceLocator;
	}

	public State add(JsonRepresentation documents) {
		State result = new State(collection, collection);
		result.setCollection(collection);
		result.setStatus(gaia.bigdata.api.Status.SUCCEEDED);
		List<Future<State>> futures = new ArrayList<Future<State>>();
		JSONObject container = null;
		try {
			container = documents.getJsonObject();
		} catch (JSONException e) {
			setStatus(org.restlet.data.Status.CLIENT_ERROR_BAD_REQUEST, e);
			result = new State();
			result.setStatus(gaia.bigdata.api.Status.FAILED);
			return result;
		}
		JSONArray jsonArray = container.optJSONArray("docs");
		JSONObject options = container.optJSONObject("options");
		JSONObject boosts = container.optJSONObject("boosts");
		for (DocumentService docService : docServices) {
			if (docService.getSupportedResultTypes().contains(ResultType.DOCUMENTS))
				futures.add(executorService.submit(new AddCallable(docService, jsonArray, options, boosts)));
		}
		try {
			for (Future<State> future : futures) {
				State state = (State) future.get();
				if (state.getStatus().equals(gaia.bigdata.api.Status.FAILED)) {
					result.setStatus(gaia.bigdata.api.Status.FAILED);
					setStatus(org.restlet.data.Status.SERVER_ERROR_SERVICE_UNAVAILABLE);
				}

				result.getChildren().add(state);
			}
		} catch (InterruptedException e) {
			log.error("Exception", e);
			result.setStatus(gaia.bigdata.api.Status.FAILED);
			setStatus(org.restlet.data.Status.SERVER_ERROR_SERVICE_UNAVAILABLE);
		} catch (ExecutionException e) {
			log.error("Exception", e);
			result.setStatus(gaia.bigdata.api.Status.FAILED);
			setStatus(org.restlet.data.Status.SERVER_ERROR_SERVICE_UNAVAILABLE);
		} finally {
		}

		return result;
	}

	public State update(JsonRepresentation documents) {
		State result = new State(collection, collection);
		result.setCollection(collection);
		result.setStatus(gaia.bigdata.api.Status.SUCCEEDED);
		List<Future> futures = new ArrayList<Future>();
		JSONObject container = null;
		try {
			container = documents.getJsonObject();
		} catch (JSONException e) {
			setStatus(org.restlet.data.Status.CLIENT_ERROR_BAD_REQUEST, e);
			result = new State();
			result.setStatus(gaia.bigdata.api.Status.FAILED);
			return result;
		}
		JSONArray docs = container.optJSONArray("docs");
		JSONObject options = container.optJSONObject("options");
		JSONObject boosts = container.optJSONObject("boosts");
		for (DocumentService docService : docServices) {
			if (docService.getSupportedResultTypes().contains(ResultType.DOCUMENTS))
				futures.add(executorService.submit(new UpdateCallable(docService, docs, options, boosts)));
		}
		try {
			for (Future future : futures) {
				State state = (State) future.get();
				if (state.getStatus().equals(gaia.bigdata.api.Status.FAILED)) {
					result.setStatus(gaia.bigdata.api.Status.FAILED);
					setStatus(org.restlet.data.Status.SERVER_ERROR_SERVICE_UNAVAILABLE);
				}
				result.getChildren().add(state);
			}
		} catch (InterruptedException e) {
			log.error("Exception", e);
			result.setStatus(gaia.bigdata.api.Status.FAILED);
			setStatus(org.restlet.data.Status.SERVER_ERROR_SERVICE_UNAVAILABLE);
		} catch (ExecutionException e) {
			log.error("Exception", e);
			result.setStatus(gaia.bigdata.api.Status.FAILED);
			setStatus(org.restlet.data.Status.SERVER_ERROR_SERVICE_UNAVAILABLE);
		} finally {
		}

		return result;
	}

	public State remove() {
		State result = new State(collection, collection);
		result.setCollection(collection);
		result.setStatus(gaia.bigdata.api.Status.SUCCEEDED);
		JSONArray docs = new JSONArray();
		JSONObject options = new JSONObject();
		try {
			options.put("deleteAll", true);
		} catch (JSONException e) {
			log.debug("Exception", e);
		}
		List<Future> futures = new ArrayList<Future>();
		for (DocumentService docService : docServices) {
			if (docService.getSupportedResultTypes().contains(ResultType.DOCUMENTS)) {
				futures.add(executorService.submit(new RemoveCallable(docService, docs, options, collection)));
			}
		}
		try {
			for (Future future : futures) {
				State state = (State) future.get();
				if (state.getStatus().equals(gaia.bigdata.api.Status.FAILED)) {
					result.setStatus(gaia.bigdata.api.Status.FAILED);
					setStatus(org.restlet.data.Status.SERVER_ERROR_SERVICE_UNAVAILABLE);
				}
				result.getChildren().add(state);
			}
		} catch (InterruptedException e) {
			log.error("Exception", e);
			result.setStatus(gaia.bigdata.api.Status.FAILED);
			setStatus(org.restlet.data.Status.SERVER_ERROR_SERVICE_UNAVAILABLE);
		} catch (ExecutionException e) {
			log.error("Exception", e);
			result.setStatus(gaia.bigdata.api.Status.FAILED);
			setStatus(org.restlet.data.Status.SERVER_ERROR_SERVICE_UNAVAILABLE);
		} finally {
		}

		return result;
	}

	private class UpdateCallable implements Callable<State> {
		private DocumentService ds;
		private JSONArray document;
		private JSONObject options;
		private JSONObject boosts;

		private UpdateCallable(DocumentService ds, JSONArray document, JSONObject options, JSONObject boosts) {
			this.ds = ds;
			this.document = document;
			this.options = options;
			this.boosts = boosts;
		}

		public State call() throws Exception {
			DocumentsServerResource.log.info("Invoking update " + ds.toString() + " DocumentService");
			try {
				return ds.update(collection, document, options, boosts);
			} catch (UnsupportedOperationException e) {
				State notS = new State(collection, collection);
				notS.setStatus(gaia.bigdata.api.Status.NOT_SUPPORTED);
				return notS;
			}
		}
	}

	private class AddCallable implements Callable<State> {
		private DocumentService ds;
		private JSONArray documents;
		private JSONObject options;
		private JSONObject boosts;

		private AddCallable(DocumentService ds, JSONArray documents, JSONObject options, JSONObject boosts) {
			this.ds = ds;
			this.documents = documents;
			this.options = options;
			this.boosts = boosts;
		}

		public State call() throws Exception {
			DocumentsServerResource.log.info("Invoking add " + ds.toString() + " DocumentService");
			try {
				return ds.add(collection, documents, options, boosts);
			} catch (UnsupportedOperationException e) {
				State notS = new State(collection, collection);
				notS.setStatus(gaia.bigdata.api.Status.NOT_SUPPORTED);
				return notS;
			}
		}
	}
}
