package gaia.search.server.api.services;

import gaia.Constants;
import gaia.admin.collection.AdminScheduler;
import gaia.admin.collection.CollectionManager;
import gaia.api.APIUtils;
import gaia.api.AuditLogger;
import gaia.api.Error;
import gaia.api.ErrorUtils;
import gaia.api.ObjectSerializer;
import gaia.crawl.ConnectorManager;
import gaia.crawl.CrawlId;
import gaia.crawl.DataSourceFactoryException;
import gaia.crawl.DataSourceManager;
import gaia.crawl.datasource.DataSource;
import gaia.crawl.datasource.DataSourceId;
import gaia.jmx.JmxManager;
import gaia.search.server.api.services.parsers.BodyParseException;
import gaia.search.server.api.services.parsers.RequestBodyParser;
import gaia.search.server.configuration.Configuration;

import java.net.URI;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.solr.core.CoreContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xnap.commons.i18n.I18nFactory;

public class DataSourceService extends BaseService {
	private static org.xnap.commons.i18n.I18n i18n = I18nFactory.getI18n(DataSourceService.class);
	private static final Logger LOG = LoggerFactory.getLogger(DataSourceService.class);

	private String collectionName;
	private ConnectorManager crawlerManager;
	private JmxManager jmx;
	private CollectionManager cm;
	private CoreContainer cores;
	private DataSourceManager dm;
	private Configuration configuration;
	protected DataSource ds;
	private AdminScheduler adminScheduler;
	private volatile boolean existing = true;

	public DataSourceService(ObjectSerializer serializer, RequestBodyParser bodyParser, String collectionName,
			ConnectorManager crawlerManager, JmxManager jmx, CollectionManager cm, CoreContainer cores, DataSourceManager dm,
			Configuration configuration, AdminScheduler adminScheduler) {
		super(serializer, bodyParser);
		this.collectionName = collectionName;
		this.crawlerManager = crawlerManager;
		this.jmx = jmx;
		this.cm = cm;
		this.cores = cores;
		this.dm = dm;
		this.configuration = configuration;
		this.adminScheduler = adminScheduler;
	}

	public void setExisting(boolean exists) {
		this.existing = exists;
	}

	public boolean isExisting() {
		return this.existing;
	}

	@GET
	@Path("{dataSourceId}")
	@Produces("text/plain")
	public Response getDataSource(String body, @Context HttpHeaders headers, @Context UriInfo ui,
			@PathParam("dataSourceId") String dataSourceId) {

		String idStr = dataSourceId;
		if (idStr == null) {
			LOG.info("no dsId");
			return null;
		}
		DataSourceId dsId = new DataSourceId(idStr);
		try {
			try {
				ds = crawlerManager.getDataSource(dsId);
				return buildResponse(Response.Status.OK, ds.toMap());
			} catch (Exception e) {
				throw ErrorUtils.statusExp(Response.Status.NOT_FOUND, "URI not found");
			}
		} finally {
		}

	}

	@GET
	@Produces("text/plain")
	public Response getDataSources(String body, @Context HttpHeaders headers, @Context UriInfo ui) {
		List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
		try {
			for (DataSource ds : crawlerManager.getDataSources(this.collectionName))
				result.add(ds.toMap());
		} catch (Exception e) {
			throw ErrorUtils.statusExp(422, new Error("getDataSources", Error.E_EXCEPTION, i18n.tr(e.toString())));
		}

		return buildResponse(Response.Status.OK, result);
	}

	@POST
	@Produces("text/plain")
	public Response createCollectionDataSources(String body, @Context HttpHeaders headers, @Context UriInfo ui)
			throws Exception {
		try {
			RequestBody requestBody = getRequestBody(body);
			return createDataSource(requestBody.getProperties());
		} catch (BodyParseException e) {
			throw ErrorUtils.statusExp(Response.Status.INTERNAL_SERVER_ERROR,
					new Error("body", Error.E_EXCEPTION, i18n.tr("cannot parse body " + body)));
		}
	}

