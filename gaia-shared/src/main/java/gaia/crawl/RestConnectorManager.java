package gaia.crawl;

import gaia.api.Error;
import gaia.crawl.batch.BatchStatus;
import gaia.crawl.datasource.DataSource;
import gaia.crawl.datasource.DataSourceId;
import gaia.crawl.resource.Resource;
import gaia.crawl.security.SecurityFilter;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response.Status;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.WebResource.Builder;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.api.json.JSONConfiguration;
import com.sun.jersey.core.util.MultivaluedMapImpl;

@Singleton
public class RestConnectorManager implements ConnectorManager {

	private static final Logger LOG = LoggerFactory.getLogger(RestConnectorManager.class);

	static long defaultHeartbeatInterval = 10000L;

	String DEFAULT_URL = "http://localhost:8765/connector/v1/mgr";
	String url;
	DataSourceManager dm;
	String version;
	WebResource res;
	Client client;
	CMCheckerThread checker = null;
	ConnectorManager noopCm = null;
	volatile boolean noCM = false;
	volatile boolean initialLoad = false;
	long heartbeatInterval = defaultHeartbeatInterval;
	ConnectorManager.ConnectorStatus status = new ConnectorManager.ConnectorStatus();

	protected RestConnectorManager() {
		status.type = "REST";
		String u = System.getProperty("connector.url");
		if (u != null) {
			if (u.endsWith("/mgr")) {
				url = u;
			} else if (!u.endsWith("/"))
				url = u;
			else {
				url = (u + "connectors/v1/mgr");
			}

			// FIXME: HttpsUrlConnection by whlee21
			ClientConfig clientConfig = new DefaultClientConfig();
			clientConfig.getFeatures().put(JSONConfiguration.FEATURE_POJO_MAPPING, Boolean.TRUE);
			client = Client.create(clientConfig);
			noCM = false;

			checker = new CMCheckerThread();
			checker.start();
		} else {
			status.addMessage("remote ConnectorManager is not configured");
			noCM = true;
		}
	}

	@Inject
	public RestConnectorManager(DataSourceManager dm) {
		this();
		this.dm = dm;

		if (noCM) {
			LOG.info("No remote ConnectorManager available, skipping data source loading...");
		} else {
			String msg = "Initiating data source loading and back-compat validation...";
			LOG.info(msg);
			status.addMessage(msg);
			initialLoad = true;
			try {
				int cnt = dm.initialLoad(this);
				msg = cnt + " data sources loaded and validated.";
				LOG.info(msg);
				status.addMessage(msg);
			} catch (Exception e) {
				msg = "Initial load of data sources failed: " + e.toString();
				LOG.warn(msg);
				status.addMessage(msg);
			} finally {
				initialLoad = false;
			}
		}
		noopCm = new NoopConnectorManager(dm);
		status.clients.put("noop", noopCm.getConnectorStatus().toMap());
	}

	private synchronized Builder builder(String method, Object[] params) throws Exception {
		WebResource res = client.resource(url + "?m=" + method);
		MultivaluedMap<String, String> multiParams = new MultivaluedMapImpl();
		for (int i = 0; i < params.length; i += 2) {
			multiParams.putSingle(params[i].toString(), params[(i + 1)] != null ? params[(i + 1)].toString() : null);
		}
		return res.queryParams(multiParams).accept(MediaType.APPLICATION_JSON_TYPE).type(MediaType.APPLICATION_JSON_TYPE);
	}

	private Exception convertException(WebResource.Builder builder, Throwable e) {
		if ((checker != null) && (e.getMessage().startsWith("Communication Error"))) {
			checker.failures += 1;
			checker.fallback(e);
		}
		ClientResponse resp = builder.get(ClientResponse.class);
		if ((resp == null) || (resp.getEntity(String.class) == null)) {
			return new WebApplicationException(e);
		}
		int status = resp.getStatus();
		String text = resp.getEntity(String.class);
		JSONArray errors = null;
		if (text.trim().length() > 0) {
			try {
				JSONObject ob = new JSONObject(text);
				errors = ob.getJSONArray("errors");
			} catch (JSONException e1) {
				try {
					errors = new JSONArray(text);
				} catch (JSONException e2) {
				}
			}
			if (errors != null) {
				text = errors.toString();
			}
		}
		return new WebApplicationException(status);
	}

