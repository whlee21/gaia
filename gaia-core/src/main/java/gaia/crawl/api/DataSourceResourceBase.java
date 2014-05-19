package gaia.crawl.api;

import gaia.crawl.ConnectorManager;
import gaia.crawl.datasource.DataSource;
import gaia.crawl.datasource.DataSourceId;
import gaia.jmx.JmxManager;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;
import org.restlet.Request;
import org.restlet.resource.ResourceException;
import org.restlet.resource.ServerResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DataSourceResourceBase extends ServerResource {
	private static final transient Logger LOG = LoggerFactory.getLogger(DataSourceResourceBase.class);
	protected String collection;
	protected DataSource ds;
	protected ConnectorManager crawlerManager;
	protected JmxManager jmx;

	public DataSourceResourceBase(ConnectorManager crawlerManager, JmxManager jmx) {
		this.crawlerManager = crawlerManager;
		this.jmx = jmx;
	}

	public void doInit() throws ResourceException {
		collection = ((String) getRequestAttributes().get("coll_name"));

		String idStr = (String) getRequest().getAttributes().get("id");
		if (idStr == null) {
			LOG.info("no dsId");
			setExisting(false);
			return;
		}
		DataSourceId dsId = new DataSourceId(idStr);
		try {
			try {
				ds = crawlerManager.getDataSource(dsId);
			} catch (Exception e) {
				LOG.warn("Exception getting data source: " + e.toString(), e);
			}
		} finally {
			if (collection != null)
				setExisting((ds != null) && (collection.equals(ds.getCollection())));
			else
				setExisting(ds != null);
		}
	}

	protected void deleteIndex() throws Exception {
		crawlerManager.deleteOutputData(ds.getDataSourceId());
	}

	protected void deleteCrawlInfo() throws Exception {
		crawlerManager.reset(ds.getCrawlerType(), ds.getCollection(), ds.getDataSourceId());
	}
}
