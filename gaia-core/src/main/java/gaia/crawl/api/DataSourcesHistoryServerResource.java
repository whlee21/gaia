package gaia.crawl.api;

import gaia.api.ErrorUtils;
import gaia.crawl.ConnectorManager;
import gaia.crawl.datasource.DataSource;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.Response;

import org.restlet.resource.Get;
import org.restlet.resource.ResourceException;
import org.restlet.resource.ServerResource;

import com.google.inject.Inject;

public class DataSourcesHistoryServerResource extends ServerResource implements DataSourcesHistoryResource {
	private ConnectorManager ccm;
	private String collection;

	@Inject
	public DataSourcesHistoryServerResource(ConnectorManager ccm) {
		this.ccm = ccm;
	}

	public void doInit() throws ResourceException {
		collection = ((String) getRequestAttributes().get("coll_name"));
	}

	@Get("json")
	public List<Map<String, Object>> retrieve() throws Exception {
		if (!isExisting()) {
			throw ErrorUtils.statusExp(Response.Status.NOT_FOUND, "URI not found");
		}
		List<Map<String, Object>> histories = new ArrayList<Map<String, Object>>();

		for (DataSource ds : ccm.getDataSources(collection)) {
			List<Map<String, Object>> history = DataSourceHistoryServerResource.getHistory(ds, ccm);
			Map<String, Object> entry = new HashMap<String, Object>();
			entry.put("id", ds.getDataSourceId().toString());
			entry.put("history", history);
			histories.add(entry);
		}
		return histories;
	}
}