	private Map<String, Object> getMap(String method, Object[] params) throws Exception {
		WebResource.Builder builder = builder(method, params);
		if (LOG.isDebugEnabled())
			LOG.debug("getMap: " + builder.toString());
		Map<String, Object> m;
		try {
			m = builder.get(Map.class);
		} catch (Exception e) {
			e = convertException(builder, e);
			throw e;
		}
		return m;
	}

	private <T> List<T> getList(String method, Class<? extends T> clz, Object[] params) throws Exception {
		WebResource.Builder builder = builder(method, params);
		if (LOG.isDebugEnabled())
			LOG.debug("getList: " + builder.toString());
		List<T> lst;
		try {
			lst = builder.get(List.class);
		} catch (Exception e) {
			e = convertException(builder, e);
			throw e;
		}
		return lst;
	}

	private <T> List<T> deleteList(String method, Class<? extends T> clz, Object[] params) throws Exception {
		WebResource.Builder builder = builder(method, params);
		if (LOG.isDebugEnabled())
			LOG.debug("deleteList: " + builder.toString());
		List<T> lst;
		try {
			lst = builder.delete(List.class);
		} catch (Exception e) {
			e = convertException(builder, e);
			throw e;
		}
		return lst;
	}

	private boolean getBoolean(String method, Object[] params) throws Exception {
		WebResource.Builder builder = builder(method, params);
		if (LOG.isDebugEnabled())
			LOG.debug("getBoolean: " + builder.toString());
		Boolean b;
		try {
			b = (Boolean) builder.get(Boolean.class);
		} catch (Exception e) {
			e = convertException(builder, e);
			throw e;
		}
		return b != null ? b.booleanValue() : false;
	}

	private boolean deleteBoolean(String method, Object[] params) throws Exception {
		WebResource.Builder builder = builder(method, params);
		if (LOG.isDebugEnabled())
			LOG.debug("deleteBoolean: " + builder.toString());
		Boolean b;
		try {
			b = builder.delete(Boolean.class);
		} catch (Exception e) {
			e = convertException(builder, e);
			throw e;
		}
		return b != null ? b.booleanValue() : false;
	}

	private boolean putBoolean(String method, Map<String, Object> args, Object[] params) throws Exception {
		if (args == null) {
			args = Collections.emptyMap();
		}
		WebResource.Builder builder = builder(method, params);
		if (LOG.isDebugEnabled())
			LOG.debug("putBoolean: " + builder.toString());
		Boolean b;
		try {
			b = builder.put(Boolean.class, args);
		} catch (Exception e) {
			e = convertException(builder, e);
			throw e;
		}
		return b != null ? b.booleanValue() : false;
	}

	private void deleteVoid(String method, Object[] params) throws Exception {
		WebResource.Builder builder = builder(method, params);
		if (LOG.isDebugEnabled())
			LOG.debug("deleteVoid: " + builder.toString());
		try {
			builder.delete();
		} catch (Exception e) {
			e = convertException(builder, e);
			throw e;
		}
	}

	private boolean postBoolean(String method, Map<String, Object> args, Object[] params) throws Exception {
		if (args == null) {
			args = Collections.emptyMap();
		}
		WebResource.Builder builder = builder(method, params);
		if (LOG.isDebugEnabled())
			LOG.debug("postBoolean: " + builder.toString());
		Boolean b;
		try {
			b = builder.post(Boolean.class, args);
		} catch (Exception e) {
			e = convertException(builder, e);
			throw e;
		}
		return b != null ? b.booleanValue() : false;
	}

	private Object postMap(String method, Class<?> clz, Map<String, Object> args, Object[] params) throws Exception {
		if (args == null) {
			args = Collections.emptyMap();
		}
		WebResource.Builder builder = builder(method, params);
		if (LOG.isDebugEnabled())
			LOG.debug("postMap: " + builder.toString());
		Object o;
		try {
			o = builder.post(clz, args);
		} catch (Exception e) {
			e = convertException(builder, e);
			throw e;
		}
		return o;
	}