	private Response createDataSource(Map<String, Object> m) throws Exception {
		if (m.size() == 0) {
			throw ErrorUtils.statusExp(422, i18n.tr("No input content found"));
		}

		List<Error> errors = new ArrayList<Error>();
		if (Constants.LOGS_COLLECTION.equals(this.collectionName)) {
			errors.add(new Error("collection", Error.E_FORBIDDEN_VALUE, i18n
					.tr("users can't create other datasources in this collection")));

			throw ErrorUtils.statusExp(422, errors);
		}

		String type = (String) m.get("type");
		String crawlerClass = (String) m.get("crawler");
		if (m.get("collection") == null) {
			m.put("collection", this.collectionName);
		} else if (!this.collectionName.equals(m.get("collection"))) {
			errors.add(new Error("collection", Error.E_INVALID_VALUE, i18n
					.tr("collection specified in datasource doesn't match this collection")));

			throw ErrorUtils.statusExp(422, errors);
		}

		DataSource ds = null;

		if (crawlerClass == null) {
			errors.add(new Error("crawler", Error.E_MISSING_VALUE, i18n.tr("Missing crawler type")));
		}

		if (type == null) {
			errors.add(new Error("type", Error.E_NULL_VALUE, i18n.tr("Missing data source type")));
		}

		if (m.get("id") != null) {
			errors.add(new Error("id", Error.E_FORBIDDEN_VALUE, i18n.tr("cannot set id on creation")));
		}

		boolean success = false;
		if (errors.size() == 0) {
			if (!crawlerManager.isAvailable(crawlerClass)) {
				errors.add(new Error("crawler", Error.E_INVALID_VALUE, i18n.tr("unknown crawler type " + crawlerClass)));

				throw ErrorUtils.statusExp(422, errors);
			}

			Map spec = crawlerManager.getCrawlerSpec(crawlerClass, type);
			if (spec == null) {
				errors.add(new Error("type", Error.E_INVALID_VALUE, i18n.tr("unknown data source type " + type)));

				throw ErrorUtils.statusExp(422, errors);
			}
			try {
				ds = crawlerManager.createDataSource(m);
			} catch (DataSourceFactoryException e) {
				errors.addAll(e.getErrors());
			}
			if (errors.size() == 0) {
				success = crawlerManager.addDataSource(ds);
			}
		}

		if (errors.size() > 0) {
			throw ErrorUtils.statusExp(422, errors);
		}

		if (ds == null) {
			errors.add(new Error("type", Error.E_EXCEPTION, i18n.tr("unable to create data source :" + type)));

			throw ErrorUtils.statusExp(422, errors);
		}

		if (success) {
			jmx.registerDataSourceMBean(ds.getDataSourceId());

			APIUtils.ensureSolrFieldMappingConfig(this.collectionName, cm, cores);

			APIUtils.toggleConnectorsSecuritySearchComponent(this.collectionName, cm, cores, dm);

			// getResponse().setLocationRef("datasources/" + ds.getDataSourceId());
			// setStatus(Status.SUCCESS_CREATED);
			AuditLogger.log("added Datasource");

			// return ds.toMap();
			URI seeOther = configuration.getCollectionUri("datasources/" + ds.getDataSourceId());
			Response response = buildResponse(Response.Status.CREATED, seeOther, ds.toMap());
			return response;
		}
		throw ErrorUtils.statusExp(422,
				new Error("add_data_source", "unspecified.error", i18n.tr("operation failed - see logs for more details")));
	}

	@PUT
	@Path("{dataSourceId}")
	@Produces("text/plain")
	public Response updateCollectionDataSource(String body, @Context HttpHeaders headers, @Context UriInfo ui,
			@PathParam("dataSourceId") String dataSourceId) throws Exception {
		// return handleRequest(headers, body, ui, Request.Type.PUT,
		// createDataSourceResource(collectionName, dataSourceId));
		LOG.debug("###   hhokyung updateCollectionDataSource!!");
		doInit(dataSourceId);
		RequestBody requestBody = null;
		try {
			requestBody = getRequestBody(body);
			// return createDataSource(requestBody.getProperties());
		} catch (BodyParseException e) {
			throw ErrorUtils.statusExp(Response.Status.INTERNAL_SERVER_ERROR,
					new Error("body", Error.E_EXCEPTION, i18n.tr("cannot parse body " + body)));
		}
		Map<String, Object> map = requestBody.getProperties();
		Response resultVal = update(map);
		return resultVal;
	}

