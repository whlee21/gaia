package gaia.crawl.api;

import gaia.api.AuditLogger;
import gaia.api.ErrorUtils;
import gaia.crawl.ConnectorManager;
import gaia.jmx.JmxManager;

import javax.ws.rs.core.Response;

import org.restlet.data.Status;
import org.restlet.resource.Delete;

import com.google.inject.Inject;

public class DataSourceCrawlDataServerResource extends DataSourceResourceBase implements DataSourceCrawlDataResource {
	@Inject
	public DataSourceCrawlDataServerResource(ConnectorManager crawlerManager, JmxManager jmx) {
		super(crawlerManager, jmx);
	}

	@Delete
	public void remove() throws Exception {
		if (!isExisting()) {
			throw ErrorUtils.statusExp(Response.Status.NOT_FOUND, "URI not found");
		}
		if (ds != null) {
			deleteCrawlInfo();

			AuditLogger.log("removed datasource crawldata");
		}
		setStatus(Status.SUCCESS_NO_CONTENT);
	}
}
