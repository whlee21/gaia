package gaia.crawl.api;

import gaia.admin.collection.CollectionManager;
import gaia.admin.collection.ScheduledSolrCommand;
import gaia.api.Error;
import gaia.api.ErrorUtils;
import gaia.crawl.ConnectorManager;
import gaia.crawl.CrawlId;
import gaia.crawl.datasource.DataSource;
import gaia.crawl.datasource.DataSourceId;
import gaia.utils.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.Response;

import org.restlet.data.Parameter;
import org.restlet.data.Status;
import org.restlet.resource.Delete;
import org.restlet.resource.Get;
import org.restlet.resource.Put;
import org.restlet.resource.ServerResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;

public class DataSourceJobServerResource extends ServerResource implements DataSourceJobResource {
	private static Logger LOG = LoggerFactory.getLogger(DataSourceJobServerResource.class);
	private ConnectorManager crawlerManager;
	private CollectionManager cm;

	@Inject
	public DataSourceJobServerResource(ConnectorManager crawlerManager, CollectionManager cm) {
		this.crawlerManager = crawlerManager;
		this.cm = cm;
	}

	private String getCollection() throws Exception {
		String collection = (String) getRequestAttributes().get("coll_name");
		return collection;
	}

	private DataSource getDataSource() throws Exception {
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
		if (ds == null) {
			throw ErrorUtils.statusExp(Response.Status.NOT_FOUND, "URI not found");
		}

		return ds;
	}

	@Put("json")
	public void start(Map<String, Object> m) throws Exception {
		boolean onlyActive = StringUtils.getBoolean(getQuery().getFirstValue("onlyActive", "false")).booleanValue();

		DataSource ds = getDataSource();
		String collection = getCollection();
		List<Error> errors = new ArrayList<Error>();
		if (ds == null) {
			for (DataSource dataSource : crawlerManager.getDataSources(collection)) {
				if (onlyActive) {
					ScheduledSolrCommand cmd = cm.getScheduledSolrCommand(collection, dataSource.getDataSourceId().toString());
					if ((cmd == null) || ((cmd.getSchedule() != null) && (!cmd.getSchedule().isActive())))
						;
				} else {
					try {
						crawlerManager.crawl(dataSource.getDataSourceId());
					} catch (Exception e) {
						LOG.warn("crawl command failed for " + dataSource.getDisplayName() + ": " + e);
						errors.add(new Error(dataSource.getDataSourceId().toString(), "error.other.exception.crawl.start", e
								.toString()));
					}
				}
			}
			if (!errors.isEmpty()) {
				throw ErrorUtils.statusExp(422, errors);
			}
			setStatus(Status.SUCCESS_NO_CONTENT);
		} else {
			try {
				crawlerManager.crawl(ds.getDataSourceId());
				setStatus(Status.SUCCESS_NO_CONTENT);
			} catch (Exception e) {
				throw ErrorUtils.statusExp(422, new Error(ds.getDataSourceId().toString(), "error.other.exception.crawl.start",
						e.toString()));
			}
		}
	}

	@Get("json")
	public Object status() throws Exception {
		return DataSourceJobStatusServerResource.doStatus(getCollection(), getDataSource(), crawlerManager);
	}

	@Delete("json")
	public void stopOrAbort() throws Exception {
		Parameter p = (Parameter) getQuery().getFirst("wait");
		long wait = p != null ? Long.parseLong(p.getValue()) : 120000L;
		p = (Parameter) getQuery().getFirst("abort");
		boolean abort = p != null ? p.getValue().toLowerCase().equals("true") : false;
		DataSource ds = getDataSource();
		String collection = getCollection();

		if (ds == null) {
			List<Error> errors = crawlerManager.finishAllJobs(null, collection, abort, wait);
			if (!errors.isEmpty()) {
				throw ErrorUtils.statusExp(422, errors);
			}
			setStatus(Status.SUCCESS_NO_CONTENT);
		} else {
			try {
				String crawler = ds.getCrawlerType();
				CrawlId jobId = new CrawlId(ds.getDataSourceId());
				if (crawlerManager.getStatus(crawler, jobId) != null)
					if (abort)
						crawlerManager.stopJob(crawler, jobId, true, wait > 0L ? wait : 5000L);
					else
						crawlerManager.stopJob(crawler, jobId, false, wait > 0L ? wait : 5000L);
			} catch (Exception e) {
				throw ErrorUtils.statusExp(422, new Error(ds.getDataSourceId().toString(), "error.other.exception.crawl.stop",
						e.toString()));
			}

			setStatus(Status.SUCCESS_NO_CONTENT);
		}
	}
}