	public void doInit(String dataSourceId) {

		String idStr = dataSourceId;
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
				LOG.warn(i18n.tr("Exception getting data source: " + e.toString()), e);
			}
		} finally {
			if (collectionName != null)
				setExisting((ds != null) && (collectionName.equals(ds.getCollection())));
			else
				setExisting(ds != null);
		}
	}

	public Response update(Map<String, Object> m) throws Exception {
		LOG.debug("#### update target map: " + m);
		if (!isExisting()) {
			throw ErrorUtils.statusExp(Response.Status.NOT_FOUND, i18n.tr("URI not found"));
		}
		if ((m == null) || (m.size() == 0)) {
			throw ErrorUtils.statusExp(422, i18n.tr("No input content found"));
		}

		if (isExisting()) {
			if (m.get("id") != null) {
				throw ErrorUtils.statusExp(422,
						new Error("id", Error.E_FORBIDDEN_VALUE, i18n.tr("cannot specify id on update")));
			}

			Map<String, Object> oldDs = ds.toMap();
			if (m.containsKey("mapping")) {
				oldDs.remove("mapping");
			}
			oldDs.putAll(m);
			oldDs.put("id", ds.getDataSourceId());
			m = oldDs;

			String oldCrawler = ds.getCrawlerType();
			String oldType = ds.getType();
			String crawler = (String) m.get("crawler");
			String type = (String) m.get("type");

			if (((type != null) && (!type.equals(oldType))) || ((crawler != null) && (!crawler.equals(oldCrawler)))) {
				throw ErrorUtils.statusExp(
						422,
						new Error("type", Error.E_INVALID_VALUE, i18n
								.tr("user specified crawler/type does not match the DataSource's current type: " + oldCrawler + "/"
										+ oldType)));
			}

			try {
				if (crawlerManager.getCrawlerSpec(crawler, type) == null) {
					throw ErrorUtils.statusExp(
							422,
							new Error("type", Error.E_INVALID_VALUE, i18n.tr("Unknown specification for: " + ds.getCrawlerType()
									+ "/" + ds.getType())));
				}

			} catch (Exception e) {
				throw ErrorUtils.statusExp(
						422,
						new Error("type", Error.E_EXCEPTION, i18n.tr("Error retrieving specification for: " + ds.getCrawlerType()
								+ "/" + ds.getType() + ": " + e.toString())));
			}

			DataSource newDs = null;
			List<Error> errors = new ArrayList<Error>();
			try {
				newDs = crawlerManager.createDataSource(m);
			} catch (DataSourceFactoryException e) {
				errors.addAll(e.getErrors());
			} catch (Exception e) {
				errors.add(new Error("createDataSource", Error.E_EXCEPTION, i18n.tr(e.toString())));
			}
			if (errors.size() > 0) {
				throw ErrorUtils.statusExp(422, errors);
			}

			ds.setProperties(newDs.getProperties());
			ds.setFieldMapping(newDs.getFieldMapping());

			Iterator<String> iterator = m.keySet().iterator();
			while (iterator.hasNext()) {
				String key = (String) iterator.next();
				System.out.print("key=" + key);
				System.out.println(" value=" + m.get(key));
				Object value = m.get(key);
				ds.setProperty(key, value);
			}

			boolean success = false;
			try {
				success = crawlerManager.updateDataSource(ds);
			} catch (Exception e) {
				throw ErrorUtils.statusExp(e);
			}

			if (success) {
				APIUtils.ensureSolrFieldMappingConfig(collectionName, cm, cores);

				APIUtils.toggleConnectorsSecuritySearchComponent(collectionName, cm, cores, dm);

				return buildResponse(Response.Status.NO_CONTENT);
			} else {
				throw ErrorUtils.statusExp(
						422,
						new Error("update_data_source", "unspecified.error", i18n
								.tr("operation failed - see logs for more details")));
			}
		} else {
			throw ErrorUtils.statusExp(Response.Status.NOT_FOUND, i18n.tr("DataSource not found"));
		}
	}

	@DELETE
	@Path("{dataSourceId}")
	@Produces("text/plain")
	public Response deleteCollectionActivities(@Context HttpHeaders headers, @Context UriInfo ui,
			@PathParam("dataSourceId") String dataSourceId) throws Exception {
		// return handleRequest(headers, null, ui, Request.Type.DELETE,
		// createDataSourceResource(collectionName, dataSourceId));
		// return null;
		return remove(dataSourceId);
	}

	private Response remove(String dataSourceId) throws Exception {
		// if (!isExisting()) {
		// throw ErrorUtils.statusExp(Response.Status.NOT_FOUND, "URI not found");
		// }
		DataSource ds = crawlerManager.getDataSource(new DataSourceId(dataSourceId));
		if (ds != null) {
			try {
				CrawlId jobId = new CrawlId(ds.getDataSourceId());
				Map<String, Object> stat = crawlerManager.getStatus(ds.getCrawlerType(), jobId);
				if (stat != null)
					crawlerManager.stopJob(ds.getCrawlerType(), jobId, true, 5000L);
			} catch (Exception e) {
				throw ErrorUtils.statusExp(424, new Error("datasource remove, stop job", i18n.tr("failed: " + e.getMessage())));
			}

			deleteCrawlInfo(ds);
			// FIXME: kslee
			// boolean keepDocs =
			// StringUtils.getBoolean(getQuery().getFirstValue("keep_docs",
			// "false")).booleanValue();
			boolean keepDocs = false;
			if (!keepDocs) {
				deleteIndex(ds);
			}

			crawlerManager.removeDataSource(ds.getDataSourceId(), true);
			crawlerManager.removeHistory(ds.getDataSourceId().toString());

			jmx.unregisterDataSourceMBean(ds.getDataSourceId());

			APIUtils.toggleConnectorsSecuritySearchComponent(this.collectionName, cm, cores, dm);

			AuditLogger.log("removed datasource");
		}
		// setStatus(Status.SUCCESS_NO_CONTENT);
		Response response = buildResponse(Response.Status.NO_CONTENT);
		return response;
	}

	private void deleteCrawlInfo(DataSource ds) throws Exception {
		crawlerManager.reset(ds.getCrawlerType(), ds.getCollection(), ds.getDataSourceId());
	}

	private void deleteIndex(DataSource ds) throws Exception {
		crawlerManager.deleteOutputData(ds.getDataSourceId());
	}

	@Path("{dataSourceId}/index")
	public DataSourceIndexService getDataSourceIndexHandler(@PathParam("dataSourceId") String dataSourceId) {
		return new DataSourceIndexService(super.serializer, super.bodyParser, collectionName, dataSourceId, crawlerManager,
				cm);
		// return null;
	}

	@Path("{dataSourceId}/schedule")
	public DataSourceScheduleService getDataSourceScheduleHandler(@PathParam("dataSourceId") String dataSourceId) {
		return new DataSourceScheduleService(super.serializer, super.bodyParser, collectionName, dataSourceId,
				adminScheduler, crawlerManager, cm);
	}

	@Path("{dataSourceId}/job")
	public DataSourceJobService getDataSourceJobHandler(@PathParam("dataSourceId") String dataSourceId) {
		return new DataSourceJobService(super.serializer, super.bodyParser, collectionName, dataSourceId, crawlerManager,
				cm);
	}

	@Path("{dataSourceId}/status")
	public DataSourceStatusService getDataSourceStatusHandler(@PathParam("dataSourceId") String dataSourceId) {
		return new DataSourceStatusService(super.serializer, super.bodyParser, collectionName, dataSourceId, crawlerManager);
	}

	@Path("{dataSourceId}/history")
	public DataSourceHistoryService getDataSourceHistoryHandler(@PathParam("dataSourceId") String dataSourceId) {
		return new DataSourceHistoryService(super.serializer, super.bodyParser, collectionName, dataSourceId,
				crawlerManager);
	}

	@Path("{dataSourceId}/crawldata")
	public DataSourceCrawlDataService getDataSourceCrawlDataHandler(@PathParam("dataSourceId") String dataSourceId) {
		return new DataSourceCrawlDataService(super.serializer, super.bodyParser, collectionName, dataSourceId,
				crawlerManager);
	}

}