	private Object putMap(String method, Class<?> clz, Map<String, Object> args, Object[] params) throws Exception {
		if (args == null) {
			args = Collections.emptyMap();
		}
		WebResource.Builder builder = builder(method, params);
		if (LOG.isDebugEnabled())
			LOG.debug("putMap: " + builder.toString());
		Object o;
		try {
			o = builder.put(clz, args);
		} catch (Exception e) {
			e = convertException(builder, e);
			throw e;
		}
		return o;
	}

	public String getVersion() throws Exception {
		if (noCM) {
			return noopCm.getVersion();
		}
		return getVersionInternal();
	}

	private String getVersionInternal() throws Exception {
		WebResource.Builder builder = builder("getVersion", new Object[0]);
		String v = null;
		try {
			v = (String) builder.get(String.class);
		} catch (Throwable t) {
			Exception e = convertException(builder, t);
			throw e;
		}
		return v;
	}

	public ConnectorManager.ConnectorStatus getConnectorStatus() {
		return status;
	}

	public Set<String> getCrawlerTypes() throws Exception {
		if (noCM)
			return noopCm.getCrawlerTypes();
		try {
			List<String> lst = getList("getCrawlerTypes", String.class, new Object[0]);
			return new HashSet<String>(lst);
		} catch (Exception e) {
		}
		return Collections.emptySet();
	}

	public List<String> initCrawlersFromJar(String url) throws Exception {
		if (noCM) {
			return noopCm.initCrawlersFromJar(url);
		}
		return getList("initCrawlersFromJar", String.class, new Object[] { "url", url });
	}

	public boolean initCrawler(String alias, String className) throws Exception {
		if (noCM) {
			return noopCm.initCrawler(alias, className);
		}
		return postBoolean("initCrawler", null, new Object[] { "alias", alias, "class", className });
	}

	public boolean isAvailable(String crawlerType) throws Exception {
		if (noCM)
			return noopCm.isAvailable(crawlerType);
		try {
			return getBoolean("isAvailable", new Object[] { "crawler", crawlerType });
		} catch (Exception e) {
		}
		return false;
	}

	public Map<String, Object> getCrawlerSpecs(String crawlerType) throws Exception {
		if (noCM) {
			return noopCm.getCrawlerSpecs(crawlerType);
		}
		return getMap("getCrawlerSpecs", new Object[] { "crawler", crawlerType });
	}

	public Map<String, Object> getCrawlerSpec(String crawlerType, String dsType) throws Exception {
		if (noCM) {
			return noopCm.getCrawlerSpec(crawlerType, dsType);
		}
		return getMap("getCrawlerSpec", new Object[] { "crawler", crawlerType, "type", dsType });
	}

	public DataSource createDataSource(Map<String, Object> map) throws Exception {
		if (noCM) {
			return noopCm.createDataSource(map);
		}
		Map<String, Object> res = (Map) postMap("createDataSource", Map.class, map, new Object[0]);
		DataSource ds = DataSource.fromMap(res);
		return ds;
	}

	public DataSource validateDataSource(DataSource input, boolean removeUnknown, boolean verifyAccess) throws Exception {
		if (noCM) {
			return noopCm.validateDataSource(input, removeUnknown, verifyAccess);
		}
		Map<String, Object> in = input.toMap();
		Map<String, Object> res = (Map) putMap("validateDataSource", Map.class, in,
				new Object[] { "remove", Boolean.valueOf(removeUnknown), "verify_access", Boolean.valueOf(verifyAccess) });

		DataSource ds = DataSource.fromMap(res);
		return ds;
	}

	public void setClosing(String crawlerController, String collection, boolean value) throws Exception {
		if (noCM) {
			noopCm.setClosing(crawlerController, collection, value);
			return;
		}
		putMap("setClosing", String.class, null, new Object[] { "crawler", crawlerController, "collection", collection,
				"closing", Boolean.valueOf(value) });
	}

	public Map<String, Object> reset(String crawlerController, String collection, DataSourceId dsId) throws Exception {
		if (noCM) {
			return noopCm.reset(crawlerController, collection, dsId);
		}
		return (Map) putMap("crawlerReset", Map.class, null, new Object[] { "crawler", crawlerController, "collection",
				collection, "id", dsId != null ? dsId.toString() : null });
	}

