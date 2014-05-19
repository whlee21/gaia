package gaia.crawl.api;

import gaia.api.ErrorUtils;
import gaia.crawl.ConnectorManager;
import gaia.crawl.CrawlId;
import gaia.crawl.datasource.DataSource;
import gaia.crawl.datasource.DataSourceId;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.Response;

import org.restlet.resource.Get;
import org.restlet.resource.ServerResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;

public class DataSourceJobStatusServerResource extends ServerResource implements DataSourceJobStatusResource {
	private static Logger LOG = LoggerFactory.getLogger(DataSourceJobStatusServerResource.class);
	private ConnectorManager crawlerManager;

	@Inject
	public DataSourceJobStatusServerResource(ConnectorManager crawlerManager) {
		this.crawlerManager = crawlerManager;
	}

	private String getCollection() throws Exception {
		String collection = (String) getRequestAttributes().get("coll_name");
		return collection;
	}

	private DataSource getDataSource(String collection) throws Exception {
		String idStr = (String) getRequest().getAttributes().get("id");
		if (idStr == null) {
			return null;
		}
		DataSourceId dsId = new DataSourceId(idStr);
		DataSource ds = null;
		try {
			ds = crawlerManager.getDataSource(dsId);
		} catch (Exception e) {
		}
		if (ds == null)
			throw ErrorUtils.statusExp(Response.Status.NOT_FOUND, "URI not found");
		if ((collection != null) && (!collection.equals(ds.getCollection()))) {
			throw ErrorUtils.statusExp(Response.Status.NOT_FOUND, "Data source " + idStr + " not found in collection "
					+ collection);
		}

		return ds;
	}

	@Get("json")
	public Object status() throws Exception {
		String collection = getCollection();
		return doStatus(collection, getDataSource(collection), crawlerManager);
	}

	static Object doStatus(String collection, DataSource ds, ConnectorManager crawlerManager) throws Exception {
		if (ds == null) {
			List<Map<String, Object>> res = new ArrayList<Map<String, Object>>();

			for (DataSource dataSource : crawlerManager.getDataSources(collection)) {
				Map<String, Object> stat = crawlerManager.getStatus(dataSource.getCrawlerType(),
						new CrawlId(dataSource.getDataSourceId()));

				if (stat != null) {
					res.add(stat);
				}
			}
			return res;
		}
		return crawlerManager.getStatus(ds.getCrawlerType(), new CrawlId(ds.getDataSourceId()));
	}
}
