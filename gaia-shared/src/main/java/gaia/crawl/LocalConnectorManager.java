package gaia.crawl;

import gaia.api.Error;
import gaia.crawl.batch.BatchManager;
import gaia.crawl.batch.BatchStatus;
import gaia.crawl.datasource.DataSource;
import gaia.crawl.datasource.DataSourceId;
import gaia.crawl.datasource.DataSourceSpec;
import gaia.crawl.resource.Resource;
import gaia.crawl.resource.ResourceManager;
import gaia.crawl.security.SecurityFilter;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class LocalConnectorManager implements ConnectorManager {
	private static transient Logger LOG = LoggerFactory.getLogger(LocalConnectorManager.class);
	private CrawlerControllerRegistry ccr;

	// FIXME: by whlee21
	public static final String[][] DEFAULT_BUILTIN_CRAWLERS = {
			{ "gaia.crawl.aperture.ApertureCrawlerController", CC_APERTURE },
			// { "gaia.crawl.gcm.GCMController", CC_GCM },
			{ "gaia.crawl.dih.DIHCrawlerController", CC_DIH },
			{ "gaia.crawl.solrxml.SolrXmlCrawlerController", CC_SOLRXML },
			{ "gaia.crawl.gaialogs.LogCrawlerController", CC_GAIALOGS },
			// { "gaia.crawl.external.ExternalCrawlerController", CC_EXTERNAL },
			{ "gaia.crawl.fs.FSCrawlerController", CC_FS },
			{ "gaia.crawl.twitter.stream.TwitterStreamController", CC_TCS },
			{ "gaia.crawl.behemoth.BehemothController", CC_HV_HDFS },
	// { "gaia.crawl.mongodb.MongoDBCrawlerController", CC_MONGO_DB }
	};
	private DataSourceManager dm;
	private History history;
	private String version;
	private ConnectorManager.ConnectorStatus status = new ConnectorManager.ConnectorStatus();

	@Inject
	public LocalConnectorManager(CrawlerControllerRegistry crawlerControllerRegistry, DataSourceManager dm,
			DataSourceHistory history) {
		ccr = crawlerControllerRegistry;
		this.dm = dm;
		this.history = history;
		version = UUID.randomUUID().toString();
		status.type = "Local ConnectorManager";
		String msg = "Initiating data source loading and back-compat verification...";
		LOG.info(msg);
		try {
			int cnt = dm.initialLoad(this);
			msg = cnt + " data sources loaded and verified.";
			LOG.info(msg);
			status.addMessage(msg);
		} catch (Exception e) {
			msg = "Initial load of data sources failed";
			LOG.warn(msg, e);
			status.addMessage(msg + ": " + e.toString());
		}
	}

	public String getVersion() {
		return version;
	}

	public ConnectorManager.ConnectorStatus getConnectorStatus() {
		return status;
	}

	public Set<String> getCrawlerTypes() {
		return ccr.getControllers().keySet();
	}

	public List<String> initCrawlersFromJar(String url) throws Exception {
		if (LOG.isDebugEnabled()) {
			LOG.debug("initCrawlersFromJar(" + url + ")");
		}
		Map<String, Class<? extends CrawlerController>> ccs = ccr.initCrawlersFromJar(url);
		return new ArrayList<String>(ccs.keySet());
	}

	public boolean initCrawler(String alias, String className) throws Exception {
		if (LOG.isDebugEnabled()) {
			LOG.debug("initCrawler(" + alias + "," + className + ")");
		}
		return ccr.initCrawler(alias, className) != null;
	}

	public Map<String, Object> getCrawlerSpecs(String crawlerType) throws Exception {
		if (LOG.isDebugEnabled()) {
			LOG.debug("getCrawlerSpecs(" + crawlerType + ")");
		}
		Map<String, Object> res = new HashMap<String, Object>();
		List<Map<String, Object>> datasourcesTypes = new ArrayList<Map<String, Object>>();
		CrawlerController cc = ccr.get(crawlerType);
		if (cc == null) {
			throw new Exception("Crawler controller '" + crawlerType + "' not found.");
		}
		DataSourceFactory dsf = cc.getDataSourceFactory();
		for (Map.Entry<String, DataSourceSpec> spec : dsf.getDataSourceSpecs().entrySet()) {
			Map<String, Object> smap = DataSourceSpec.toMap((DataSourceSpec) spec.getValue());
			smap.put("type", spec.getKey());
			datasourcesTypes.add(smap);
		}
		res.put("name", crawlerType);
		res.put("datasource_types", datasourcesTypes);
		return res;
	}

	public Map<String, Object> getCrawlerSpec(String crawlerType, String dsType) {
		if (LOG.isDebugEnabled()) {
			LOG.debug("getCrawlerSpec(" + crawlerType + "," + dsType + ")");
		}
		CrawlerController cc = ccr.get(crawlerType);
		if (cc == null) {
			return null;
		}
		DataSourceFactory dsf = cc.getDataSourceFactory();
		Map<String, DataSourceSpec> specs = dsf.getDataSourceSpecs();
		DataSourceSpec spec = (DataSourceSpec) specs.get(dsType);
		if (spec == null) {
			return null;
		}
		return DataSourceSpec.toMap(spec);
	}

	public boolean isAvailable(String crawlerType) {
		if (LOG.isDebugEnabled()) {
			LOG.debug("isAvailable(" + crawlerType + ")");
		}
		return ccr.isLoaded(crawlerType);
	}

	public DataSource createDataSource(Map<String, Object> map) throws DataSourceFactoryException {
		if (LOG.isDebugEnabled()) {
			LOG.debug("createDataSource(" + map + ")");
		}
		String ccType = (String) map.get("crawler");
		if (ccType == null) {
			throw new DataSourceFactoryException("crawler", Error.E_MISSING_VALUE);
		}
		CrawlerController cc = ccr.get(ccType);
		if (cc == null) {
			throw new DataSourceFactoryException("Crawler controller '" + ccType + "' not found.", new Error("crawler",
					Error.E_INVALID_VALUE));
		}
		DataSource ds = cc.getDataSourceFactory().create(map, (String) map.get("collection"));
		return ds;
	}

	public DataSource validateDataSource(DataSource input, boolean removeUnknown, boolean verifyAccess)
			throws DataSourceFactoryException {
		if (LOG.isDebugEnabled()) {
			LOG.debug("validateDataSource(" + input + "," + removeUnknown + ")");
		}
		CrawlerController cc = ccr.get(input.getCrawlerType());
		if (cc == null) {
			throw new DataSourceFactoryException("Crawler controller '" + input.getCrawlerType() + "' not found.", new Error(
					"crawler", Error.E_INVALID_VALUE));
		}
		DataSourceSpec spec = (DataSourceSpec) cc.getDataSourceFactory().getDataSourceSpecs().get(input.getType());
		if (spec == null)
			throw new DataSourceFactoryException("Data source type '" + input.getType() + "' in crawler controller '"
					+ input.getCrawlerType() + "' not found.", new Error("type", Error.E_INVALID_VALUE));
		Map<String, Object> props;
		if (removeUnknown) {
			props = input.getProperties();
			Set<String> keys = new HashSet<String>(props.keySet());
			for (String s : keys) {
				if (spec.getSpecProperty(s) == null) {
					props.remove(s);
				}
			}
		}
		List<Error> errors = null;
		Map<String, Object> map = input.toMap();
		if (spec.getSpecProperty("verify_access") != null) {
			if (!verifyAccess) {
				map.put("verify_access", Boolean.valueOf(false));
			}
		}
		errors = spec.validate(map);
		if (!errors.isEmpty()) {
			throw new DataSourceFactoryException("validation errors", errors);
		}
		spec.cast(input.getProperties());

		if ((spec.getSpecProperty("mapping") != null) && (input.getFieldMapping() == null)) {
			input.setFieldMapping(spec.getDefaultFieldMapping());
		}

		return input;
	}

	public void setClosing(String crawlerController, String collection, boolean value) throws Exception {
		if (LOG.isDebugEnabled()) {
			LOG.debug("setClosing(" + crawlerController + "," + collection + "," + value + ")");
		}
		if (crawlerController == null) {
			for (CrawlerController cc : ccr.getControllers().values())
				cc.setClosing(collection, value);
		} else {
			CrawlerController cc = ccr.get(crawlerController);
			if (cc == null) {
				throw new Exception("Crawler '" + crawlerController + "' not found.");
			}
			cc.setClosing(collection, value);
		}
	}

	public Map<String, Object> reset(String crawlerController, String collection, DataSourceId dsId) throws Exception {
		if (LOG.isDebugEnabled()) {
			LOG.debug("reset(" + crawlerController + "," + collection + "," + dsId + ")");
		}
		if (crawlerController == null) {
			for (CrawlerController cc : ccr.getControllers().values())
				cc.reset(collection, dsId);
		} else {
			CrawlerController cc = ccr.get(crawlerController);
			if (cc == null) {
				throw new Exception("Crawler '" + crawlerController + "' not found.");
			}
			cc.reset(collection, dsId);
		}
		return Collections.emptyMap();
	}

	public Map<String, Object> resetAll(String crawlerController, String collection) throws Exception {
		if (LOG.isDebugEnabled()) {
			LOG.debug("resetAll(" + crawlerController + "," + collection + ")");
		}
		if (crawlerController == null) {
			for (CrawlerController cc : ccr.getControllers().values())
				cc.resetAll(collection);
		} else {
			CrawlerController cc = ccr.get(crawlerController);
			if (cc == null) {
				throw new Exception("Crawler '" + crawlerController + "' not found.");
			}
			cc.resetAll(collection);
		}
		return Collections.emptyMap();
	}

	public List<CrawlId> close(String crawlerController, String collection, boolean dryRun) throws Exception {
		if (LOG.isDebugEnabled()) {
			LOG.debug("close(" + crawlerController + "," + collection + "," + dryRun + ")");
		}
		if (crawlerController == null) {
			List<CrawlId> res = new ArrayList<CrawlId>();
			for (CrawlerController cc : ccr.getControllers().values()) {
				res.addAll(cc.close(collection, dryRun));
			}
			return res;
		}
		CrawlerController cc = ccr.get(crawlerController);
		if (cc == null) {
			throw new Exception("Crawler '" + crawlerController + "' not found.");
		}
		return cc.close(collection, dryRun);
	}

	public Map<String, Object> crawl(DataSourceId dsId) throws Exception {
		if (LOG.isDebugEnabled()) {
			LOG.debug("crawl(" + dsId + ")");
		}
		DataSource ds = getDataSource(dsId);
		if (ds == null) {
			throw new Exception("Unknown datasource " + dsId);
		}

		CrawlerController controller = ccr.get(ds.getCrawlerType());

		CrawlStatus status = null;
		try {
			MDC.put("datasource", ds.getDisplayName());
			MDC.put("collection", ds.getCollection());

			status = CrawlerUtils.crawl(controller, ds, null);
		} finally {
			MDC.remove("datasource");
			MDC.remove("collection");
		}

		if (status == null) {
			return Collections.emptyMap();
		}
		return status.toMap();
	}

	public List<Map<String, Object>> listJobs(String crawlerController, boolean batch) throws Exception {
		if (LOG.isDebugEnabled()) {
			LOG.debug("listJobs(" + crawlerController + "," + batch + ")");
		}
		List<Map<String, Object>> res = new LinkedList<Map<String, Object>>();
		List<CrawlerController> ccs = new LinkedList<CrawlerController>();
		if (crawlerController != null) {
			CrawlerController cc = ccr.get(crawlerController);
			if (cc == null) {
				throw new Exception("Crawler '" + crawlerController + "' not found.");
			}
			ccs.add(cc);
		} else {
			ccs.addAll(ccr.getControllers().values());
		}
		for (CrawlerController cc : ccs) {
			List<CrawlStatus> stats;
			if (batch)
				stats = cc.listBatchJobs();
			else {
				stats = cc.listJobs();
			}
			for (CrawlStatus cs : stats) {
				res.add(cs.toMap());
			}
		}
		return res;
	}

	public Map<String, Object> getStatus(String crawlerController, CrawlId jobId) throws Exception {
		if (LOG.isDebugEnabled()) {
			LOG.debug("getStatus(" + crawlerController + "," + jobId + ")");
		}
		CrawlStatus status = null;
		if (crawlerController != null) {
			CrawlerController cc = ccr.get(crawlerController);
			if (cc == null) {
				throw new Exception("Crawler '" + crawlerController + "' not found.");
			}
			status = cc.getStatus(jobId);
		} else {
			for (CrawlerController cc : ccr.getControllers().values()) {
				status = cc.getStatus(jobId);
				if (status != null) {
					break;
				}
			}
		}
		if (status == null) {
			List<Map<String, Object>> hist = history.getHistory(jobId.toString());
			if ((hist != null) && (hist.size() > 0)) {
				return hist.get(hist.size() - 1);
			}

			status = new CrawlStatus(jobId, new DataSourceId(jobId.toString()));
			return status.toMap();
		}

		return status.toMap();
	}

	public List<Error> finishAllJobs(String crawlerController, String collection, boolean abort, long wait) {
		if (LOG.isDebugEnabled()) {
			LOG.debug("finishAllJobs(" + crawlerController + "," + collection + "," + abort + "," + wait + ")");
		}
		return CrawlerUtils.finishAllJobs(ccr, crawlerController, collection, abort, wait);
	}

	public synchronized boolean addDataSource(DataSource ds) throws Exception {
		if (LOG.isDebugEnabled()) {
			LOG.debug("addDataSource(" + ds + ")");
		}
		CrawlerController cc = ccr.get(ds.getCrawlerType());
		if (cc == null) {
			throw new IOException("Crawler type '" + ds.getCrawlerType() + "' not present.");
		}
		DataSourceSpec spec = cc.getDataSourceFactory().getSpec(ds.getType());
		if (spec == null) {
			throw new IOException("Data source type '" + ds.getType() + "' not present.");
		}

		spec.cast(ds.getProperties());
		boolean res = cc.getDataSourceRegistry().addDataSource(ds);
		if (res) {
			dm.syncSave(this);
		}
		return res;
	}

	public synchronized boolean exists(DataSourceId dsId) throws Exception {
		if (LOG.isDebugEnabled()) {
			LOG.debug("exists(" + dsId + ")");
		}
		for (CrawlerController cc : ccr.getControllers().values()) {
			if (cc.getDataSourceRegistry().getDataSource(dsId) != null) {
				return true;
			}
		}
		return false;
	}

	public synchronized DataSource getDataSource(DataSourceId dsId) {
		if (LOG.isDebugEnabled()) {
			LOG.debug("getDataSource(" + dsId + ")");
		}
		DataSource res = null;
		for (CrawlerController cc : ccr.getControllers().values()) {
			res = cc.getDataSourceRegistry().getDataSource(dsId);
			if (res != null) {
				break;
			}
		}
		return res;
	}

	public synchronized List<DataSource> getDataSources(String collection) {
		if (LOG.isDebugEnabled()) {
			LOG.debug("getDataSources(" + collection + ")");
		}
		List<DataSource> res = new LinkedList<DataSource>();
		for (CrawlerController cc : ccr.getControllers().values()) {
			List<DataSource> dss = cc.getDataSourceRegistry().getDataSources(collection);
			if ((dss != null) && (!dss.isEmpty())) {
				res.addAll(dss);
			}
		}
		return res;
	}

	public synchronized List<DataSourceId> listDataSources(String collection) {
		if (LOG.isDebugEnabled()) {
			LOG.debug("listDataSources(" + collection + ")");
		}
		List<DataSourceId> res = new LinkedList<DataSourceId>();
		for (CrawlerController cc : ccr.getControllers().values()) {
			List<DataSourceId> dss = cc.getDataSourceRegistry().listDataSources(collection);
			if ((dss != null) && (!dss.isEmpty())) {
				res.addAll(dss);
			}
		}
		return res;
	}

	public synchronized boolean removeDataSource(DataSourceId dsId, boolean force) throws Exception {
		if (LOG.isDebugEnabled()) {
			LOG.debug("removeDataSource(" + dsId + "," + force + ")");
		}
		DataSource ds = getDataSource(dsId);
		if (ds == null) {
			throw new IOException("Data source id=" + dsId + " not found.");
		}
		CrawlerController cc = ccr.get(ds.getCrawlerType());
		if (cc == null) {
			throw new IOException("Crawler type '" + ds.getCrawlerType() + "' not present.");
		}
		boolean res = cc.getDataSourceRegistry().removeDataSource(dsId, force);
		if (res) {
			dm.syncSave(this);
		}
		return res;
	}

	public synchronized List<DataSourceId> removeDataSources(String collection, boolean force) throws Exception {
		if (LOG.isDebugEnabled()) {
			LOG.debug("removeDataSources(" + collection + "," + force + ")");
		}
		List<DataSourceId> res = new LinkedList<DataSourceId>();
		for (CrawlerController cc : ccr.getControllers().values()) {
			List<DataSourceId> dss = cc.getDataSourceRegistry().removeDataSources(collection, force);
			if ((dss != null) && (!dss.isEmpty())) {
				res.addAll(dss);
			}
		}
		if (!res.isEmpty()) {
			dm.syncSave(this);
		}
		return res;
	}

	public synchronized boolean updateDataSource(DataSource ds) throws Exception {
		if (LOG.isDebugEnabled()) {
			LOG.debug("updateDataSource(" + ds + ")");
		}
		CrawlerController cc = ccr.get(ds.getCrawlerType());
		if (cc == null) {
			throw new IOException("Crawler type '" + ds.getCrawlerType() + "' not present.");
		}
		boolean res = cc.getDataSourceRegistry().updateDataSource(ds);
		if (res) {
			dm.syncSave(this);
		}
		return res;
	}

	public void reset(boolean initRegistry) throws Exception {
		if (LOG.isDebugEnabled()) {
			LOG.debug("initRegistry(" + initRegistry + ")");
		}
		ccr.reset(initRegistry);
		status.addMessage("reset initRegistry=" + initRegistry);
	}

	public void shutdown() throws Exception {
		if (LOG.isDebugEnabled()) {
			LOG.debug("shutdown()");
		}
		ccr.shutdown();
		status.addMessage("shutdown");
	}

	public List<BatchStatus> listBatches(String crawler, String collection, String dsId) throws Exception {
		if (LOG.isDebugEnabled()) {
			LOG.debug("listBatches(" + crawler + "," + collection + "," + dsId + ")");
		}
		List<BatchStatus> res = new ArrayList<BatchStatus>();
		List<CrawlerController> ccs = new ArrayList<CrawlerController>();
		if (crawler == null) {
			ccs.addAll(ccr.getControllers().values());
		} else {
			CrawlerController cc = ccr.get(crawler);
			if (cc == null) {
				throw new Exception("Crawler controller '" + crawler + "' not present.");
			}
			if (cc.getBatchManager() == null)
				LOG.warn("Crawler controller '" + crawler + "' does not support batch operations.");
			else {
				ccs.add(cc);
			}
		}
		for (CrawlerController cc : ccs)
			if (cc.getBatchManager() != null) {
				BatchManager bm = cc.getBatchManager();
				res.addAll(bm.listBatchStatuses(collection, dsId));
			}
		return res;
	}

	public boolean deleteBatches(String crawler, String collection, String batchId) throws Exception {
		if (LOG.isDebugEnabled()) {
			LOG.debug("deleteBatches(" + crawler + "," + collection + "," + batchId + ")");
		}
		List<CrawlerController> ccs = new ArrayList<CrawlerController>();
		if (crawler == null) {
			ccs.addAll(ccr.getControllers().values());
		} else {
			CrawlerController cc = ccr.get(crawler);
			if (cc == null) {
				throw new Exception("Crawler controller '" + crawler + "' not present.");
			}
			if (cc.getBatchManager() == null)
				LOG.warn("deleteBatches: crawler controller '" + crawler + "' does not support batch operations.");
			else {
				ccs.add(cc);
			}
		}
		boolean res = false;
		for (CrawlerController cc : ccs) {
			if (cc.getBatchManager() != null) {
				BatchManager bm = cc.getBatchManager();
				if (bm.deleteBatches(collection, batchId))
					res = true;
			}
		}
		return res;
	}

	public BatchStatus getBatchStatus(String crawler, String collection, String batchId) throws Exception {
		if (LOG.isDebugEnabled()) {
			LOG.debug("getBatchStatus(" + crawler + "," + collection + "," + batchId + ")");
		}
		CrawlerController cc = ccr.get(crawler);
		if (cc == null) {
			throw new Exception("Crawler controller '" + crawler + "' not present.");
		}
		if (cc.getBatchManager() == null) {
			throw new Exception("Crawler controller '" + crawler + "' does not support batch operations.");
		}
		return cc.getBatchManager().getBatchStatus(collection, batchId);
	}

	public CrawlId startBatchJob(String crawler, String collection, String batchId, DataSource template, boolean parse,
			boolean index) throws Exception {
		if (LOG.isDebugEnabled()) {
			LOG.debug("startBatchJob(" + crawler + "," + collection + "," + batchId + "," + template + "," + parse + ","
					+ index + ")");
		}

		CrawlerController cc = ccr.get(crawler);
		if (cc == null) {
			throw new Exception("Crawler controller '" + crawler + "' not present.");
		}
		BatchStatus bs = getBatchStatus(crawler, collection, batchId);
		if (bs == null) {
			throw new Exception("Batch " + crawler + "/" + collection + "/" + batchId + " not found");
		}

		return cc.startBatchJob(bs, template, null, null, parse, index);
	}

	public List<Map<String, Object>> getBatchJobStatuses(String crawler, String collection, String batchId)
			throws Exception {
		if (LOG.isDebugEnabled()) {
			LOG.debug("getBatchStatuses(" + crawler + "," + collection + "," + batchId + ")");
		}
		List<Map<String, Object>> res = new ArrayList<Map<String, Object>>();
		List<CrawlerController> ccs = new ArrayList<CrawlerController>();
		if (crawler == null) {
			ccs.addAll(ccr.getControllers().values());
		} else {
			CrawlerController cc = ccr.get(crawler);
			if (cc == null) {
				throw new Exception("Crawler controller '" + crawler + "' not present.");
			}
			if (cc.getBatchManager() == null) {
				throw new Exception("Crawler controller '" + crawler + "' does not support batch operations.");
			}
			ccs.add(cc);
		}
		for (CrawlerController cc : ccs) {
			if (cc.getBatchManager() != null) {
				BatchManager bm = cc.getBatchManager();
				if (batchId != null) {
					BatchStatus bs = bm.getBatchStatus(collection, batchId);
					if (bs != null) {
						res.add(bs.toMap());
					}
				} else {
					List<BatchStatus> lst = bm.listBatchStatuses(collection);
					if ((lst != null) && (!lst.isEmpty())) {
						for (BatchStatus bs : lst)
							res.add(bs.toMap());
					}
				}
			}
		}
		return res;
	}

	public boolean stopJob(String crawler, CrawlId jobId, boolean abort, long waitTime) throws Exception {
		if (LOG.isDebugEnabled()) {
			LOG.debug("stopJob(" + crawler + "," + jobId + "," + abort + "," + waitTime + ")");
		}
		CrawlerController cc = ccr.get(crawler);
		if (cc == null) {
			throw new Exception("Crawler controller '" + crawler + "' not present.");
		}
		if (!cc.jobExists(jobId)) {
			return false;
		}
		if (abort)
			cc.abortJob(jobId);
		else {
			cc.stopJob(jobId);
		}
		if (waitTime > 0L) {
			CrawlerUtils.waitJob(cc, jobId, waitTime);
		}
		return true;
	}

	public List<Map<String, Object>> getHistory(String key) {
		if (LOG.isDebugEnabled()) {
			LOG.debug("getHistory(" + key + ")");
		}
		return history.getHistory(key);
	}

	public Map<String, Object> getCumulativeHistory(String key) {
		if (LOG.isDebugEnabled()) {
			LOG.debug("getCumulativeHistory(" + key + ")");
		}
		return history.getCumulativeHistory(key);
	}

	public void removeHistory(String key) {
		if (LOG.isDebugEnabled()) {
			LOG.debug("removeHistory(" + key + ")");
		}
		history.removeHistory(key);
	}

	public List<Resource> listResources(String crawler, String collection, DataSourceId dsId) throws Exception {
		if (LOG.isDebugEnabled()) {
			LOG.debug("listResources(" + crawler + "," + collection + "," + dsId + ")");
		}
		ResourceManager rm = getResourceManager(crawler);
		return rm.listResources(collection, dsId);
	}

	public void uploadResource(String crawler, Resource res, InputStream is, String collection, DataSourceId dsId)
			throws Exception {
		if (LOG.isDebugEnabled()) {
			LOG.debug("uploadResource(" + crawler + "," + res + "," + is + "," + collection + "," + dsId + ")");
		}

		ResourceManager rm = getResourceManager(crawler);
		rm.uploadResource(collection, dsId, res, is, true);
	}

	public void deleteResource(String crawler, String name, String collection, DataSourceId dsId) throws Exception {
		if (LOG.isDebugEnabled()) {
			LOG.debug("deleteResource(" + crawler + "," + name + "," + collection + "," + dsId + ")");
		}
		ResourceManager rm = getResourceManager(crawler);
		rm.deleteResource(collection, dsId, name);
	}

	public InputStream openResource(String crawler, String name, String collection, DataSourceId dsId) throws Exception {
		if (LOG.isDebugEnabled()) {
			LOG.debug("openResource(" + crawler + "," + name + "," + collection + "," + dsId + ")");
		}
		ResourceManager rm = getResourceManager(crawler);
		return rm.openResource(collection, dsId, name);
	}

	private ResourceManager getResourceManager(String crawler) throws Exception {
		CrawlerController cc = ccr.get(crawler);
		if (cc == null) {
			throw new Exception("Crawler controller '" + crawler + "' not present.");
		}
		if (cc.getResourceManager() == null) {
			throw new Exception("Crawler controller '" + crawler + "' does not support resource uploads.");
		}
		return cc.getResourceManager();
	}

	public SecurityFilter buildSecurityFilter(DataSourceId dsId, String user) throws Exception {
		if (LOG.isDebugEnabled()) {
			LOG.debug("buildSecurityFilter(" + dsId + ", " + user + ")");
		}
		DataSource ds = getDataSource(dsId);
		if (ds == null) {
			throw new Exception("Unknown datasource " + dsId);
		}

		CrawlerController controller = ccr.get(ds.getCrawlerType());
		SecurityFilter filter = controller.buildSecurityFilter(ds, user);
		return filter;
	}

	public void deleteOutputData(DataSourceId dsId) throws UnsupportedOperationException, Exception {
		DataSource ds = getDataSource(dsId);
		if (ds == null) {
			throw new Exception("Unknown datasource " + dsId);
		}
		CrawlerController cc = ccr.get(ds.getCrawlerType());
		String datasourceField = "data_source";
		if (ds.getFieldMapping() != null) {
			datasourceField = ds.getFieldMapping().getDatasourceField();
			if (datasourceField == null) {
				throw new UnsupportedOperationException("data_source field not defined in field mapping");
			}
		}

		ds.setFieldMapping(null);
		UpdateController update = UpdateController.create(cc, ds);
		update.start();
		update.deleteByQuery(datasourceField + ":" + dsId.toString());
		update.finish(true);
	}
}
