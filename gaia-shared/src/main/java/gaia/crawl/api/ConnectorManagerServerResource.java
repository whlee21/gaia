package gaia.crawl.api;

import gaia.api.Error;
import gaia.api.ErrorUtils;
import gaia.crawl.ConnectorManager;
import gaia.crawl.CrawlId;
import gaia.crawl.DataSourceFactoryException;
import gaia.crawl.JobStateException;
import gaia.crawl.batch.BatchStatus;
import gaia.crawl.datasource.DataSource;
import gaia.crawl.datasource.DataSourceId;
import gaia.crawl.resource.Resource;
import gaia.crawl.security.SecurityFilter;
import gaia.utils.StringUtils;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.io.IOUtils;
import org.restlet.data.Form;
import org.restlet.data.MediaType;
import org.restlet.representation.EmptyRepresentation;
import org.restlet.representation.InputRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.ResourceException;
import org.restlet.resource.ServerResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;

public class ConnectorManagerServerResource extends ServerResource implements ConnectorManagerResource {

	private static final Logger LOG = LoggerFactory.getLogger(ConnectorManagerServerResource.class);
	private ConnectorManager mgr;

	@Inject
	public ConnectorManagerServerResource(ConnectorManager mgr) {
		this.mgr = mgr;
	}

	private Object getParam(String name, boolean require) throws ResourceException {
		return getParam(name, null, require);
	}

	private Object getParam(String name, Map<String, Object> params, boolean require) throws ResourceException {
		Object param = getRequest().getAttributes().get(name);
		if (param == null) {
			Form f = getQuery();
			if (f != null) {
				param = f.getFirstValue(name);
			}
		}
		if ((param == null) && (params != null)) {
			param = params.get(name);
		}
		if ((param == null) && (require)) {
			// throw new ResourceException(422, "'" + name + "' attribute required");
		}
		return param;
	}

	public String getVersion() throws ResourceException {
		try {
			return this.mgr.getVersion();
		} catch (Exception e) {
			throw ErrorUtils.statusExp(e);
		}
	}

	public Set<String> getCrawlerTypes() throws ResourceException {
		try {
			return this.mgr.getCrawlerTypes();
		} catch (Exception e) {
			throw ErrorUtils.statusExp(e);
		}
	}

	public List<String> initCrawlersFromJar(String url) throws ResourceException {
		try {
			return this.mgr.initCrawlersFromJar(url);
		} catch (Exception e) {
			throw ErrorUtils.statusExp(e);
		}
	}

	public boolean initCrawler(Map<String, Object> m) throws ResourceException {
		String alias = (String) getParam("alias", m, true);
		String classname = (String) getParam("class", m, true);
		try {
			return this.mgr.initCrawler(alias, classname);
		} catch (Exception e) {
			throw ErrorUtils.statusExp(e);
		}
	}

	public boolean isAvailable() throws ResourceException {
		String crawler = (String) getParam("crawler", true);
		try {
			return this.mgr.isAvailable(crawler);
		} catch (Exception e) {
			throw ErrorUtils.statusExp(e);
		}
	}

	public Map<String, Object> getCrawlerSpecs() throws ResourceException {
		String crawler = (String) getParam("crawler", true);
		try {
			return this.mgr.getCrawlerSpecs(crawler);
		} catch (Exception e) {
			throw ErrorUtils.statusExp(e);
		}
	}

	public Map<String, Object> getCrawlerSpec() throws ResourceException {
		String crawler = (String) getParam("crawler", true);
		String dsType = (String) getParam("type", true);
		try {
			return this.mgr.getCrawlerSpec(crawler, dsType);
		} catch (Exception e) {
			throw ErrorUtils.statusExp(e);
		}
	}

	public Map<String, Object> createDataSource(Map<String, Object> map) throws ResourceException {
		try {
			DataSource ds = this.mgr.createDataSource(map);
			return ds.toMap();
		} catch (DataSourceFactoryException dse) {
			List<Error> errors = dse.getErrors();
			if ((errors != null) && (!errors.isEmpty())) {
				throw ErrorUtils.statusExp(422, errors);
			}
			throw ErrorUtils.statusExp(dse);
		} catch (Exception e) {
			throw ErrorUtils.statusExp(e);
		}
	}

