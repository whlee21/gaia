package gaia.search.server.api.services;

import gaia.admin.collection.CollectionManager;
import gaia.admin.collection.ScheduledSolrCommand;
import gaia.api.Error;
import gaia.api.ErrorUtils;
import gaia.api.ObjectSerializer;
import gaia.crawl.ConnectorManager;
import gaia.crawl.CrawlId;
import gaia.crawl.datasource.DataSource;
import gaia.crawl.datasource.DataSourceId;
import gaia.search.server.api.services.parsers.BodyParseException;
import gaia.search.server.api.services.parsers.RequestBodyParser;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xnap.commons.i18n.I18nFactory;

public class DataSourceJobService  extends BaseService  {

	private static org.xnap.commons.i18n.I18n i18n = I18nFactory.getI18n(DataSourceJobService.class);
	private static final Logger LOG = LoggerFactory.getLogger(DataSourceJobService.class);
	
	private String collection;
	private String dataSourceId;
	private ConnectorManager crawlerManager;
	private CollectionManager cm;
	
	public DataSourceJobService(ObjectSerializer serializer, RequestBodyParser bodyParser, String collectionName,
			String dataSourceId, ConnectorManager crawlerManager, CollectionManager cm) {
		super(serializer, bodyParser);
		this.collection = collectionName;
		this.dataSourceId = dataSourceId;
		this.crawlerManager = crawlerManager;
		this.cm = cm;
	}

	@GET
	@Produces("text/plain")
	public Response getDataSourceJob(@Context HttpHeaders headers, @Context UriInfo ui) throws Exception {
		
		Object result = null;
		
		if (dataSourceId.equals("all")) {
			return getAllDataSourceJob();
		}else {
			return getOneDataSourceJob();
		}
		
	}
	
	public Response getOneDataSourceJob() throws Exception {
		DataSourceId dsId = new DataSourceId(dataSourceId);
		DataSource ds = null;
		try {
			ds = crawlerManager.getDataSource(dsId);
		} catch (Exception e) {
		}
		if (ds == null)
			throw ErrorUtils.statusExp(Response.Status.NOT_FOUND, i18n.tr("URI not found"));
		if ((collection != null) && (!collection.equals(ds.getCollection()))) {
			throw ErrorUtils.statusExp(Response.Status.NOT_FOUND, i18n.tr("Data source " + dataSourceId + " not found in collection "
					+ collection));
		}
		Map<String, Object> result = crawlerManager.getStatus(ds.getCrawlerType(), new CrawlId(ds.getDataSourceId()));
		return buildResponse(Response.Status.OK, result);
	}

	public Response getAllDataSourceJob() throws Exception {
		if (collection == null) {
			throw ErrorUtils.statusExp(Response.Status.NOT_FOUND, i18n.tr("collection not found in gaia "
					+ collection));
		}
		List<Map<String, Object>> allDsJobStatus =  new ArrayList<Map<String, Object>>();
		
		for (DataSource dataSource : crawlerManager.getDataSources(this.collection)) {
			Map<String, Object> result = crawlerManager.getStatus(dataSource.getCrawlerType(), new CrawlId(dataSource.getDataSourceId()));
			allDsJobStatus.add(result);
		}

		return buildResponse(Response.Status.OK, allDsJobStatus);
	}
	
	
	

	@PUT
	@Produces("text/plain")
	public Response startDataSourceJob(String body, @Context HttpHeaders headers, @Context UriInfo ui) throws Exception {
		try {
			RequestBody requestBody = getRequestBody(body);
			return start(requestBody.getProperties());
		} catch (BodyParseException e) {
			throw ErrorUtils.statusExp(Response.Status.INTERNAL_SERVER_ERROR, new Error("body", Error.E_EXCEPTION,
					i18n.tr("cannot parse body " + body)));
		}
	}
	
