package gaia.crawl.api;

import gaia.api.AuditLogger;
import gaia.crawl.ConnectorManager;
import gaia.jmx.JmxManager;

import org.restlet.data.Status;
import org.restlet.resource.Delete;

import com.google.inject.Inject;

public class DataSourceIndexResource extends DataSourceResourceBase {
	@Inject
	public DataSourceIndexResource(ConnectorManager crawlerManager, JmxManager jmx) {
		super(crawlerManager, jmx);
	}

	@Delete
	public void remove() throws Exception {
//		if (!isExisting()) {
//			throw ErrorUtils.statusExp(ResultStatus.STATUS.NOT_FOUND, "URI not found");
//		}
		if (this.ds != null) {
			deleteCrawlInfo();

			deleteIndex();

			AuditLogger.log("Removed index content for datasource: " + this.ds.getDisplayName());
		}
		setStatus(Status.SUCCESS_NO_CONTENT);
	}
}