	public Map<String, Object> validateDataSource(Map<String, Object> input) throws ResourceException {
		DataSource ds = DataSource.fromMap(input);
		boolean remove = StringUtils.getBoolean(getParam("remove", input, false), true).booleanValue();
		boolean verifyAccess = StringUtils.getBoolean(getParam("verify_access", input, false), true).booleanValue();
		try {
			return this.mgr.validateDataSource(ds, remove, verifyAccess).toMap();
		} catch (DataSourceFactoryException dse) {
			List<Error> errors = dse.getErrors();
			if ((errors != null) && (!errors.isEmpty())) {
				throw ErrorUtils.statusExp(422, errors);
			}
			throw ErrorUtils.statusExp(dse);
		} catch (Exception e) {
			throw ErrorUtils.statusExp(e);
		}
	}

	public void setClosing(Map<String, Object> map) throws ResourceException {
		String collection = (String) getParam("collection", map, false);
		String crawler = (String) getParam("crawler", map, false);
		boolean value = StringUtils.getBoolean(getParam("closing", map, true), true).booleanValue();
		try {
			this.mgr.setClosing(crawler, collection, value);
		} catch (Exception e) {
			throw ErrorUtils.statusExp(e);
		}
	}

	public Map<String, Object> crawlerReset(Map<String, Object> map) throws ResourceException {
		String collection = (String) getParam("collection", map, false);
		String crawler = (String) getParam("crawler", map, true);
		String id = (String) getParam("id", map, true);
		try {
			return this.mgr.reset(crawler, collection, new DataSourceId(id));
		} catch (Exception e) {
			throw ErrorUtils.statusExp(e);
		}
	}

	public Map<String, Object> crawlerResetAll(Map<String, Object> map) throws ResourceException {
		String collection = (String) getParam("collection", map, false);
		String crawler = (String) getParam("crawler", map, false);
		try {
			return this.mgr.resetAll(crawler, collection);
		} catch (Exception e) {
			throw ErrorUtils.statusExp(e);
		}
	}

	public List<String> close(Map<String, Object> map) throws ResourceException {
		String collection = (String) getParam("collection", map, false);
		String crawler = (String) getParam("crawler", map, false);
		boolean value = StringUtils.getBoolean(getParam("dryRun", map, false), true).booleanValue();
		List<CrawlId> lst = null;
		try {
			lst = this.mgr.close(crawler, collection, value);
		} catch (Exception e) {
			throw ErrorUtils.statusExp(e);
		}
		List<String> res = new ArrayList<String>();
		for (CrawlId cid : lst) {
			res.add(cid.toString());
		}
		return res;
	}

	public Map<String, Object> crawl(Map<String, Object> map) throws ResourceException {
		String id = (String) getParam("id", map, true);
		try {
			return this.mgr.crawl(new DataSourceId(id));
		} catch (JobStateException jse) {
			throw ErrorUtils.statusExp(422, new Error(id, Error.E_INVALID_OPERATION, jse.toString()));
		} catch (Exception e) {
			throw ErrorUtils.statusExp(e);
		}
	}

	public List<Map<String, Object>> listJobs() throws ResourceException {
		String crawler = (String) getParam("crawler", null, false);
		boolean batch = StringUtils.getBoolean(getParam("batch", null, false), true).booleanValue();
		try {
			return this.mgr.listJobs(crawler, batch);
		} catch (Exception e) {
			throw ErrorUtils.statusExp(e);
		}
	}

	public Map<String, Object> getJobStatus() throws ResourceException {
		String crawler = (String) getParam("crawler", false);
		String id = (String) getParam("id", true);
		CrawlId cid = new CrawlId(id);
		try {
			return this.mgr.getStatus(crawler, cid);
		} catch (Exception e) {
			throw ErrorUtils.statusExp(e);
		}
	}

	public boolean stopJob(Map<String, Object> map) throws ResourceException {
		String crawler = (String) getParam("crawler", map, true);
		String id = (String) getParam("id", true);
		boolean abort = StringUtils.getBoolean(getParam("abort", map, false), true).booleanValue();
		Object o = getParam("waitTime", map, false);
		long waitTime = -1L;
		if (o != null)
			try {
				waitTime = Long.parseLong(o.toString());
			} catch (Exception e) {
				LOG.warn("Invalid waitTime, using -1: " + e.toString());
			}
		try {
			return this.mgr.stopJob(crawler, new CrawlId(id), abort, waitTime);
		} catch (JobStateException jse) {
			throw ErrorUtils.statusExp(422, new Error(id, Error.E_INVALID_OPERATION, jse.toString()));
		} catch (Exception e) {
			throw ErrorUtils.statusExp(e);
		}
	}