	public Map<String, Object> resetAll(String crawlerController, String collection) throws Exception {
		if (noCM) {
			return noopCm.resetAll(crawlerController, collection);
		}
		return (Map) putMap("crawlerResetAll", Map.class, null, new Object[] { "crawler", crawlerController, "collection",
				collection });
	}

	public List<CrawlId> close(String crawlerController, String collection, boolean dryRun) throws Exception {
		if (noCM) {
			return noopCm.close(crawlerController, collection, dryRun);
		}
		List<String> lst = (List) putMap("close", List.class, null, new Object[] { "crawler", crawlerController,
				"collection", collection, "dryRun", Boolean.valueOf(dryRun) });

		List<CrawlId> res = new ArrayList<CrawlId>();
		for (String s : lst) {
			res.add(new CrawlId(s));
		}
		return res;
	}

	public Map<String, Object> crawl(DataSourceId dsId) throws Exception {
		if (noCM) {
			return noopCm.crawl(dsId);
		}
		return (Map) putMap("crawl", Map.class, null, new Object[] { "id", dsId.toString() });
	}

	public Map<String, Object> getStatus(String crawlerController, CrawlId jobId) throws Exception {
		if (noCM) {
			return noopCm.getStatus(crawlerController, jobId);
		}
		return getMap("getJobStatus", new Object[] { "crawler", crawlerController, "id", jobId.toString() });
	}

	public List<Map<String, Object>> listJobs(String crawler, boolean batch) throws Exception {
		if (noCM) {
			return noopCm.listJobs(crawler, batch);
		}
		List<Map> lst = getList("listJobs", Map.class, new Object[] { "crawler", crawler, "batch", Boolean.valueOf(batch) });

		List<Map<String, Object>> res = new ArrayList<Map<String, Object>>();
		for (Map<String, Object> m : lst) {
			res.add(m);
		}
		return res;
	}

	public boolean stopJob(String crawler, CrawlId jobId, boolean abort, long waitTime) throws Exception {
		if (noCM) {
			return noopCm.stopJob(crawler, jobId, abort, waitTime);
		}
		return putBoolean(
				"stopJob",
				null,
				new Object[] { "crawler", crawler, "id", jobId.toString(), "abort", Boolean.valueOf(abort), "waitTime",
						Long.valueOf(waitTime) });
	}

	public List<Error> finishAllJobs(String crawlerController, String collection, boolean abort, long wait)
			throws Exception {
		if (noCM) {
			return noopCm.finishAllJobs(crawlerController, collection, abort, wait);
		}
		List<Object> lst = (List) putMap("finishAllJobs", List.class, null, new Object[] { "crawler", crawlerController,
				"collection", collection, "abort", Boolean.valueOf(abort), "waitTime", Long.valueOf(wait) });

		List<Error> res = new ArrayList<Error>();
		for (Iterator<Object> iter = lst.iterator(); iter.hasNext();) {
			Object o = iter.next();
			res.add((Error) o);
		}
		return res;
	}

	public boolean addDataSource(DataSource ds) throws Exception {
		if (noCM) {
			return noopCm.addDataSource(ds);
		}
		boolean res = postBoolean("addDataSource", ds.toMap(), new Object[0]);
		if ((res) && (!initialLoad)) {
			dm.syncSave(this);
		}
		return res;
	}

	public boolean exists(DataSourceId dsId) throws Exception {
		if (noCM) {
			return noopCm.exists(dsId);
		}
		return getBoolean("exists", new Object[] { "id", dsId.toString() });
	}

	public DataSource getDataSource(DataSourceId dsId) throws Exception {
		if (noCM) {
			return noopCm.getDataSource(dsId);
		}
		Map<String, Object> map = null;
		try {
			map = getMap("getDataSource", new Object[] { "id", dsId.toString() });
		} catch (Exception e) {
			LOG.error("getDataSource: " + e.toString());
			return null;
		}
		if (map == null) {
			return null;
		}
		return DataSource.fromMap(map);
	}

