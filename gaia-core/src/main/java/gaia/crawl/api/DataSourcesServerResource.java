package gaia.crawl.api;

import gaia.Constants;
import gaia.admin.collection.CollectionManager;
import gaia.api.APIUtils;
import gaia.api.AuditLogger;
import gaia.api.Error;
import gaia.api.ErrorUtils;
import gaia.crawl.ConnectorManager;
import gaia.crawl.DataSourceFactoryException;
import gaia.crawl.DataSourceManager;
import gaia.crawl.datasource.DataSource;
import gaia.jmx.JmxManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.solr.core.CoreContainer;
import org.restlet.data.Status;
import org.restlet.resource.Get;
import org.restlet.resource.Post;
import org.restlet.resource.ResourceException;
import org.restlet.resource.ServerResource;

import com.google.inject.Inject;

public class DataSourcesServerResource extends ServerResource implements DataSourcesResource {
	private String collection;
	private ConnectorManager crawlerManager;
	private JmxManager jmx;
	private CollectionManager cm;
	private CoreContainer cores;
	private DataSourceManager dm;

	@Inject
	public DataSourcesServerResource(ConnectorManager crawlerManager, JmxManager jmx, CollectionManager cm,
			CoreContainer cores, DataSourceManager dm) {
		this.crawlerManager = crawlerManager;
		this.jmx = jmx;
		this.cm = cm;
		this.cores = cores;
		this.dm = dm;
	}

	public void doInit() throws ResourceException {
		collection = ((String) getRequestAttributes().get("coll_name"));
	}

	// @Post("json")
	// public Map<String, Object> add(Map<String, Object> m) throws Exception {
	@Post("json")
	public Map<String, Object> add(Map<String, Object> m) throws Exception {
		if (m.size() == 0) {
			throw ErrorUtils.statusExp(422, "No input content found");
		}

		String collection = (String) getRequestAttributes().get("coll_name");

		List<Error> errors = new ArrayList<Error>();
		if (Constants.LOGS_COLLECTION.equals(collection)) {
			errors.add(new Error("collection", Error.E_FORBIDDEN_VALUE,
					"users can't create other datasources in this collection"));

			throw ErrorUtils.statusExp(422, errors);
		}

		String type = (String) m.get("type");
		String crawlerClass = (String) m.get("crawler");
		if (m.get("collection") == null) {
			m.put("collection", collection);
		} else if (!collection.equals(m.get("collection"))) {
			errors.add(new Error("collection", Error.E_INVALID_VALUE,
					"collection specified in datasource doesn't match this collection"));

			throw ErrorUtils.statusExp(422, errors);
		}

		DataSource ds = null;

		if (crawlerClass == null) {
			errors.add(new Error("crawler", Error.E_MISSING_VALUE, "Missing crawler type"));
		}

		if (type == null) {
			errors.add(new Error("type", Error.E_NULL_VALUE, "Missing data source type"));
		}

		if (m.get("id") != null) {
			errors.add(new Error("id", Error.E_FORBIDDEN_VALUE, "cannot set id on creation"));
		}

		boolean success = false;
		if (errors.size() == 0) {
			if (!crawlerManager.isAvailable(crawlerClass)) {
				errors.add(new Error("crawler", Error.E_INVALID_VALUE, "unknown crawler type " + crawlerClass));

				throw ErrorUtils.statusExp(422, errors);
			}

			Map<String, Object> spec = crawlerManager.getCrawlerSpec(crawlerClass, type);
			if (spec == null) {
				errors.add(new Error("type", Error.E_INVALID_VALUE, "unknown data source type " + type));

				throw ErrorUtils.statusExp(422, errors);
			}
			try {
				ds = crawlerManager.createDataSource(m);
			} catch (DataSourceFactoryException e) {
				errors.addAll(e.getErrors());
			} catch (ResourceException rse) {
				throw rse;
			}
			if (errors.size() == 0) {
				success = crawlerManager.addDataSource(ds);
			}
		}

		if (errors.size() > 0) {
			throw ErrorUtils.statusExp(422, errors);
		}

		if (ds == null) {
			errors.add(new Error("type", Error.E_EXCEPTION, "unable to create data source :" + type));

			throw ErrorUtils.statusExp(422, errors);
		}

		if (success) {
			jmx.registerDataSourceMBean(ds.getDataSourceId());

			APIUtils.ensureSolrFieldMappingConfig(collection, cm, cores);

			APIUtils.toggleConnectorsSecuritySearchComponent(collection, cm, cores, dm);

			getResponse().setLocationRef("datasources/" + ds.getDataSourceId());
			setStatus(Status.SUCCESS_CREATED);
			AuditLogger.log("added Datasource");

			return ds.toMap();
		}
		throw ErrorUtils.statusExp(422, new Error("add_data_source", "unspecified.error",
				"operation failed - see logs for more details"));
	}

	@Get("json")
	public List<Map<String, Object>> retrieve() throws IOException {
		List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
		try {
			for (DataSource ds : crawlerManager.getDataSources(collection))
				result.add(ds.toMap());
		} catch (Exception e) {
			throw ErrorUtils.statusExp(422, new Error("getDataSources", Error.E_EXCEPTION, e.toString()));
		}
		return result;
	}
}