	public List<Error> finishAllJobs(Map<String, Object> map) throws ResourceException {
		String collection = (String) getParam("collection", map, false);
		String crawler = (String) getParam("crawler", map, false);
		boolean abort = StringUtils.getBoolean(getParam("abort", map, false), true).booleanValue();
		Object o = getParam("waitTime", map, false);
		long waitTime = -1L;
		if (o != null)
			try {
				waitTime = Long.parseLong(o.toString());
			} catch (Exception e) {
				LOG.warn("Invalid waitTime, using -1: " + e.toString());
			}
		try {
			return this.mgr.finishAllJobs(crawler, collection, abort, waitTime);
		} catch (Exception e) {
			throw ErrorUtils.statusExp(e);
		}
	}

	public boolean addDataSource(Map<String, Object> map) throws ResourceException {
		DataSource ds = DataSource.fromMap(map);
		try {
			return this.mgr.addDataSource(ds);
		} catch (DataSourceFactoryException dse) {
			List<Error> errors = dse.getErrors();
			if ((errors != null) && (!errors.isEmpty())) {
				throw ErrorUtils.statusExp(422, errors);
			}
			throw ErrorUtils.statusExp(dse);
		} catch (Exception e) {
			throw ErrorUtils.statusExp(e);
		}
	}

	public boolean exists() throws ResourceException {
		String id = (String) getParam("id", true);
		DataSource ds = null;
		try {
			ds = this.mgr.getDataSource(new DataSourceId(id));
		} catch (Exception e) {
			throw ErrorUtils.statusExp(e);
		}
		if (ds == null) {
			return false;
		}
		return true;
	}

	public Map<String, Object> getDataSource() throws ResourceException {
		String id = (String) getParam("id", true);
		DataSource ds = null;
		try {
			ds = this.mgr.getDataSource(new DataSourceId(id));
		} catch (Exception e) {
			throw ErrorUtils.statusExp(e);
		}
		if (ds == null) {
			// setStatus(Response.Status.NOT_FOUND);
			return null;
		}
		return ds.toMap();
	}

	public List<Map<String, Object>> getDataSources() throws ResourceException {
		String collection = (String) getParam("collection", false);
		List<DataSource> lst = null;
		try {
			lst = this.mgr.getDataSources(collection);
		} catch (Exception e) {
			throw ErrorUtils.statusExp(e);
		}
		List<Map<String, Object>> res = new ArrayList<Map<String, Object>>();
		for (DataSource ds : lst) {
			res.add(ds.toMap());
		}
		return res;
	}

	public List<String> listDataSources() throws ResourceException {
		String collection = (String) getParam("collection", false);
		List<DataSourceId> lst = null;
		try {
			lst = this.mgr.listDataSources(collection);
		} catch (Exception e) {
			throw ErrorUtils.statusExp(e);
		}
		List<String> res = new ArrayList<String>();
		for (DataSourceId dsId : lst) {
			res.add(dsId.toString());
		}
		return res;
	}

	public boolean removeDataSource() throws ResourceException {
		String id = (String) getParam("id", true);
		boolean force = StringUtils.getBoolean(getParam("force", false), true).booleanValue();
		try {
			return this.mgr.removeDataSource(new DataSourceId(id), force);
		} catch (Exception e) {
			LOG.error("removeDataSource", e);
			throw ErrorUtils.statusExp(e);
		}
	}

	public List<String> removeDataSources() throws ResourceException {
		String collection = (String) getParam("collection", true);
		boolean force = StringUtils.getBoolean(getParam("force", false), true).booleanValue();
		List<DataSourceId> lst = null;
		try {
			lst = this.mgr.removeDataSources(collection, force);
		} catch (Exception e) {
			throw ErrorUtils.statusExp(e);
		}
		List<String> res = new ArrayList<String>();
		for (DataSourceId dsId : lst) {
			res.add(dsId.toString());
		}
		return res;
	}

	public boolean updateDataSource(Map<String, Object> map) throws ResourceException {
		DataSource ds = DataSource.fromMap(map);
		try {
			return this.mgr.updateDataSource(ds);
		} catch (DataSourceFactoryException dse) {
			List<Error> errors = dse.getErrors();
			if ((errors != null) && (!errors.isEmpty())) {
				throw ErrorUtils.statusExp(422, errors);
			}
			throw ErrorUtils.statusExp(dse);
		} catch (Exception e) {
			throw ErrorUtils.statusExp(e);
		}
	}

