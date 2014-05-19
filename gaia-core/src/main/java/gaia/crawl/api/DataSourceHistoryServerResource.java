package gaia.crawl.api;

import gaia.api.ErrorUtils;
import gaia.crawl.ConnectorManager;
import gaia.crawl.datasource.DataSource;
import gaia.crawl.datasource.DataSourceId;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.Response;

import org.restlet.resource.Get;
import org.restlet.resource.ResourceException;
import org.restlet.resource.ServerResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;

public class DataSourceHistoryServerResource extends ServerResource implements DataSourceHistoryResource {
	private static transient Logger LOG = LoggerFactory.getLogger(DataSourceHistoryServerResource.class);
	static final String ID = "id";
	private DataSource ds;
	private String collection;
	private ConnectorManager ccm;

	@Inject
	public DataSourceHistoryServerResource(ConnectorManager ccm) {
		this.ccm = ccm;
	}

	public void doInit() throws ResourceException {
		collection = ((String) getRequestAttributes().get("coll_name"));
		String idStr = (String) getRequest().getAttributes().get("id");
		if (idStr == null) {
			setExisting(false);
			return;
		}
		DataSourceId dsId = new DataSourceId(idStr);
		try {
			ds = ccm.getDataSource(dsId);
		} catch (Exception e) {
		} finally {
			boolean existing = ds != null;
			if ((existing) && (collection != null)) {
				existing = collection.equals(ds.getCollection());
			}

			setExisting(existing);
		}
	}

	@Get("json")
	public List<Map<String, Object>> retrieve() throws Exception {
		if (!isExisting()) {
			throw ErrorUtils.statusExp(Response.Status.NOT_FOUND, "URI not found");
		}
		return getHistory(ds, ccm);
	}

	static List<Map<String, Object>> getHistory(DataSource ds, ConnectorManager ccm) throws Exception {
		String id = ds.getDataSourceId().toString();
		List<Map<String, Object>> history = ccm.getHistory(id);
		if (history == null) {
			return new ArrayList<Map<String, Object>>(0);
		}

		return history;
	}
}
