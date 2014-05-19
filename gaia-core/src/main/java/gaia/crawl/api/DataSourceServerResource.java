package gaia.crawl.api;

import gaia.admin.collection.CollectionManager;
import gaia.api.APIUtils;
import gaia.api.AuditLogger;
import gaia.api.Error;
import gaia.api.ErrorUtils;
import gaia.crawl.ConnectorManager;
import gaia.crawl.CrawlId;
import gaia.crawl.DataSourceFactoryException;
import gaia.crawl.DataSourceManager;
import gaia.crawl.datasource.DataSource;
import gaia.jmx.JmxManager;
import gaia.utils.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.Response;

import org.apache.solr.core.CoreContainer;
import org.restlet.data.Status;
import org.restlet.resource.Delete;
import org.restlet.resource.Get;
import org.restlet.resource.Put;
import org.restlet.resource.ResourceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;

public class DataSourceServerResource extends DataSourceResourceBase implements DataSourceResource {
	private static final Logger LOG = LoggerFactory.getLogger(DataSourceServerResource.class);
	public static final String KEEP_DOCS = "keep_docs";
	private CollectionManager cm;
	private CoreContainer cores;
	private DataSourceManager dm;

	@Inject
	public DataSourceServerResource(ConnectorManager crawlerManager, JmxManager jmx, CollectionManager cm,
			CoreContainer cores, DataSourceManager dm) {
		super(crawlerManager, jmx);
		this.cm = cm;
		this.cores = cores;
		this.dm = dm;
	}

	@Delete
	public void remove() throws Exception {
		if (!isExisting()) {
			throw ErrorUtils.statusExp(Response.Status.NOT_FOUND, "URI not found");
		}
		if (ds != null) {
			try {
				CrawlId jobId = new CrawlId(ds.getDataSourceId());
				Map<String, Object> stat = crawlerManager.getStatus(ds.getCrawlerType(), jobId);
				if (stat != null)
					crawlerManager.stopJob(ds.getCrawlerType(), jobId, true, 5000L);
			} catch (Exception e) {
				throw ErrorUtils.statusExp(424, new Error("datasource remove, stop job", "failed: " + e.getMessage()));
			}

			deleteCrawlInfo();

			boolean keepDocs = StringUtils.getBoolean(getQuery().getFirstValue("keep_docs", "false")).booleanValue();
			if (!keepDocs) {
				deleteIndex();
			}

			crawlerManager.removeDataSource(ds.getDataSourceId(), true);
			crawlerManager.removeHistory(ds.getDataSourceId().toString());

			jmx.unregisterDataSourceMBean(ds.getDataSourceId());

			APIUtils.toggleConnectorsSecuritySearchComponent(collection, cm, cores, dm);

			AuditLogger.log("removed datasource");
		}
		setStatus(Status.SUCCESS_NO_CONTENT);
	}

	@Put("json")
	public void update(Map<String, Object> m) throws Exception {
		if (!isExisting()) {
			throw ErrorUtils.statusExp(Response.Status.NOT_FOUND, "URI not found");
		}
		if ((m == null) || (m.size() == 0)) {
			throw ErrorUtils.statusExp(422, "No input content found");
		}

		if (isExisting()) {
			if (m.get("id") != null) {
				throw ErrorUtils.statusExp(422, new Error("id", Error.E_FORBIDDEN_VALUE, "cannot specify id on update"));
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
				throw ErrorUtils.statusExp(422, new Error("type", Error.E_INVALID_VALUE,
						"user specified crawler/type does not match the DataSource's current type: " + oldCrawler + "/" + oldType));
			}

			try {
				if (crawlerManager.getCrawlerSpec(crawler, type) == null) {
					throw ErrorUtils.statusExp(
							422,
							new Error("type", Error.E_INVALID_VALUE, "Unknown specification for: " + ds.getCrawlerType() + "/"
									+ ds.getType()));
				}

			} catch (Exception e) {
				throw ErrorUtils.statusExp(422, new Error("type", Error.E_EXCEPTION, "Error retrieving specification for: "
						+ ds.getCrawlerType() + "/" + ds.getType() + ": " + e.toString()));
			}

			DataSource newDs = null;
			List<Error> errors = new ArrayList<Error>();
			try {
				newDs = crawlerManager.createDataSource(m);
			} catch (DataSourceFactoryException e) {
				errors.addAll(e.getErrors());
			} catch (ResourceException rse) {
				throw rse;
			} catch (Exception e) {
				errors.add(new Error("createDataSource", Error.E_EXCEPTION, e.toString()));
			}
			if (errors.size() > 0) {
				throw ErrorUtils.statusExp(422, errors);
			}

			ds.setProperties(newDs.getProperties());
			ds.setFieldMapping(newDs.getFieldMapping());

			boolean success = false;
			try {
				success = crawlerManager.updateDataSource(ds);
			} catch (ResourceException rse) {
				throw rse;
			} catch (Exception e) {
				throw ErrorUtils.statusExp(e);
			}

			if (success) {
				APIUtils.ensureSolrFieldMappingConfig(collection, cm, cores);

				APIUtils.toggleConnectorsSecuritySearchComponent(collection, cm, cores, dm);

				setStatus(Status.SUCCESS_NO_CONTENT);
				AuditLogger.log("updated datasource");
			} else {
				throw ErrorUtils.statusExp(422, new Error("update_data_source", "unspecified.error",
						"operation failed - see logs for more details"));
			}
		} else {
			throw ErrorUtils.statusExp(Response.Status.NOT_FOUND, "DataSource not found");
		}
	}

	@Get("json")
	public Map<String, Object> retrieve() {
		if (!isExisting()) {
			throw ErrorUtils.statusExp(Response.Status.NOT_FOUND, "URI not found");
		}
		return ds.toMap();
	}
}
