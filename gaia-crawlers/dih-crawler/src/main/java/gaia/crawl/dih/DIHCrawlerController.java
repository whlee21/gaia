package gaia.crawl.dih;

import com.google.inject.Inject;
import gaia.Constants;
import gaia.crawl.CrawlEvent;
import gaia.crawl.CrawlEvent.Status;
import gaia.crawl.CrawlEvent.Type;
import gaia.crawl.CrawlId;
import gaia.crawl.CrawlListener;
import gaia.crawl.CrawlProcessor;
import gaia.crawl.CrawlStateManager;
import gaia.crawl.CrawlerController;
import gaia.crawl.DataSourceFactory;
import gaia.crawl.DataSourceRegistry;
import gaia.crawl.batch.BatchManager;
import gaia.crawl.datasource.DataSource;
import gaia.crawl.datasource.DataSourceId;
import java.io.File;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DIHCrawlerController extends CrawlerController {
	private static transient Logger LOGGER = LoggerFactory.getLogger(DIHCrawlerController.class);

	public static String DATA_DIR = Constants.GAIA_DATA_HOME + File.separator + "gaia.jdbc";
	public static String JARS_DIR = DATA_DIR + File.separator + "jdbc";
	public static String DS_DIR = DATA_DIR + File.separator + "datasources";

	private DataSourceFactory dsFactory = new DIHDataSourceFactory(this);

	@Inject
	public DIHCrawlerController() {
		batchMgr = BatchManager.create("gaia.jdbc", getClass().getClassLoader());
		resourceMgr = new JdbcDriversResourceManager(new File(JARS_DIR));

		dsRegistry.addListener(new CrawlListener() {
			public void processEvent(CrawlEvent event) {
				if (!CrawlEvent.Status.OK.toString().equals(event.getStatus())) {
					return;
				}
				if (CrawlEvent.Type.REMOVE.toString().equals(event.getType())) {
					DataSource ds = (DataSource) event.getSource();
					File dir = new File(DIHCrawlerController.DS_DIR, ds.getDataSourceId().getId());
					new DIHConfigurationManager(dir).removeConfigs();
					return;
				}

				if (CrawlEvent.Type.REMOVE_MULTI.toString().equals(event.getType())) {
					List<DataSource> dses = (List) event.getSource();
					for (DataSource ds : dses) {
						File dir = new File(DIHCrawlerController.DS_DIR, ds.getDataSourceId().getId());
						new DIHConfigurationManager(dir).removeConfigs();
					}
				}
			}
		});
	}

	public DataSourceFactory getDataSourceFactory() {
		return dsFactory;
	}

	public void reset(String collection, DataSourceId dsId) {
		File dir = new File(DS_DIR, dsId.getId());
		new DIHConfigurationManager(dir).removeDihPropFile(dsId);
	}

	public void resetAll(String collection) {
		List<DataSource> datasources = dsRegistry.getDataSources(collection);
		for (DataSource dataSource : datasources)
			if (dsFactory.getDataSourceSpecs().containsKey(dataSource.getClass().getName()))
				reset(collection, dataSource.getDataSourceId());
	}

	public CrawlId defineJob(DataSource ds, CrawlProcessor processor) throws Exception {
		assureNotClosing(ds.getCollection());
		DIHCrawlState state = new DIHCrawlState();
		state.init(ds, processor, historyRecorder);
		jobStateMgr.add(state);
		return state.getId();
	}

	public void startJob(CrawlId descrId) throws Exception {
		DIHCrawlState state = (DIHCrawlState) jobStateMgr.get(descrId);
		if (state == null) {
			throw new Exception("Unknown job id: " + descrId);
		}
		assureJobNotRunning(state);
		assureNotClosing(state.getDataSource().getCollection());

		refreshDatasource(state);

		state.crawl();
	}

	public void stopJob(CrawlId id) throws Exception {
		DIHCrawlState state = (DIHCrawlState) jobStateMgr.get(id);
		if (state == null) {
			throw new Exception("Unknown job id: " + id);
		}
		state.stop();
	}

	public void abortJob(CrawlId id) throws Exception {
		stopJob(id);
	}
}