	public Response start(Map<String, Object> m) throws Exception {
//		boolean onlyActive = StringUtils.getBoolean(m.get("onlyActive")).booleanValue();
		boolean onlyActive = false;
		
		if (m != null) {
			if (m.get("onlyActive") == null) {
				onlyActive = false;
			}else {
				onlyActive = (boolean) m.get("onlyActive");
			}
			LOG.debug("DataSourceJobService start onlyActive: "+onlyActive);
		}
		
		Response.Status statusCode = Response.Status.OK;
		DataSource ds  = null; 
		if (!this.dataSourceId.equals("all")) {
			ds = getDataSource(this.dataSourceId);
		}

		List<Error> errors = new ArrayList<Error>();
		if (ds == null) {
			for (DataSource dataSource : crawlerManager.getDataSources(this.collection)) {
				if (onlyActive) {
					ScheduledSolrCommand cmd = cm.getScheduledSolrCommand(this.collection, dataSource.getDataSourceId().toString());
					if ((cmd == null) || ((cmd.getSchedule() != null) && (!cmd.getSchedule().isActive())))
						;
				} else {
					try {
						crawlerManager.crawl(dataSource.getDataSourceId());
					} catch (Exception e) {
						LOG.warn("crawl command failed for " + dataSource.getDisplayName() + ": " + e);
						errors.add(new Error(dataSource.getDataSourceId().toString(), i18n.tr("error.other.exception.crawl.start", e
								.toString())));
					}
				}
			}
			if (!errors.isEmpty()) {
				throw ErrorUtils.statusExp(422, errors);
			}
//			setStatus(Status.SUCCESS_NO_CONTENT);
			statusCode = Response.Status.NO_CONTENT;
		} else {
			try {
				crawlerManager.crawl(ds.getDataSourceId());
//				setStatus(Status.SUCCESS_NO_CONTENT);
				statusCode = Response.Status.NO_CONTENT;
			} catch (Exception e) {
				e.printStackTrace();
				throw ErrorUtils.statusExp(422, new Error(ds.getDataSourceId().toString(), i18n.tr("error.other.exception.crawl.start"),
						e.toString()));
			}
		}
		
		Response response = buildResponse(statusCode);
		return response;
	}
	
	private DataSource getDataSource(String idStr) throws Exception {
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
			throw ErrorUtils.statusExp(Response.Status.NOT_FOUND, i18n.tr("URI not found"));
		}

		return ds;
	}

	@DELETE
	@Produces("text/plain")
	public Response stopDataSourceJob(String body, @Context HttpHeaders headers, @Context UriInfo ui) throws Exception {
		try {
			RequestBody requestBody = getRequestBody(body);
			return stop(requestBody.getProperties());
		} catch (BodyParseException e) {
			throw ErrorUtils.statusExp(Response.Status.INTERNAL_SERVER_ERROR, new Error("body", Error.E_EXCEPTION,
					i18n.tr("cannot parse body " + body)));
		}
	}
		

	public Response stop(Map<String, Object> m) throws Exception {
		long wait = 120000L;
		boolean abort = false;
		
		if (m != null) {
			if (m.get("wait") == null) {
				wait = 120000L;
			}else {
				wait = (long) m.get("wait");
			}
			
			if (m.get("abort") == null) {
				abort = false;
			}else {
				abort = (boolean) m.get("abort");
			}
		}
//		long wait = p != null ? Long.parseLong(p.getValue()) : 120000L;
		
		DataSourceId dsId = new DataSourceId(dataSourceId);
		DataSource ds = null;
		try {
			ds = crawlerManager.getDataSource(dsId);
		} catch (Exception e) {
		}
		
		if (!dataSourceId.equals("all")) {
			if (ds == null) {
				throw ErrorUtils.statusExp(Response.Status.NOT_FOUND, i18n.tr("URI not found"));
			}
		}
//		p = (Parameter) getQuery().getFirst("abort");
//		boolean abort = p != null ? p.getValue().toLowerCase().equals("true") : false;

		if (ds == null) {
			List<Error> errors = crawlerManager.finishAllJobs(null, collection, abort, wait);
			if (!errors.isEmpty()) {
				throw ErrorUtils.statusExp(422, errors);
			}
			return buildResponse(Response.Status.NO_CONTENT);
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
						i18n.tr(e.toString())));
			}

			return buildResponse(Response.Status.NO_CONTENT);
		}
	}
	
}
