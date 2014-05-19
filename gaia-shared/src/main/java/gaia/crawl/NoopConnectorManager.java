package gaia.crawl;

import gaia.api.Error;
import gaia.crawl.batch.BatchStatus;
import gaia.crawl.datasource.DataSource;
import gaia.crawl.datasource.DataSourceId;
import gaia.crawl.resource.Resource;
import gaia.crawl.security.SecurityFilter;
import gaia.utils.DeepCopy;

import java.io.InputStream;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class NoopConnectorManager implements ConnectorManager {
	DataSourceManager dm;
	private static final ConnectorManager.ConnectorStatus status = new ConnectorManager.ConnectorStatus();

	public NoopConnectorManager() {
		status.type = "No-op ConnectorManager";
		status.addMessage("read-only datasource cache (local)");
	}

	public NoopConnectorManager(DataSourceManager dm) {
		this();
		this.dm = dm;
		status.addMessage("cached " + dm.getDataSources().size() + " datasources");
	}

	public String getVersion() throws Exception {
		return "NoopConnectorManager";
	}

	public ConnectorManager.ConnectorStatus getConnectorStatus() {
		return status;
	}

	public Set<String> getCrawlerTypes() throws Exception {
		return Collections.emptySet();
	}

	public List<String> initCrawlersFromJar(String url) throws Exception {
		return Collections.emptyList();
	}

	public boolean initCrawler(String alias, String className) throws Exception {
		return false;
	}

	public boolean isAvailable(String crawlerType) throws Exception {
		return false;
	}

	public Map<String, Object> getCrawlerSpecs(String crawlerType) throws Exception {
		return Collections.emptyMap();
	}

	public Map<String, Object> getCrawlerSpec(String crawlerType, String dsType) throws Exception {
		return Collections.emptyMap();
	}

	public DataSource createDataSource(Map<String, Object> map) throws Exception {
		return null;
	}

	public DataSource validateDataSource(DataSource input, boolean removeUnknownProps, boolean verifyAccess)
			throws Exception {
		return null;
	}

	public void setClosing(String crawlerController, String collection, boolean value) throws Exception {
	}

	private Map<String, Object> fakeStatus(DataSourceId dsId) {
		CrawlStatus cs = new CrawlStatus(new CrawlId(dsId), dsId);
		cs.setState(CrawlStatus.JobState.UNKNOWN);
		cs.setMessage("read-only offline cache, operations ignored");
		return cs.toMap();
	}

	public Map<String, Object> reset(String crawlerController, String collection, DataSourceId dsId) throws Exception {
		return fakeStatus(dsId);
	}

	public Map<String, Object> resetAll(String crawlerController, String collection) throws Exception {
		return Collections.emptyMap();
	}

	public List<CrawlId> close(String crawlerController, String collection, boolean dryRun) throws Exception {
		return Collections.emptyList();
	}

	public Map<String, Object> crawl(DataSourceId dsId) throws Exception {
		return fakeStatus(dsId);
	}

	public List<Map<String, Object>> listJobs(String crawlerController, boolean batch) throws Exception {
		return Collections.emptyList();
	}

	public Map<String, Object> getStatus(String crawlerController, CrawlId jobId) throws Exception {
		return fakeStatus(new DataSourceId(jobId.toString()));
	}

	public boolean stopJob(String crawler, CrawlId jobId, boolean abort, long waitTime) throws Exception {
		return false;
	}

	public List<Error> finishAllJobs(String crawlerController, String collection, boolean abort, long wait)
			throws Exception {
		return Collections.emptyList();
	}

	public boolean addDataSource(DataSource ds) throws Exception {
		return false;
	}

	public boolean exists(DataSourceId dsId) throws Exception {
		if (dm != null) {
			List<DataSource> dss = dm.getDataSources();
			for (DataSource ds : dss) {
				if (ds.getDataSourceId().equals(dsId)) {
					return true;
				}
			}
		}
		return false;
	}

	public DataSource getDataSource(DataSourceId dsId) throws Exception {
		if (dm != null) {
			List<DataSource> dss = dm.getDataSources();
			for (DataSource ds : dss) {
				if (ds.getDataSourceId().equals(dsId)) {
					return (DataSource) DeepCopy.copy(ds);
				}
			}
		}
		return null;
	}

	public List<DataSource> getDataSources(String collection) throws Exception {
		if (dm != null) {
			List<DataSource> lst = new LinkedList<DataSource>();
			List<DataSource> dss = dm.getDataSources();
			for (DataSource ds : dss) {
				if ((collection == null) || (ds.getCollection().equals(collection))) {
					lst.add((DataSource) DeepCopy.copy(ds));
				}
			}
			return lst;
		}
		return Collections.emptyList();
	}

	public List<DataSourceId> listDataSources(String collection) throws Exception {
		if (dm != null) {
			List<DataSourceId> lst = new LinkedList<DataSourceId>();
			List<DataSource> dss = dm.getDataSources();
			for (DataSource ds : dss) {
				if ((collection == null) || (ds.getCollection().equals(collection))) {
					lst.add(new DataSourceId(ds.getDataSourceId()));
				}
			}
			return lst;
		}
		return Collections.emptyList();
	}

	public boolean removeDataSource(DataSourceId dsId, boolean force) throws Exception {
		return false;
	}

	public List<DataSourceId> removeDataSources(String collection, boolean force) throws Exception {
		return Collections.emptyList();
	}

	public boolean updateDataSource(DataSource ds) throws Exception {
		return false;
	}

	public void reset(boolean initRegistry) throws Exception {
	}

	public void shutdown() throws Exception {
	}

	public List<BatchStatus> listBatches(String crawler, String collection, String dsId) throws Exception {
		return Collections.emptyList();
	}

	public BatchStatus getBatchStatus(String crawler, String collection, String batchId) throws Exception {
		return null;
	}

	public boolean deleteBatches(String crawler, String collection, String batchId) throws Exception {
		return false;
	}

	public CrawlId startBatchJob(String crawler, String collection, String batchId, DataSource template, boolean parse,
			boolean index) throws Exception {
		return null;
	}

	public List<Map<String, Object>> getBatchJobStatuses(String crawler, String collection, String batchId)
			throws Exception {
		return Collections.emptyList();
	}

	public List<Map<String, Object>> getHistory(String key) throws Exception {
		return Collections.emptyList();
	}

	public Map<String, Object> getCumulativeHistory(String key) throws Exception {
		return Collections.emptyMap();
	}

	public void removeHistory(String key) throws Exception {
	}

	public List<Resource> listResources(String crawler, String collection, DataSourceId dsId) throws Exception {
		return Collections.emptyList();
	}

	public void uploadResource(String crawler, Resource res, InputStream is, String collection, DataSourceId dsId)
			throws Exception {
	}

	public void deleteResource(String crawler, String name, String collection, DataSourceId dsId) throws Exception {
	}

	public InputStream openResource(String crawler, String name, String collection, DataSourceId dsId) throws Exception {
		return null;
	}

	public SecurityFilter buildSecurityFilter(DataSourceId dsId, String user) throws Exception {
		return null;
	}

	public void deleteOutputData(DataSourceId dsId) throws UnsupportedOperationException, Exception {
		throw new UnsupportedOperationException("read-only offline cache - operation not available");
	}
}
