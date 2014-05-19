package gaia.crawl.api;

import gaia.api.Error;
import gaia.api.ErrorUtils;
import gaia.crawl.ConnectorManager;
import gaia.crawl.DataSourceFactoryException;
import gaia.crawl.datasource.DataSource;

import java.util.HashMap;
import java.util.Map;

import org.restlet.resource.Post;
import org.restlet.resource.ResourceException;
import org.restlet.resource.ServerResource;

import com.google.inject.Inject;

public class DataSourceValidatorResource extends ServerResource {
	private String collection;
	private ConnectorManager crawlerManager;

	@Inject
	public DataSourceValidatorResource(ConnectorManager crawlerManager) {
		this.crawlerManager = crawlerManager;
	}

	public void doInit() throws ResourceException {
		this.collection = ((String) getRequestAttributes().get("coll_name"));
	}

	@Post("json")
	public Map<String, Object> validate(Map<String, Object> m) {
		if (m.size() == 0) {
			throw ErrorUtils.statusExp(422, "No input content found");
		}
		String crawlerType = (String) m.get("crawler");
		if (crawlerType == null) {
			throw ErrorUtils.statusExp(422, new Error("crawler", Error.E_MISSING_VALUE, "Missing crawler type."));
		}

		String dsType = (String) m.get("type");
		if (dsType == null) {
			throw ErrorUtils.statusExp(422, new Error("type", Error.E_MISSING_VALUE, "Missing data source type."));
		}

		m.put("collection", this.collection);
		Map<String, Object> res = new HashMap<String, Object>();
		try {
			DataSource ds = this.crawlerManager.createDataSource(m);
		} catch (DataSourceFactoryException dse) {
			res.put("errors", dse.getErrors());
		} catch (Exception e) {
			res.put("exception", e);
		}
		return res;
	}
}