	public List<DataSource> getDataSources(String collection) throws Exception {
		if (noCM) {
			return noopCm.getDataSources(collection);
		}
		List<Map> lst = null;
		try {
			lst = getList("getDataSources", Map.class, new Object[] { "collection", collection });
		} catch (Exception e) {
			LOG.error("getDataSources: " + e.toString());
		}
		if ((lst == null) || (lst.isEmpty())) {
			return Collections.emptyList();
		}
		List<DataSource> res = new ArrayList<DataSource>(lst.size());
		for (Map<String, Object> m : lst) {
			DataSource ds = DataSource.fromMap(m);
			if (ds == null) {
				LOG.warn("No valid data source in " + m);
			} else
				res.add(ds);
		}
		return res;
	}

	public List<DataSourceId> listDataSources(String collection) throws Exception {
		if (noCM) {
			return noopCm.listDataSources(collection);
		}
		List<String> lst = null;
		try {
			lst = getList("listDataSources", String.class, new Object[] { "collection", collection });
		} catch (Exception e) {
			LOG.error("listDataSources: " + e.toString());
		}
		if ((lst == null) || (lst.isEmpty())) {
			return Collections.emptyList();
		}
		List<DataSourceId> res = new ArrayList<DataSourceId>(lst.size());
		for (String s : lst) {
			res.add(new DataSourceId(s));
		}
		return res;
	}

	public boolean removeDataSource(DataSourceId dsId, boolean force) throws Exception {
		if (noCM) {
			return noopCm.removeDataSource(dsId, force);
		}
		boolean res = deleteBoolean("removeDataSource",
				new Object[] { "id", dsId.toString(), "force", Boolean.valueOf(force) });

		if (res) {
			dm.syncSave(this);
		}
		return res;
	}

	public List<DataSourceId> removeDataSources(String collection, boolean force) throws Exception {
		if (noCM) {
			return noopCm.removeDataSources(collection, force);
		}
		List<String> lst = null;
		try {
			lst = deleteList("removeDataSources", String.class,
					new Object[] { "collection", collection, "force", Boolean.valueOf(force) });
		} catch (Exception e) {
			LOG.error("removeDataSources: " + e.toString());
		}
		if ((lst == null) || (lst.isEmpty())) {
			return Collections.emptyList();
		}
		List<DataSourceId> res = new ArrayList<DataSourceId>(lst.size());
		for (String s : lst) {
			res.add(new DataSourceId(s));
		}
		if (!res.isEmpty()) {
			dm.syncSave(this);
		}
		return res;
	}

	public boolean updateDataSource(DataSource ds) throws Exception {
		if (noCM) {
			return noopCm.updateDataSource(ds);
		}
		Map<String, Object> map = ds.toMap();
		boolean res = putBoolean("updateDataSource", map, new Object[0]);
		if (res) {
			dm.syncSave(this);
		}
		return res;
	}

	public void reset(boolean initRegistry) throws Exception {
		if (noCM) {
			noopCm.reset(initRegistry);
			return;
		}
		putBoolean("reset", null, new Object[] { "init", Boolean.valueOf(initRegistry) });
	}

	public void shutdown() throws Exception {
		if (checker != null) {
			checker.interrupt();
		}
		if (noCM) {
			noopCm.shutdown();
			return;
		}
		deleteVoid("shutdown", (Object[]) null);
		// client.stop();
		status.addMessage("shutdown");
	}

	public List<BatchStatus> listBatches(String crawler, String collection, String dsId) throws Exception {
		if (noCM) {
			return noopCm.listBatches(crawler, collection, dsId);
		}
		List<Map> lst = null;
		try {
			lst = getList("listBatches", Map.class, new Object[] { "crawler", crawler, "collection", collection, "id", dsId });
		} catch (Exception e) {
			LOG.error("listBatches: " + e.toString());
		}
		if ((lst == null) || (lst.isEmpty())) {
			return Collections.emptyList();
		}
		List<BatchStatus> res = new ArrayList<BatchStatus>(lst.size());
		for (Map<String, Object> m : lst) {
			BatchStatus bs = BatchStatus.fromMap(m);
			res.add(bs);
		}
		return res;
	}