	public void reset() throws ResourceException {
		boolean init = StringUtils.getBoolean(getParam("init", false), true).booleanValue();
		try {
			this.mgr.reset(init);
		} catch (Exception e) {
			throw ErrorUtils.statusExp(e);
		}
	}

	public void shutdown() throws ResourceException {
		LOG.info("shutdown() is ignored");
	}

	public List<Map<String, Object>> listBatches() throws ResourceException {
		String crawler = (String) getParam("crawler", false);
		String collection = (String) getParam("collection", false);
		String id = (String) getParam("id", false);
		List<BatchStatus> lst = null;
		try {
			lst = this.mgr.listBatches(crawler, collection, id);
		} catch (Exception e) {
			throw ErrorUtils.statusExp(e);
		}
		List<Map<String, Object>> res = new ArrayList<Map<String, Object>>();
		for (BatchStatus bs : lst) {
			res.add(bs.toMap());
		}
		return res;
	}

	public Map<String, Object> getBatchStatus() throws ResourceException {
		String crawler = (String) getParam("crawler", true);
		String collection = (String) getParam("collection", false);
		String id = (String) getParam("id", true);
		BatchStatus bs = null;
		try {
			bs = this.mgr.getBatchStatus(crawler, collection, id);
		} catch (Exception e) {
			throw ErrorUtils.statusExp(e);
		}
		if (bs == null) {
			// setStatus(Response.Status.NOT_FOUND);
			return null;
		}
		return bs.toMap();
	}

	public boolean deleteBatches() throws ResourceException {
		String crawler = (String) getParam("crawler", false);
		String collection = (String) getParam("collection", false);
		String id = (String) getParam("id", false);
		try {
			return this.mgr.deleteBatches(crawler, collection, id);
		} catch (Exception e) {
			throw ErrorUtils.statusExp(e);
		}
	}

	public String startBatchJob(Map<String, Object> map) throws ResourceException {
		String crawler = (String) getParam("crawler", map, true);
		String collection = (String) getParam("collection", map, false);
		String id = (String) getParam("id", map, true);
		boolean parse = StringUtils.getBoolean(getParam("parse", map, false), true).booleanValue();
		boolean index = StringUtils.getBoolean(getParam("index", map, false), true).booleanValue();

		Map<String, Object> dsMap = (Map) getParam("template", map, false);
		DataSource template = null;
		if (dsMap != null) {
			template = DataSource.fromMap(dsMap);
			try {
				template = this.mgr.validateDataSource(template, true, false);
			} catch (DataSourceFactoryException dse) {
				List<Error> errors = dse.getErrors();
				if ((errors != null) && (!errors.isEmpty())) {
					throw ErrorUtils.statusExp(422, errors);
				}
				throw ErrorUtils.statusExp(dse);
			} catch (Exception e) {
				throw ErrorUtils.statusExp(e);
			}
		}
		CrawlId cid = null;
		try {
			cid = this.mgr.startBatchJob(crawler, collection, id, template, parse, index);
		} catch (Exception e) {
			throw ErrorUtils.statusExp(e);
		}
		return cid.toString();
	}

	public List<Map<String, Object>> getBatchJobStatuses() throws ResourceException {
		String crawler = (String) getParam("crawler", true);
		String collection = (String) getParam("collection", false);
		String id = (String) getParam("id", false);
		try {
			return this.mgr.getBatchJobStatuses(crawler, collection, id);
		} catch (Exception e) {
			throw ErrorUtils.statusExp(e);
		}
	}

	public List<Map<String, Object>> getHistory() throws ResourceException {
		String key = (String) getParam("key", true);
		try {
			return this.mgr.getHistory(key);
		} catch (Exception e) {
			throw ErrorUtils.statusExp(e);
		}
	}

	public Map<String, Object> getCumulativeHistory() throws ResourceException {
		String key = (String) getParam("key", true);
		try {
			return this.mgr.getCumulativeHistory(key);
		} catch (Exception e) {
			throw ErrorUtils.statusExp(e);
		}
	}

	public boolean removeHistory() throws ResourceException {
		String key = (String) getParam("key", true);
		try {
			this.mgr.removeHistory(key);
		} catch (Exception e) {
			throw ErrorUtils.statusExp(e);
		}

		return true;
	}

	public List<Map<String, Object>> listResources() throws ResourceException {
		String crawler = (String) getParam("crawler", true);
		String collection = (String) getParam("collection", false);
		String dsId = (String) getParam("id", false);
		DataSourceId ds = null;
		if (dsId != null)
			ds = new DataSourceId(dsId);
		List<Resource> resources = null;
		try {
			resources = this.mgr.listResources(crawler, collection, ds);
		} catch (Exception e) {
			throw ErrorUtils.statusExp(e);
		}
		List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
		for (Resource resource : resources) {
			result.add(resource.toMap());
		}
		return result;
	}

