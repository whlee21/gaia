package gaia.crawl.fs;

import gaia.crawl.CrawlEvent;
import gaia.crawl.CrawlEvent.Status;
import gaia.crawl.CrawlEvent.Type;
import gaia.crawl.CrawlId;
import gaia.crawl.CrawlListener;
import gaia.crawl.CrawlProcessor;
import gaia.crawl.CrawlState;
import gaia.crawl.CrawlStateManager;
import gaia.crawl.CrawlStatus;
import gaia.crawl.CrawlerController;
import gaia.crawl.DataSourceFactory;
import gaia.crawl.DataSourceRegistry;
import gaia.crawl.batch.BatchManager;
import gaia.crawl.datasource.DataSource;
import gaia.crawl.datasource.DataSourceId;
import gaia.crawl.fs.ds.CIFSFSSpec;
import gaia.crawl.security.SecurityFilter;
import gaia.security.WindowsACLQueryFilterer;
import gaia.security.ad.ADHelper;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.naming.NamingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FSCrawlerController extends CrawlerController {
	private static final Logger LOG = LoggerFactory.getLogger(FSCrawlerController.class);
	protected DataSourceFactory dsFactory;
	private Map<DataSourceId, ADHelper> adHelpers = Collections.synchronizedMap(new HashMap<DataSourceId, ADHelper>());

	public FSCrawlerController() {
		initialize();
	}

	protected void initialize() {
		dsFactory = new FSDataSourceFactory(this);
		batchMgr = BatchManager.create("gaia.fs", getClass().getClassLoader());

		dsRegistry.addListener(new CrawlListener() {
			public void processEvent(CrawlEvent event) {
				if (!CrawlEvent.Status.OK.toString().equals(event.getStatus())) {
					return;
				}
				if (CrawlEvent.Type.REMOVE.toString().equals(event.getType())) {
					DataSource ds = (DataSource) event.getSource();
					adHelpers.remove(ds.getDataSourceId());
					return;
				}
				if (CrawlEvent.Type.REMOVE_MULTI.toString().equals(event.getType())) {
					List<DataSource> dses = (List) event.getSource();
					for (DataSource ds : dses) {
						adHelpers.remove(ds.getDataSourceId());
					}
					return;
				}

				DataSource ds = (DataSource) event.getSource();
				refreshAdHelper(ds);
			}
		});
	}

	public DataSourceFactory getDataSourceFactory() {
		return dsFactory;
	}

	public CrawlId defineJob(DataSource ds, CrawlProcessor processor) throws Exception {
		assureNotClosing(ds.getCollection());
		CrawlState state = new FSCrawlState(this);
		state.init(ds, processor, historyRecorder);
		jobStateMgr.add(state);
		return state.getId();
	}

	public void startJob(CrawlId id) throws Exception {
		final FSCrawlState state = (FSCrawlState) jobStateMgr.get(id);
		if (state == null) {
			throw new Exception("Unknown job id " + id);
		}
		assureJobNotRunning(state);
		assureNotClosing(state.getDataSource().getCollection());

		refreshDatasource(state);

		DataSource ds = dsRegistry.getDataSource(state.getDataSource().getDataSourceId());
		refreshAdHelper(ds);

		state.getStatus().starting();

		Thread t = new Thread("fsCrawl " + state.getDataSource().getDisplayName()) {
			public void run() {
				state.crawl();
			}
		};
		t.start();
	}

	private void refreshAdHelper(DataSource ds) {
		if (!ds.getType().equals(FSDataSourceFactory.Type.smb.toString())) {
			return;
		}

		if (ds.getString(CIFSFSSpec.AD_URL) == null) {
			adHelpers.remove(ds.getDataSourceId());
			return;
		}

		if (!ds.getBoolean(CIFSFSSpec.AD_CACHE_GROUPS, Boolean.FALSE.booleanValue())) {
			adHelpers.remove(ds.getDataSourceId());
			return;
		}

		ADHelper adHelper = CIFSFSSpec.getAdHelper(ds.getProperties());

		adHelper.readGroups();
		adHelpers.put(ds.getDataSourceId(), adHelper);
	}

	public void stopJob(CrawlId id) throws Exception {
		FSCrawlState state = (FSCrawlState) jobStateMgr.get(id);
		if (state == null) {
			throw new Exception("Unknown job id " + id);
		}
		state.stop();
	}

	public void abortJob(CrawlId id) throws Exception {
		FSCrawlState state = (FSCrawlState) jobStateMgr.get(id);
		if (state == null) {
			throw new Exception("Unknown job id " + id);
		}
		state.abort();
	}

	public void reset(String collection, DataSourceId dsId) throws Exception {
	}

	public void resetAll(String collection) throws Exception {
	}

	public SecurityFilter buildSecurityFilter(DataSource ds, String user) {
		if (!ds.getType().equals(FSDataSourceFactory.Type.smb.toString())) {
			return null;
		}
		if (ds.getString(CIFSFSSpec.AD_URL) == null) {
			return null;
		}

		ADHelper helper = null;
		if (ds.getBoolean(CIFSFSSpec.AD_CACHE_GROUPS, Boolean.FALSE.booleanValue())) {
			helper = (ADHelper) adHelpers.get(ds.getDataSourceId());
			if (helper == null) {
				LOG.warn("Could not find cached ADHelper for ds " + ds.getDataSourceId()
						+ ", switched to read AD groups at a runtime");
				helper = CIFSFSSpec.getAdHelper(ds.getProperties());
			}
		} else {
			helper = CIFSFSSpec.getAdHelper(ds.getProperties());
		}

		Set<String> tags = new HashSet<String>();
		if (user != null) {
			try {
				tags = helper.getSidsForUser(user);
				if (tags.size() > 0) {
					tags.add("S-1-1-0");
				}
			} catch (NamingException e) {
				LOG.error("Could not retrieve user data from ActiveDirectory: " + e.getMessage(), e);
			}
		}

		WindowsACLQueryFilterer filterer = new WindowsACLQueryFilterer();
		SecurityFilter result = filterer.buildFilter(tags, ds.getDataSourceId().getId());

		return result;
	}
}
