package gaia.bigdata.api.document;

import gaia.bigdata.api.State;
import gaia.bigdata.api.Status;
import java.util.concurrent.Callable;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class RemoveCallable implements Callable<State> {
	private static transient Logger log = LoggerFactory.getLogger(RemoveCallable.class);
	private DocumentService ds;
	private JSONArray document;
	private JSONObject options;
	private String collection;

	RemoveCallable(DocumentService ds, JSONArray document, JSONObject options, String collection) {
		this.ds = ds;
		this.document = document;
		this.options = options;
		this.collection = collection;
	}

	public State call() throws Exception {
		log.info("Invoking delete " + ds.toString() + " DocumentService");
		try {
			return ds.delete(collection, document, options);
		} catch (UnsupportedOperationException e) {
			State notS = new State(collection, collection);
			notS.setStatus(Status.NOT_SUPPORTED);
			return notS;
		}
	}
}
