package gaia.bigdata.api.document;

import gaia.bigdata.ResultType;
import gaia.bigdata.api.State;
import gaia.commons.api.Configuration;
import gaia.commons.util.JSONWrapper;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;

import org.json.JSONObject;
import org.restlet.data.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;

public class DocumentsRetrievalServerResource extends BaseDocumentServerResource implements DocumentsRetrievalResource {
	private static transient Logger log = LoggerFactory.getLogger(DocumentsRetrievalServerResource.class);

	@Inject
	public DocumentsRetrievalServerResource(Configuration configuration, ExecutorService execService,
			Set<DocumentService> docServices) {
		super(configuration, docServices, execService);
	}

	public Map<ResultType, Object> retrieve(Map<String, Object> request) {
		EnumMap<ResultType, Object> result = new EnumMap<ResultType, Object>(ResultType.class);
		log.info("Request: " + request);
		RequestsTracker requests = RequestsTracker.getRequests(dsMap, request);
		List<State> unfulfilledResults = new ArrayList<State>();
		if ((requests != null) && (!requests.supported.isEmpty())) {
			List<FutureWrapper> futures = new ArrayList<FutureWrapper>();
			for (DocServiceRequest req : requests.supported) {
				FutureWrapper wrapper = new FutureWrapper();
				wrapper.future = executorService.submit(new RetrieveCallable(req));
				wrapper.origRequest = req;
				futures.add(wrapper);
			}
			for (FutureWrapper future : futures) {
				try {
					JSONObject results = (JSONObject) future.future.get();

					if (results != null) {
						result.put(future.origRequest.resultType, new JSONWrapper(results.toString()));
					} else {
						log.warn("No results available for request: {}, service: {}", future.origRequest.resultType,
								future.origRequest.getService());
						State unfill = new State();
						unfill.setErrorMsg("No results available for " + future.origRequest.resultType + " and service: "
								+ future.origRequest.getService());
						unfulfilledResults.add(unfill);
					}
				} catch (InterruptedException e) {
				} catch (ExecutionException e) {
					log.warn("Exception", e);
					State unfill = new State();
					unfill.setThrowable(e);
					unfulfilledResults.add(unfill);
				}
			}

			if ((requests.unsupported != null) && (!requests.unsupported.isEmpty())) {
				String bldr = RequestsTracker.createUnsupportedMessage(requests);
				State state = new State();
				state.setErrorMsg(bldr);
			}

		} else if ((requests.unsupported != null) && (!requests.unsupported.isEmpty())) {
			String bldr = RequestsTracker.createUnsupportedMessage(requests);
			setStatus(Status.SERVER_ERROR_SERVICE_UNAVAILABLE, bldr.toString());
		} else {
			setStatus(Status.SERVER_ERROR_SERVICE_UNAVAILABLE);
		}

		if (unfulfilledResults.size() > 0) {
			result.put(ResultType.FAILED, unfulfilledResults);
		}
		return result;
	}

	private class RetrieveCallable implements Callable<JSONObject> {
		private DocServiceRequest dsr;

		private RetrieveCallable(DocServiceRequest request) {
			dsr = request;
		}

		public JSONObject call() throws Exception {
			log.info("Invoking " + dsr.getService().toString() + " DocumentService for " + dsr.resultType);
			return dsr.getService().retrieve(collection, dsr);
		}
	}
}
