package gaia.jmx;

import gaia.admin.collection.CollectionManager;
import gaia.crawl.ConnectorManager;
import gaia.crawl.CrawlId;
import gaia.crawl.CrawlStatus;
import gaia.crawl.DataSourceManager;
import gaia.crawl.History;
import gaia.crawl.datasource.DataSource;
import gaia.crawl.datasource.DataSourceId;
import gaia.utils.StringUtils;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.AttributeNotFoundException;
import javax.management.DynamicMBean;
import javax.management.InvalidAttributeValueException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.ReflectionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class DefaultJmxManager implements JmxManager {
	private static Logger LOG = LoggerFactory.getLogger(DefaultJmxManager.class);
	private ConnectorManager crawlerManager;
	private CollectionManager collectionManager;
	private DataSourceManager dm;
	private MBeanServer server = null;
	private String jmxRootName = "gaia";

	@Inject
	public DefaultJmxManager(CollectionManager collectionManager, DataSourceManager dm, ConnectorManager crawlerManager) {
		this.collectionManager = collectionManager;
		this.dm = dm;
		this.crawlerManager = crawlerManager;

		List<MBeanServer> servers = null;
		servers = MBeanServerFactory.findMBeanServer(null);

		if ((servers == null) || (servers.isEmpty())) {
			LOG.warn("No JMX servers found, not exposing GaiaSearch information with JMX.");
			return;
		}

		server = servers.get(0);
		LOG.info("JMX monitoring is enabled. Adding GaiaSearch mbeans to JMX Server: " + server);

		registerOnStart();

		collectionManager.setJmxManager(this);
	}

	public void registerOnStart() {
		registerAggregatenMBean();
		Collection<String> collections = collectionManager.getCollectionNames();
		for (String collection : collections)
			registerCollectionMBean(collection);
		try {
			List<DataSourceId> dss = crawlerManager.listDataSources(null);
			for (DataSourceId ds : dss)
				try {
					registerDataSourceMBean(ds);
				} catch (Exception e) {
					LOG.warn("Unable to register datasource " + ds, e);
				}
		} catch (Exception e) {
			LOG.warn("Unable to register datasources on start, error listing datasources: " + e.toString());
		}
	}

	public void unregisterOnShutdown() {
		unregisterAggregatenMBean();
		Collection<String> collections = collectionManager.getCollectionNames();
		for (String collection : collections) {
			unregisterCollectionMBean(collection);
		}
		for (DataSource ds : dm.getDataSources())
			try {
				unregisterDataSourceMBean(ds.getDataSourceId());
			} catch (Exception e) {
				LOG.warn("Unable to unregister datasource " + ds, e);
			}
	}

	public void registerAggregatenMBean() {
		if (server == null)
			return;
		try {
			ObjectName name = getAggregateObjectName();
			if (server.isRegistered(name))
				server.unregisterMBean(name);
			CrawlersMBean mbean = new CrawlersMBean(this);
			server.registerMBean(mbean, name);
		} catch (Exception e) {
			LOG.warn("Failed to register info bean: aggregate crawlers stats for GaiaSearch", e);
		}
	}

	public void unregisterAggregatenMBean() {
		if (server == null)
			return;
		try {
			ObjectName name = getAggregateObjectName();
			if (server.isRegistered(name))
				server.unregisterMBean(name);
		} catch (Exception e) {
			LOG.warn("Failed to unregister info bean: aggregate crawlers stats for GaiaSearch", e);
		}
	}

	public ObjectName getAggregateObjectName() throws MalformedObjectNameException {
		Hashtable<String, String> map = new Hashtable<String, String>();
		map.put("type", "total");
		map.put("id", "crawlers");
		return ObjectName.getInstance(jmxRootName, map);
	}

	public void registerCollectionMBean(String collectionId) {
		if (server == null)
			return;
		try {
			ObjectName name = getCollectionObjectName(collectionId);
			if (server.isRegistered(name))
				server.unregisterMBean(name);
			CollectionCrawlersMBean mbean = new CollectionCrawlersMBean(collectionId, this);
			server.registerMBean(mbean, name);
		} catch (Exception e) {
			LOG.warn("Failed to register info bean: crawlers stats for collection " + collectionId, e);
		}
	}

	public void unregisterCollectionMBean(String collectionId) {
		if (server == null)
			return;
		try {
			ObjectName name = getCollectionObjectName(collectionId);
			if (server.isRegistered(name))
				server.unregisterMBean(name);
		} catch (Exception e) {
			LOG.warn("Failed to unregister info bean: crawlers stats for collection " + collectionId, e);
		}
	}

	public ObjectName getCollectionObjectName(String collectionId) throws MalformedObjectNameException {
		Hashtable<String, String> map = new Hashtable<String, String>();
		map.put("type", "collections");
		map.put("name", collectionId);
		map.put("id", "crawlers");
		return ObjectName.getInstance(jmxRootName, map);
	}

	public void registerDataSourceMBean(DataSourceId dataSourceId) {
		if (server == null)
			return;
		try {
			ObjectName name = getDataSourceObjectName(dataSourceId);
			if (server.isRegistered(name))
				server.unregisterMBean(name);
			DataSourceCrawlerMBean mbean = new DataSourceCrawlerMBean(dataSourceId, this);
			server.registerMBean(mbean, name);
		} catch (Exception e) {
			LOG.warn("Failed to register info bean: crawlers stats for data source " + dataSourceId, e);
		}
	}

	public void unregisterDataSourceMBean(DataSourceId dataSourceId) {
		if (server == null)
			return;
		try {
			ObjectName name = getDataSourceObjectName(dataSourceId);
			if (server.isRegistered(name))
				server.unregisterMBean(name);
		} catch (Exception e) {
			LOG.warn("Failed to unregister info bean: crawlers stats for data source " + dataSourceId, e);
		}
	}

	public ObjectName getDataSourceObjectName(DataSourceId dataSourceId) throws MalformedObjectNameException {
		Hashtable<String, String> map = new Hashtable<String, String>();
		map.put("type", "datasources");
		map.put("name", dataSourceId.toString());
		map.put("id", "crawlers");
		return ObjectName.getInstance(jmxRootName, map);
	}

	private Map<String, Long> getDataSourceCrawlerStats(DataSourceId dataSourceId) {
		Map<String, Long> result = new HashMap<String, Long>();
		DataSource ds = null;
		try {
			ds = crawlerManager.getDataSource(dataSourceId);
		} catch (Exception e1) {
			LOG.warn("Unable to get data source " + dataSourceId, e1);
		}
		if (ds == null) {
			LOG.error("Could not find data source " + dataSourceId);
			return Collections.emptyMap();
		}
		Map<String, Object> cumulativeDataSourceHistory;
		try {
			cumulativeDataSourceHistory = crawlerManager.getCumulativeHistory(dataSourceId.toString());
		} catch (Exception e1) {
			LOG.error("Could not retrieve cumulative history for " + dataSourceId + ": " + e1.toString());
			cumulativeDataSourceHistory = Collections.emptyMap();
		}

		for (Map.Entry<String, Object> entry : cumulativeDataSourceHistory.entrySet())
			result.put(entry.getKey(), Long.valueOf(((Number) entry.getValue()).longValue()));
		Map<String, Object> dataSourceStatus;
		try {
			dataSourceStatus = crawlerManager.getStatus(ds.getCrawlerType(), new CrawlId(ds.getDataSourceId()));
		} catch (Exception e) {
			LOG.error("Could not retrieve data source status", e);
			return result;
		}
		if (dataSourceStatus == null) {
			return result;
		}
		String crawlState = (String) dataSourceStatus.get("crawl_state");
		if ((!CrawlStatus.JobState.ABORTING.toString().equals(crawlState))
				&& (!CrawlStatus.JobState.FINISHING.toString().equals(crawlState))
				&& (!CrawlStatus.JobState.RUNNING.toString().equals(crawlState))
				&& (!CrawlStatus.JobState.STARTING.toString().equals(crawlState))
				&& (!CrawlStatus.JobState.STOPPING.toString().equals(crawlState))) {
			return result;
		}
		long crawlStarted;
		try {
			crawlStarted = StringUtils.parseDate((String) dataSourceStatus.get("crawl_started")).getTime();
		} catch (ParseException e) {
			LOG.warn("Could not parse crawlStarted date from status object", e);
			return result;
		}
		result.put("total_runs", Long.valueOf(((Number) result.get("total_runs")).longValue() + 1L));
		result.put("total_time",
				Long.valueOf(((Number) result.get("total_time")).longValue() + new Date().getTime() - crawlStarted));

		for (CrawlStatus.Counter counter : CrawlStatus.Counter.values()) {
			Object obj = dataSourceStatus.get(counter.toString());
			if (obj != null) {
				long val = (obj instanceof Number) ? ((Number) obj).longValue() : 0L;
				if (result.get(counter.toString()) == null)
					result.put(counter.toString(), Long.valueOf(val));
				else {
					result.put(counter.toString(), Long.valueOf(((Long) result.get(counter.toString())).longValue() + val));
				}
			}
		}
		return result;
	}

	private Map<String, Long> getCollectionCrawlerStats(String collection) {
		Map<String, Long> result = null;
		try {
			List<DataSource> dataSources = crawlerManager.getDataSources(collection);
			for (DataSource dataSource : dataSources)
				if (result == null) {
					result = new HashMap<String, Long>();
					result.putAll(getDataSourceCrawlerStats(dataSource.getDataSourceId()));
				} else {
					sum(result, getDataSourceCrawlerStats(dataSource.getDataSourceId()));
				}
		} catch (Exception e) {
			LOG.warn("Unable to get data source information for collection " + collection, e);
		}
		if (result == null) {
			result = new HashMap<String, Long>();
			for (Map.Entry<String, Object> entry : History.initEmptyItem().entrySet()) {
				result.put(entry.getKey(), Long.valueOf(((Number) entry.getValue()).longValue()));
			}
		}
		return result;
	}

	private Map<String, Long> getTotalCrawlerStats() {
		Map<String, Long> result = null;
		Collection<String> collections = collectionManager.getCollectionNames();
		for (String collection : collections) {
			if (result == null) {
				result = new HashMap<String, Long>();
				result.putAll(getCollectionCrawlerStats(collection));
			} else {
				sum(result, getCollectionCrawlerStats(collection));
			}
		}
		return result;
	}

	private void sum(Map<String, Long> res, Map<String, Long> arg) {
		for (String key : res.keySet())
			if (arg.get(key) != null)
				res.put(key, Long.valueOf(((Long) res.get(key)).longValue() + ((Long) arg.get(key)).longValue()));
	}

	public static class CrawlersMBean extends DefaultJmxManager.AbstractCrawlersStatsMBean {
		public CrawlersMBean(DefaultJmxManager jmxManager) {
			super(jmxManager);
		}

		protected Map<String, Long> getStats() {
			return jmxManager.getTotalCrawlerStats();
		}
	}

	public static class DataSourceCrawlerMBean extends DefaultJmxManager.AbstractCrawlersStatsMBean {
		private DataSourceId dataSourceId;

		public DataSourceCrawlerMBean(DataSourceId dataSourceId, DefaultJmxManager jmxManager) {
			super(jmxManager);
			this.dataSourceId = dataSourceId;
		}

		protected Map<String, Long> getStats() {
			return jmxManager.getDataSourceCrawlerStats(dataSourceId);
		}
	}

	public static class CollectionCrawlersMBean extends DefaultJmxManager.AbstractCrawlersStatsMBean {
		private String collection;

		public CollectionCrawlersMBean(String collection, DefaultJmxManager jmxManager) {
			super(jmxManager);
			this.collection = collection;
		}

		protected Map<String, Long> getStats() {
			return jmxManager.getCollectionCrawlerStats(collection);
		}
	}

	public static abstract class AbstractCrawlersStatsMBean implements DynamicMBean {
		protected DefaultJmxManager jmxManager;

		public AbstractCrawlersStatsMBean(DefaultJmxManager jmxManager) {
			this.jmxManager = jmxManager;
		}

		protected abstract Map<String, Long> getStats();

		public Object getAttribute(String attribute) throws AttributeNotFoundException, MBeanException, ReflectionException {
			Map<String, Long> stats = getStats();
			if (stats.get(attribute) != null)
				return stats.get(attribute);
			return null;
		}

		public void setAttribute(Attribute attribute) throws AttributeNotFoundException, InvalidAttributeValueException,
				MBeanException, ReflectionException {
			throw new UnsupportedOperationException("Operation not Supported");
		}

		public AttributeList getAttributes(String[] attributes) {
			AttributeList list = new AttributeList();
			Map<String, Long> stats = getStats();
			for (String key : attributes) {
				if (stats.get(key) != null) {
					list.add(new Attribute(key, stats.get(key)));
				}
			}
			return list;
		}

		public AttributeList setAttributes(AttributeList attributes) {
			throw new UnsupportedOperationException("Operation not Supported");
		}

		public Object invoke(String actionName, Object[] params, String[] signature) throws MBeanException,
				ReflectionException {
			throw new UnsupportedOperationException("Operation not Supported");
		}

		public MBeanInfo getMBeanInfo() {
			List<MBeanAttributeInfo> attrInfoList = new ArrayList<MBeanAttributeInfo>();

			Map<String, Long> stats = getStats();
			for (String key : stats.keySet()) {
				attrInfoList.add(new MBeanAttributeInfo(key, Long.class.getName(), null, true, false, false));
			}

			MBeanAttributeInfo[] attrInfoArr = (MBeanAttributeInfo[]) attrInfoList
					.toArray(new MBeanAttributeInfo[attrInfoList.size()]);
			return new MBeanInfo(getClass().getName(), "GaiaSearch crawlers runtime stats", attrInfoArr, null, null, null);
		}
	}
}
