package gaia.crawl.api;

import gaia.api.Error;
import gaia.api.ErrorUtils;
import gaia.crawl.ConnectorManager;
import gaia.crawl.batch.BatchStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.restlet.data.Status;
import org.restlet.resource.Delete;
import org.restlet.resource.Get;
import org.restlet.resource.ServerResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;

public class BatchesServerResource extends ServerResource implements BatchesResource {
	private static final Logger LOG = LoggerFactory.getLogger(BatchesServerResource.class);
	private ConnectorManager mgr;

	@Inject
	public BatchesServerResource(ConnectorManager mgr) {
		this.mgr = mgr;
	}

	@Get("json")
	public List<Map<String, Object>> listBatches() throws Exception {
		String crawlerType = (String) getRequest().getAttributes().get("crawler");
		String collection = (String) getRequestAttributes().get("coll_name");
		String dsId = (String) getRequestAttributes().get("id");
		List<Map<String, Object>> res = new ArrayList<Map<String, Object>>();
		List<BatchStatus> stats = mgr.listBatches(crawlerType, collection, dsId);
		for (BatchStatus bs : stats) {
			res.add(bs.toMap());
		}
		return res;
	}

	@Delete("json")
	public void deleteBatch() throws Exception {
		String crawlerType = (String) getRequest().getAttributes().get("crawler");
		String dsId = (String) getRequest().getAttributes().get("id");
		String batchId = (String) getRequest().getAttributes().get("batch_id");
		String collection = (String) getRequestAttributes().get("coll_name");
		if (collection == null) {
			throw ErrorUtils.statusExp(422, new Error("collection", Error.E_MISSING_VALUE));
		}

		List<Error> errors = new ArrayList<Error>();
		try {
			if (!mgr.deleteBatches(crawlerType, collection, batchId))
				;
		} catch (Exception e) {
			errors.add(new Error("deleteBatches", Error.E_EXCEPTION, e.toString()));
		}
		if (errors.size() > 0) {
			throw ErrorUtils.statusExp(422, errors);
		}
		setStatus(Status.SUCCESS_NO_CONTENT);
	}
}