	public BatchStatus getBatchStatus(String crawler, String collection, String batchId) throws Exception {
		if (noCM) {
			return noopCm.getBatchStatus(crawler, collection, batchId);
		}
		Map<String, Object> m = getMap("getBatchStatus", new Object[] { "crawler", crawler, "collection", collection, "id",
				batchId });

		if (m == null) {
			return null;
		}
		return BatchStatus.fromMap(m);
	}

	public boolean deleteBatches(String crawler, String collection, String batchId) throws Exception {
		if (noCM) {
			return noopCm.deleteBatches(crawler, collection, batchId);
		}
		return deleteBoolean("deleteBatches", new Object[] { "crawler", crawler, "collection", collection, "id", batchId });
	}

	public CrawlId startBatchJob(String crawler, String collection, String batchId, DataSource template, boolean parse,
			boolean index) throws Exception {
		if (noCM) {
			return noopCm.startBatchJob(crawler, collection, batchId, template, parse, index);
		}
		Map<String, Object> map = new HashMap<String, Object>();
		if (template != null) {
			map.put("template", template.toMap());
		}
		String id = (String) putMap("startBatchJob", String.class, map, new Object[] { "crawler", crawler, "collection",
				collection, "id", batchId, "parse", Boolean.valueOf(parse), "index", Boolean.valueOf(index) });

		return new CrawlId(id);
	}

	public List<Map<String, Object>> getBatchJobStatuses(String crawler, String collection, String batchId)
			throws Exception {
		if (noCM) {
			return noopCm.getBatchJobStatuses(crawler, collection, batchId);
		}
		List<Map> lst = getList("getBatchJobStatuses", Map.class, new Object[] { "crawler", crawler, "collection",
				collection, "id", batchId });

		if ((lst == null) || (lst.isEmpty())) {
			return Collections.emptyList();
		}
		List<Map<String, Object>> res = new ArrayList<Map<String, Object>>(lst.size());
		for (Map<String, Object> m : lst) {
			res.add(m);
		}
		return res;
	}

	public List<Map<String, Object>> getHistory(String key) throws Exception {
		if (noCM) {
			return noopCm.getHistory(key);
		}
		List<Map> lst = getList("getHistory", Map.class, new Object[] { "key", key });

		if ((lst == null) || (lst.isEmpty())) {
			return Collections.emptyList();
		}
		List<Map<String, Object>> res = new ArrayList<Map<String, Object>>(lst.size());
		for (Map<String, Object> m : lst) {
			res.add(m);
		}
		return res;
	}

	public Map<String, Object> getCumulativeHistory(String key) throws Exception {
		if (noCM) {
			return noopCm.getCumulativeHistory(key);
		}
		return getMap("getCumulativeHistory", new Object[] { "key", key });
	}

	public void removeHistory(String key) throws Exception {
		if (noCM) {
			noopCm.removeHistory(key);
			return;
		}
		deleteBoolean("removeHistory", new Object[] { "key", key });
	}

	public List<Resource> listResources(String crawler, String collection, DataSourceId dsId) throws Exception {
		if (noCM) {
			return noopCm.listResources(crawler, collection, dsId);
		}
		List<Resource> resources = new ArrayList<Resource>();
		try {
			List<Map> maps = getList("listResources", Map.class, new Object[] { "crawler", crawler, "collection", collection,
					"id", dsId });
			for (Map map : maps) {
				Resource res = Resource.fromMap(map);
				resources.add(res);
			}
		} catch (Exception e) {
			LOG.error("listResources: " + e.toString());
		}
		if ((resources == null) || (resources.isEmpty())) {
			return Collections.emptyList();
		}
		return resources;
	}

	public InputStream openResource(String crawler, String name, String collection, DataSourceId dsId) throws Exception {
		if (noCM) {
			return noopCm.openResource(crawler, name, collection, dsId);
		}
		// WebResource resource = res("openResource", new Object[] { "crawler",
		// crawler, "name", name, "collection",
		// collection, "id", dsId });
		// try {
		// Representation repr = resource.get();
		// if ((repr instanceof InputRepresentation)) {
		// InputRepresentation iRepr = (InputRepresentation) repr;
		// return iRepr.getStream();
		// }
		// throw new Exception("No binary data");
		// } finally {
		// // resource.delete();
		// }
		return null;
	}

