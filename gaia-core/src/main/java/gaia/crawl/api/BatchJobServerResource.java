package gaia.crawl.api;

import gaia.api.Error;
import gaia.api.ErrorUtils;
import gaia.crawl.ConnectorManager;
import gaia.crawl.CrawlId;
import gaia.crawl.datasource.DataSource;
import gaia.crawl.datasource.DataSourceId;
import gaia.utils.StringUtils;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.restlet.data.Status;
import org.restlet.resource.Delete;
import org.restlet.resource.Get;
import org.restlet.resource.Put;
import org.restlet.resource.ServerResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;

public class BatchJobServerResource extends ServerResource implements BatchJobResource {
	private static final Logger LOG = LoggerFactory.getLogger(BatchJobServerResource.class);
	private ConnectorManager mgr;

	@Inject
	public BatchJobServerResource(ConnectorManager mgr) {
		this.mgr = mgr;
	}

	private String getCollection() throws Exception {
		String collection = (String) getRequestAttributes().get("coll_name");
		return collection;
	}

	@Put("json")
	public void start(Map<String, Object> map) throws Exception {
		List<Error> errors = new ArrayList<Error>();
		String collection = getCollection();
		String newDsId = (String) map.get("ds_id");
		String batchId = (String) map.get("batch_id");
		String crawlerType = (String) getRequest().getAttributes().get("crawler");

		if (collection == null) {
			errors.add(new Error("collection", Error.E_MISSING_VALUE));
		}
		if (batchId == null) {
			errors.add(new Error("batch_id", Error.E_MISSING_VALUE));
		}
		if (crawlerType == null) {
			errors.add(new Error("crawler", Error.E_MISSING_VALUE));
		}
		String newCollection = (String) map.get("new_collection");
		if (errors.size() > 0) {
			throw ErrorUtils.statusExp(422, errors);
		}
		boolean parse = map.get("parse") != null ? StringUtils.getBoolean(map.get("parse")).booleanValue() : false;
		boolean index = map.get("index") != null ? StringUtils.getBoolean(map.get("index")).booleanValue() : true;

		DataSource ds = null;
		if (newDsId != null) {
			ds = mgr.getDataSource(new DataSourceId(newDsId));
			if (ds == null) {
				throw ErrorUtils.statusExp(422, new Error("new_ds_id", Error.E_NOT_FOUND, "unknown data source id " + newDsId));
			}
		}

		try {
			mgr.startBatchJob(crawlerType, newCollection, batchId, ds, parse, index);
			setStatus(Status.SUCCESS_NO_CONTENT);
		} catch (Exception e) {
			throw ErrorUtils.statusExp(422, e.toString());
		}
	}

	@Get("json")
	public List<Map<String, Object>> status() throws Exception {
		String collection = getCollection();
		String crawlerType = (String) getRequest().getAttributes().get("crawler");
		String batchId = (String) getRequest().getAttributes().get("batch_id");
		List<Error> errors = new ArrayList<Error>();
		if (crawlerType == null) {
			errors.add(new Error("crawler", Error.E_MISSING_VALUE));
		}
		if (errors.size() > 0)
			throw ErrorUtils.statusExp(422, errors);
		try {
			List<Map<String, Object>> res = mgr.listJobs(crawlerType, true);
			List<Map<String, Object>> filtered = new LinkedList<Map<String, Object>>();
			for (Map<String, Object> st : res)
				if ((batchId == null) || (batchId.equals(st.get("job_id")))) {
					if (collection != null) {
						String id = (String) st.get("id");
						if (id != null) {
							DataSource ds = mgr.getDataSource(new DataSourceId(id));
							if ((ds == null) || (!collection.equals(ds.getCollection())))
								;
						}

					} else {
						filtered.add(st);
					}
				}
			return filtered;
		} catch (Exception e) {
			throw ErrorUtils.statusExp(e);
		}
	}

	@Delete("json")
	public void stop() throws Exception {
		String crawlerType = (String) getRequest().getAttributes().get("crawler");
		String batchId = (String) getRequest().getAttributes().get("batch_id");
		List<Error> errors = new ArrayList<Error>();
		if (batchId == null) {
			errors.add(new Error("batch_id", Error.E_MISSING_VALUE));
		}
		if (crawlerType == null) {
			errors.add(new Error("crawler", Error.E_MISSING_VALUE));
		}
		if (errors.size() > 0) {
			throw ErrorUtils.statusExp(422, errors);
		}
		CrawlId cid = new CrawlId(batchId);
		Map<String, Object> status = mgr.getStatus(crawlerType, cid);
		if ((status == null) || (status.get("crawl_state").equals("IDLE"))) {
			// setStatus(ResultStatus.STATUS.NOT_FOUND);
			return;
		}
		try {
			mgr.stopJob(crawlerType, cid, false, 5000L);
			setStatus(Status.SUCCESS_NO_CONTENT);
		} catch (Exception e) {
			throw ErrorUtils.statusExp(e);
		}
	}
}
