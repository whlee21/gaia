package gaia.bigdata.api.document;

import gaia.bigdata.api.State;
import gaia.bigdata.api.Status;
import gaia.commons.api.Configuration;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
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

public class DocumentsDeletionSR extends BaseDocumentServerResource implements DocumentsDeletionResource {
	private static transient Logger log = LoggerFactory.getLogger(DocumentsDeletionSR.class);

	@Inject
	public DocumentsDeletionSR(Configuration configuration, Set<DocumentService> docServices, ExecutorService execService) {
		super(configuration, docServices, execService);
	}

	public State delete(JsonRepresentation documents) throws JSONException {
		State result = new State(collection, collection);
		result.setCollection(collection);
		result.setStatus(Status.SUCCEEDED);
		JSONObject container = documents.getJsonObject();
		JSONArray docs = container.optJSONArray("docs");
		JSONObject options = container.optJSONObject("options");
		List<Future> futures = new ArrayList<Future>();
		for (DocumentService docService : docServices) {
			futures.add(executorService.submit(new RemoveCallable(docService, docs, options, collection)));
		}
		try {
			for (Future future : futures) {
				State state = (State) future.get();
				if (state.getStatus().equals(Status.FAILED)) {
					result.setStatus(Status.FAILED);
				}
				result.getChildren().add(state);
			}
		} catch (InterruptedException e) {
			log.error("Exception", e);
			result.setStatus(Status.FAILED);
		} catch (ExecutionException e) {
			log.error("Exception", e);
			result.setStatus(Status.FAILED);
		} finally {
		}

		return result;
	}
}
