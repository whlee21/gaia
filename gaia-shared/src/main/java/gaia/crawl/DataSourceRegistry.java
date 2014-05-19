package gaia.crawl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gaia.crawl.datasource.DataSource;
import gaia.crawl.datasource.DataSourceId;
import gaia.utils.DeepCopy;

public class DataSourceRegistry {
	private static final Logger LOG = LoggerFactory.getLogger(DataSourceRegistry.class);

	protected Set<CrawlListener> listeners = Collections.synchronizedSet(new HashSet<CrawlListener>());

	protected Map<DataSourceId, DataSource> datasources = new HashMap<DataSourceId, DataSource>();
	protected CrawlerController cc;

	public DataSourceRegistry(CrawlerController cc) {
		this.cc = cc;
	}

	public void addListener(CrawlListener listener) {
		listeners.add(listener);
	}

	public boolean removeListener(CrawlListener listener) {
		return listeners.remove(listener);
	}

	public void removeListeners() {
		listeners.clear();
	}

	protected void fireListeners(CrawlEvent e) {
		for (CrawlListener cl : listeners)
			cl.processEvent(e);
	}

	public synchronized boolean addDataSource(DataSource ds) throws Exception {
		if (datasources.containsKey(ds.getDataSourceId())) {
			fireListeners(new CrawlEvent(CrawlEvent.Type.ADD.toString(), CrawlEvent.Status.FAIL.toString(), "already exists",
					ds));
			return false;
		}
		datasources.put(ds.getDataSourceId(), (DataSource) DeepCopy.copy(ds));
		fireListeners(new CrawlEvent(CrawlEvent.Type.ADD.toString(), CrawlEvent.Status.OK.toString(), ds));
		return true;
	}

	public synchronized DataSource getDataSource(DataSourceId id) {
		DataSource ds = (DataSource) datasources.get(id);
		if (ds == null) {
			return null;
		}
		return (DataSource) DeepCopy.copy(ds);
	}

	public synchronized boolean updateDataSource(DataSource ds) {
		if (!datasources.containsKey(ds.getDataSourceId())) {
			fireListeners(new CrawlEvent(CrawlEvent.Type.UPDATE.toString(), CrawlEvent.Status.FAIL.toString(),
					"doesn't exist", ds));
			return false;
		}
		DataSource oldDs = (DataSource) datasources.get(ds.getDataSourceId());
		if (!oldDs.getCrawlerType().equals(ds.getCrawlerType())) {
			fireListeners(new CrawlEvent(CrawlEvent.Type.UPDATE.toString(), CrawlEvent.Status.FAIL.toString(),
					"crawler type mismatch", ds));
			return false;
		}
		if (!oldDs.getType().equals(ds.getType())) {
			fireListeners(new CrawlEvent(CrawlEvent.Type.UPDATE.toString(), CrawlEvent.Status.FAIL.toString(),
					"ds type mismatch", ds));
			return false;
		}
		datasources.put(ds.getDataSourceId(), (DataSource) DeepCopy.copy(ds));
		fireListeners(new CrawlEvent(CrawlEvent.Type.UPDATE.toString(), CrawlEvent.Status.OK.toString(), ds));
		return true;
	}

	public synchronized boolean removeDataSource(DataSourceId id, boolean force) {
		if (!datasources.containsKey(id)) {
			fireListeners(new CrawlEvent(CrawlEvent.Type.REMOVE.toString(), CrawlEvent.Status.FAIL.toString(),
					"doesn't exist", id));
			return false;
		}
		DataSource ds = (DataSource) datasources.get(id);
		CrawlId cid = new CrawlId(id);
		if ((!force) && (cc.jobIsActive(cid))) {
			fireListeners(new CrawlEvent(CrawlEvent.Type.REMOVE.toString(), CrawlEvent.Status.FAIL.toString(),
					"jobs active, force=false", ds));
			return false;
		}
		try {
			if (cc.jobExists(cid)) {
				cc.abortJob(cid);
				cc.waitJob(cid, 30000);
				cc.removeJob(cid);
			}
		} catch (Exception e) {
			LOG.info("Failed removing job " + cid + ": " + e.toString());
			fireListeners(new CrawlEvent(CrawlEvent.Type.REMOVE.toString(), CrawlEvent.Status.EXCEPTION.toString(),
					e.toString(), ds));
			return false;
		}
		datasources.remove(id);
		fireListeners(new CrawlEvent(CrawlEvent.Type.REMOVE.toString(), CrawlEvent.Status.OK.toString(), ds));
		return true;
	}

	public synchronized List<DataSourceId> removeDataSources(String collection, boolean force) {
		List<DataSource> failed = new ArrayList<DataSource>();
		List<DataSource> remove = new ArrayList<DataSource>();

		cc.setClosing(collection, true);
		for (Map.Entry<DataSourceId, DataSource> e : datasources.entrySet()) {
			DataSource ds = (DataSource) e.getValue();
			if ((collection == null) || (collection.equals(ds.getCollection()))) {
				CrawlId cid = new CrawlId((DataSourceId) e.getKey());
				if (cc.jobIsActive(cid)) {
					if (force)
						try {
							if (cc.jobExists(cid)) {
								cc.abortJob(cid);
								cc.waitJob(cid, 30000);
								cc.removeJob(cid);
							}
							remove.add(ds);
						} catch (Exception ex) {
							LOG.info("Failed removing job " + cid + ": " + ex.toString());
							failed.add(ds);
						}
					else
						failed.add(ds);
				} else
					remove.add(ds);
			}
		}
		if (failed.isEmpty()) {
			for (DataSource ds : remove) {
				datasources.remove(ds.getDataSourceId());
			}
			fireListeners(new CrawlEvent(CrawlEvent.Type.REMOVE_MULTI.toString(), CrawlEvent.Status.OK.toString(),
					"collection=" + collection, remove));
		} else {
			fireListeners(new CrawlEvent(CrawlEvent.Type.REMOVE_MULTI.toString(), CrawlEvent.Status.FAIL.toString(),
					"collection=" + collection, remove));
		}
		cc.setClosing(collection, false);

		List<DataSourceId> failedIds = new ArrayList<DataSourceId>();
		for (DataSource ds : failed) {
			failedIds.add(ds.getDataSourceId());
		}

		return failedIds;
	}

	public synchronized List<DataSource> getDataSources(String collection) {
		return getDataSources(null, collection);
	}

	public synchronized List<DataSource> getDataSources(String user, String collection) {
		List<DataSource> res = new ArrayList<DataSource>();
		for (Map.Entry<DataSourceId, DataSource> e : datasources.entrySet())
			if (((collection == null) || (collection.equals(e.getValue().getCollection())))
					&& ((user == null) || (user.equals(e.getKey().getUser())))) {
				res.add((DataSource) DeepCopy.copy(e.getValue()));
			}
		return res;
	}

	public synchronized List<DataSourceId> listDataSources(String collection) {
		return listDataSources(null, collection);
	}

	public synchronized List<DataSourceId> listDataSources(String user, String collection) {
		List<DataSourceId> res = new ArrayList<DataSourceId>();
		for (Map.Entry<DataSourceId, DataSource> e : datasources.entrySet())
			if (((collection == null) || (collection.equals(((DataSource) e.getValue()).getCollection())))
					&& ((user == null) || (user.equals(((DataSourceId) e.getKey()).getUser())))) {
				res.add(((DataSource) e.getValue()).getDataSourceId());
			}
		return res;
	}
}