	public void uploadResource(String crawler, Resource res, InputStream is, String collection, DataSourceId dsId)
			throws Exception {
		if (noCM) {
			noopCm.uploadResource(crawler, res, is, collection, dsId);
			return;
		}
		// FormDataSet form = new FormDataSet();
		// form.setMultipart(true);
		// form.getEntries().add(new FormData("crawler", crawler));
		// if (collection != null) {
		// form.getEntries().add(new FormData("collection", collection));
		// }
		// if (dsId != null) {
		// form.getEntries().add(new FormData("id", dsId.getId()));
		// }
		// File file = FileUtils.createFileInTempDirectory(res.getName(), is);
		// Representation fileRepresentation = new FileRepresentation(file,
		// MediaType.ALL);
		// form.getEntries().add(new FormData("file", fileRepresentation));
		//
		// if (res.getProperties() != null) {
		// for (Map.Entry entry : res.getProperties().entrySet()) {
		// form.getEntries().add(new FormData((String) entry.getKey(), (String)
		// entry.getValue()));
		// }
		// }
		//
		// WebResource resource = res("uploadResource", new Object[0]);
		// try {
		// resource.put(form);
		// } finally {
		// // resource.delete();
		// }
	}

	public void deleteResource(String crawler, String name, String collection, DataSourceId dsId) throws Exception {
		if (noCM) {
			noopCm.deleteResource(crawler, name, collection, dsId);
			return;
		}
		deleteBoolean("deleteResource", new Object[] { "crawler", crawler, "name", name, "collection", collection, "id",
				dsId });
	}

	public SecurityFilter buildSecurityFilter(DataSourceId dsId, String user) throws Exception {
		if (noCM) {
			return noopCm.buildSecurityFilter(dsId, user);
		}
		Map map = getMap("buildSecurityFilter", new Object[] { "id", dsId.toString(), "user", user });
		return SecurityFilter.fromMap(map);
	}

	public void deleteOutputData(DataSourceId dsId) throws UnsupportedOperationException, Exception {
		if (noCM) {
			try {
				noopCm.deleteOutputData(dsId);
			} catch (Exception t) {
				throw new WebApplicationException(t, Status.SERVICE_UNAVAILABLE);
			}
			return;
		}
		deleteVoid("deleteOutputData", new Object[] { "id", dsId.toString() });
	}

	private class CMCheckerThread extends Thread {
		public volatile int failures = 0;

		public CMCheckerThread() {
			setDaemon(true);
		}

		public void run() {
			while (true)
				if (!isInterrupted()) {
					try {
						getVersionInternal();
						boolean prevNoCM = noCM;
						noCM = false;
						status.clients.put(url, new ConnectorManager.ConnectorStatus("local", CStatus.OK, "online").toMap());
						failures = 0;
						if (prevNoCM) {
							String msg = "Synchronizing local and remote data sources...";
							LOG.info(msg);
							status.addMessage(msg);
							initialLoad = true;
							try {
								int cnt = dm.initialLoad(RestConnectorManager.this);
								msg = cnt + " data sources loaded and validated.";
								LOG.info(msg);
								status.addMessage(msg);
								status.status = CStatus.OK;
							} catch (Exception e) {
								msg = "Synchronization of data sources failed";
								LOG.warn(msg, e);
								status.addMessage(msg + ": " + e.toString());
								status.status = CStatus.WARNING;
							} finally {
								initialLoad = false;
							}
						}
					} catch (Throwable t) {
						failures += 1;
						if ((failures > 1) && (!noCM)) {
							fallback(t);
						}
					}
					try {
						Thread.sleep(heartbeatInterval);
					} catch (InterruptedException e) {
					}
				}
		}

		public void fallback(Throwable t) {
			if (noCM) {
				return;
			}
			String msg = "No connection to remote ConnectorManager! Switching to read-only cache (noop).";
			LOG.warn(msg);
			status.addMessage(msg + ": " + t.toString());
			status.status = CStatus.WARNING;
			status.clients.put(url, new ConnectorManager.ConnectorStatus("local", CStatus.ERROR, t.toString()).toMap());
			noCM = true;
		}
	}
}
