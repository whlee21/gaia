package gaia.crawl;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import gaia.crawl.datasource.DataSource;
import gaia.crawl.datasource.DataSourceId;
import gaia.yaml.YamlBean;

@Singleton
public class YamlDataSourceManager extends YamlBean implements DataSourceManager {
	private static final Logger LOG = LoggerFactory.getLogger(YamlDataSourceManager.class);
	List<DataSource> datasources;
	private transient boolean beforeInitialLoad = true;

	public YamlDataSourceManager() {
	}

	@Inject
	public YamlDataSourceManager(@Named("datasources-filename") String filename) {
		this(filename, false);
	}

	public YamlDataSourceManager(String filename, boolean ignoreExistingContents) {
		super(filename, ignoreExistingContents);
		LOG.info("Using " + filename + " to persist datasources.");
	}

	public int initialLoad(ConnectorManager cm) throws Exception {
		int count = 0;
		Set<DataSourceId> updated = new HashSet<DataSourceId>();
		for (DataSource ds : datasources) {
			if (!cm.isAvailable(ds.getCrawlerType())) {
				LOG.error("REMOVING DATASOURCE " + ds.getDataSourceId() + " (" + ds.getDisplayName()
						+ ") using unavailable crawler " + ds.getCrawlerType());

				updated.add(ds.getDataSourceId());
			} else if (cm.getCrawlerSpec(ds.getCrawlerType(), ds.getType()) == null) {
				LOG.error("REMOVING DATASOURCE " + ds.getDataSourceId() + " (" + ds.getDisplayName()
						+ ") using unavailable data source type  " + ds.getCrawlerType() + "/" + ds.getType());

				updated.add(ds.getDataSourceId());
			} else {
				DataSource newDs = cm.validateDataSource(ds, true, false);

				Set<String> keys = new HashSet<String>(ds.getProperties().keySet());
				for (String key : keys) {
					if (ds.getProperty(key) != null) {
						if (newDs.getProperty(key) == null) {
							LOG.info("Removing obsolete property in ds=" + ds.getDataSourceId() + " (" + ds.getDisplayName()
									+ ") type " + ds.getCrawlerType() + "/" + ds.getType() + "/" + key);
						} else if (!ds.getProperty(key).getClass().equals(newDs.getProperty(key).getClass())) {
							LOG.info("Updating property in ds=" + ds.getDataSourceId() + " (" + ds.getDisplayName() + ") type "
									+ ds.getCrawlerType() + "/" + ds.getType() + "/" + key + " to type "
									+ newDs.getProperty(key).getClass().getSimpleName());
						}

					}

				}

				if (!ds.equals(newDs)) {
					updated.add(ds.getDataSourceId());
				}

				if (cm.exists(ds.getDataSourceId())) {
					if (!ds.equals(newDs)) {
						LOG.info("UPDATING DATASOURCE " + ds.getDataSourceId());
						cm.updateDataSource(newDs);
					} else {
						LOG.info("DataSource id " + ds.getDataSourceId() + " already registered and valid.");
					}
					count++;
				} else {
					try {
						cm.addDataSource(newDs);
						count++;
					} catch (Exception e1) {
						LOG.warn("Failed to update datasource " + ds.getDataSourceId() + ": " + e1.toString());
					}
				}
			}
		}
		if (updated.size() > 0) {
			LOG.info("Updated existing datasources to the current version: " + updated);
			syncSave(cm);
		}
		beforeInitialLoad = false;
		return count;
	}

	public synchronized void syncSave(ConnectorManager cm) throws Exception {
		if (beforeInitialLoad) {
			LOG.debug("syncSave called before initial load, ignoring ...");
			return;
		}
		Map<DataSourceId, DataSource> dss = new HashMap<DataSourceId, DataSource>();

		for (DataSource ds : cm.getDataSources(null)) {
			dss.put(ds.getDataSourceId(), ds);
		}
		datasources.clear();
		datasources.addAll(dss.values());
		save();
	}

	public List<DataSource> getDataSources() {
		return datasources;
	}

	protected void load(YamlBean yamlBean) {
		YamlDataSourceManager dm = (YamlDataSourceManager) yamlBean;
		datasources = dm.datasources;
	}

	protected void init() {
		datasources = new LinkedList<DataSource>();
	}

	public synchronized void reload(ConnectorManager cm) throws Exception {
		LOG.info("reloading all datasources from " + file);
		datasources.clear();
		construct(file, false, location);
		initialLoad(cm);
	}
}
