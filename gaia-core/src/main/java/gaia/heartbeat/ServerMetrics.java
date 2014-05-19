package gaia.heartbeat;

import gaia.Constants;
import gaia.admin.collection.CollectionManager;
import gaia.crawl.DataSourceManager;
import gaia.crawl.datasource.DataSource;
import gaia.utils.StringUtils;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.lucene.index.DirectoryReader;
import org.apache.solr.common.cloud.SolrZkClient;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.core.CoreContainer;
import org.apache.solr.core.SolrCore;
import org.apache.solr.handler.component.SearchHandler;
import org.apache.solr.search.SolrIndexSearcher;
import org.apache.solr.util.RefCounted;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.servo.annotations.DataSourceType;
import com.netflix.servo.annotations.Monitor;

public class ServerMetrics {
	private static final Logger LOG = LoggerFactory.getLogger(ServerMetrics.class);
	private CollectionManager collectionManager;
	private CoreContainer cores;
	private DataSourceManager dsManager;
	private static final String zkClusterIdPath = "/lws-cluster-id";

	public ServerMetrics(CollectionManager collectionManager, CoreContainer cores, DataSourceManager dsManager) {
		this.collectionManager = collectionManager;
		this.cores = cores;
		this.dsManager = dsManager;
	}

	@Monitor(name = "jvm_memory_total", type = DataSourceType.GAUGE)
	public long getTotalJvmMemory() {
		return Runtime.getRuntime().totalMemory();
	}

	@Monitor(name = "jvm_memory_max", type = DataSourceType.GAUGE)
	public long getMaxJvmMemory() {
		long maxMemory = Runtime.getRuntime().maxMemory();
		return maxMemory == Long.MAX_VALUE ? 0L : maxMemory;
	}

	@Monitor(name = "jvm_memory_free", type = DataSourceType.GAUGE)
	public long getFreeJvmMemory() {
		return Runtime.getRuntime().freeMemory();
	}

	@Monitor(name = "num_collections", type = DataSourceType.GAUGE)
	public int getNumCollections() {
		Collection<String> names = collectionManager.getCollectionNames();
		if (names.contains(Constants.LOGS_COLLECTION)) {
			return names.size() - 1;
		}
		return names.size();
	}

	@Monitor(name = "num_docs", type = DataSourceType.GAUGE)
	public long getNumDocs() {
		long res = 0L;
		for (String name : cores.getCoreNames()) {
			if (!Constants.LOGS_COLLECTION.equals(name)) {
				SolrCore core = null;
				RefCounted<SolrIndexSearcher> searcher = null;
				try {
					core = cores.getCore(name);
					searcher = core.getNewestSearcher(true);
					DirectoryReader reader = ((SolrIndexSearcher) searcher.get()).getIndexReader();
					res += reader.numDocs();
				} finally {
					if (searcher != null) {
						searcher.decref();
					}
					if (core != null)
						core.close();
				}
			}
		}
		return res;
	}

	@Monitor(name = "num_adds", type = DataSourceType.GAUGE)
	public long getNumberOfAdds() {
		return getUpdateHandlerStatsItem("cumulative_adds");
	}

	private long getUpdateHandlerStatsItem(String item) {
		long res = 0L;
		for (String name : cores.getCoreNames()) {
			if (!Constants.LOGS_COLLECTION.equals(name)) {
				SolrCore core = null;
				try {
					core = cores.getCore(name);
					NamedList nl = core.getUpdateHandler().getStatistics();
					if (nl == null) {
						if (core != null)
							core.close();
					} else {
						Object obj = nl.get(item);
						if ((obj != null) && ((obj instanceof Long)))
							res += ((Long) obj).longValue();
					}
				} finally {
					if (core != null)
						core.close();
				}
			}
		}
		return res;
	}

	@Monitor(name = "num_datasources", type = DataSourceType.GAUGE)
	public int getNumDataSources() {
		int res = 0;
		List<DataSource> dataSources = dsManager.getDataSources();
		for (DataSource ds : dataSources)
			if (!Constants.LOGS_COLLECTION.equals(ds.getCollection())) {
				res++;
			}
		return res;
	}

	@Monitor(name = "num_search_requests", type = DataSourceType.GAUGE)
	public long getSearchRequests() {
		return getRequestHandlersStatsItem("requests");
	}

	private long getRequestHandlersStatsItem(String item) {
		long res = 0L;
		for (String name : cores.getCoreNames()) {
			if (!Constants.LOGS_COLLECTION.equals(name)) {
				SolrCore core = null;
				try {
					core = cores.getCore(name);
					Map<String, SearchHandler> searchHandlers = core.getRequestHandlers(SearchHandler.class);
					for (Map.Entry<String, SearchHandler> entry : searchHandlers.entrySet()) {
						SearchHandler searchHandler = entry.getValue();
						NamedList nl = searchHandler.getStatistics();
						if (nl != null) {
							Object obj = nl.get(item);
							if ((obj != null) && ((obj instanceof Long)))
								res += ((Long) obj).longValue();
						}
					}
				} finally {
					if (core != null)
						core.close();
				}
			}
		}
		return res;
	}

	@Monitor(name = "num_solrcloud_nodes", type = DataSourceType.GAUGE)
	public int numSolrCloudNodes() {
		if (cores.isZooKeeperAware()) {
			return cores.getZkController().getClusterState().getLiveNodes().size();
		}
		return 0;
	}

	public Boolean isSolrCloud() {
		return Boolean.valueOf(cores.isZooKeeperAware());
	}

	public String solrCloudClusterId() {
		if (!cores.isZooKeeperAware()) {
			return null;
		}
		SolrZkClient zkClient = cores.getZkController().getZkClient();
		try {
			if (zkClient.exists(zkClusterIdPath, false).booleanValue()) {
				byte[] bytes = zkClient.getData(zkClusterIdPath, null, null, false);
				return new String(bytes, StringUtils.UTF_8);
			}

			String id = UUID.randomUUID().toString();
			try {
				zkClient.makePath(zkClusterIdPath, id.getBytes(), CreateMode.PERSISTENT, null, true, false);
			} catch (KeeperException.NodeExistsException e) {
				return new String(zkClient.getData(zkClusterIdPath, null, null, false), StringUtils.UTF_8);
			}
			return id;
		} catch (Throwable e) {
			LOG.warn("Could not read/save SolrCloud cluster id from/to Zk", e);
		}
		return null;
	}
}