	public Representation uploadResource(Representation entity) throws ResourceException {
		if ((entity == null) || (!MediaType.MULTIPART_FORM_DATA.equals(entity.getMediaType(), true))) {
			// throw new ResourceException(422, "No multipart data in request");
		}
		DiskFileItemFactory factory = new DiskFileItemFactory();
		factory.setSizeThreshold(1000240);
		// RestletFileUpload upload = new RestletFileUpload(factory);

		String crawler = null;
		String collection = null;
		String dsId = null;
		FileItem resourceFileItem = null;
		// List<FileItem> items;
		List<FileItem> items = new ArrayList<FileItem>();
		try {
			// items = upload.parseRequest(getRequest());
		} catch (Exception e) {
			throw ErrorUtils.statusExp(e);
		}

		Map<String, String> properties = new HashMap<String, String>();
		try {
			for (FileItem fi : items) {
				String name = fi.getName();
				if (name == null) {
					if ("crawler".equals(fi.getFieldName()))
						crawler = new String(fi.get(), "UTF-8");
					else if ("collection".equals(fi.getFieldName()))
						collection = new String(fi.get(), "UTF-8");
					else if ("id".equals(fi.getFieldName()))
						dsId = new String(fi.get(), "UTF-8");
					else
						properties.put(fi.getFieldName(), new String(fi.get(), "UTF-8"));
				} else
					resourceFileItem = fi;
			}
		} catch (Exception e) {
			throw ErrorUtils.statusExp(e);
		}

		// if (crawler == null) {
		// throw new ResourceException(422, "'crawler' attribute required");
		// }
		//
		// if (resourceFileItem == null) {
		// throw new ResourceException(422, "No file stream in POST data");
		// }

		String filename = resourceFileItem.getName();
		InputStream is = null;
		try {
			is = resourceFileItem.getInputStream();
			DataSourceId ds = null;
			if (dsId != null)
				ds = new DataSourceId(dsId);
			this.mgr.uploadResource(crawler, new Resource(filename, properties), is, collection, ds);
		} catch (Exception e) {
			throw ErrorUtils.statusExp(e);
		} finally {
			if (is != null) {
				IOUtils.closeQuietly(is);
			}
		}

		return new EmptyRepresentation();
	}

	public boolean deleteResource() throws ResourceException {
		String crawler = (String) getParam("crawler", true);
		String collection = (String) getParam("collection", false);
		String dsId = (String) getParam("id", false);
		String name = (String) getParam("name", true);
		DataSourceId ds = null;
		if (dsId != null)
			ds = new DataSourceId(dsId);
		try {
			this.mgr.deleteResource(crawler, name, collection, ds);
		} catch (Exception e) {
			throw ErrorUtils.statusExp(e);
		}
		return true;
	}

	public Representation openResource(Representation entity) throws ResourceException {
		String crawler = (String) getParam("crawler", true);
		String collection = (String) getParam("collection", false);
		String dsId = (String) getParam("id", false);
		DataSourceId ds = null;
		if (dsId != null) {
			ds = new DataSourceId(dsId);
		}
		String name = (String) getParam("name", true);
		InputStream is = null;
		try {
			is = this.mgr.openResource(crawler, name, collection, ds);
		} catch (Exception e) {
			throw ErrorUtils.statusExp(e);
		}
		// setStatus(Response.Status.OK);
		return new InputRepresentation(is, MediaType.APPLICATION_ALL);
	}

	public Map<String, Object> buildSecurityFilter() throws ResourceException {
		String user = (String) getParam("user", true);
		String id = (String) getParam("id", true);
		DataSourceId dsId = new DataSourceId(id);
		try {
			SecurityFilter filter = this.mgr.buildSecurityFilter(dsId, user);
			if (filter != null) {
				return filter.toMap();
			}
			return null;
		} catch (Exception e) {
			throw ErrorUtils.statusExp(e);
		}
	}

	public void deleteOutputData() throws ResourceException {
		String id = (String) getParam("id", true);
		DataSourceId dsId = new DataSourceId(id);
		try {
			this.mgr.deleteOutputData(dsId);
		} catch (Exception e) {
			throw ErrorUtils.statusExp(e);
		}
	}
}
