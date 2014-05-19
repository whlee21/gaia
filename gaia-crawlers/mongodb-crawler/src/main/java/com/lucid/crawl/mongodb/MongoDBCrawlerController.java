 package gaia.crawl.mongodb;
 
 import gaia.crawl.CrawlId;
 import gaia.crawl.CrawlProcessor;
 import gaia.crawl.CrawlStateManager;
 import gaia.crawl.CrawlStatus;
 import gaia.crawl.CrawlerController;
 import gaia.crawl.DataSourceFactory;
 import gaia.crawl.batch.BatchManager;
 import gaia.crawl.datasource.DataSource;
 import gaia.crawl.datasource.DataSourceId;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
 
 public class MongoDBCrawlerController extends CrawlerController
 {
   private static final Logger log = LoggerFactory.getLogger(MongoDBCrawlerController.class);
   private DataSourceFactory dataSourceFactory;
 
   public MongoDBCrawlerController()
   {
     this.batchMgr = BatchManager.create("gaia.mongodb", getClass().getClassLoader());
 
     this.dataSourceFactory = new MongoDBDataSourceFactory(this);
   }
 
   public DataSourceFactory getDataSourceFactory()
   {
     return this.dataSourceFactory;
   }
 
   public BatchManager getBatchManager()
   {
     return this.batchMgr;
   }
 
   public void reset(String collection, DataSourceId dsId)
   {
   }
 
   public void resetAll(String collection)
   {
   }
 
   public CrawlId defineJob(DataSource dataSource, CrawlProcessor processor)
     throws Exception
   {
     assureNotClosing(dataSource.getCollection());
     MongoDBCrawlState state = new MongoDBCrawlState();
     state.init(dataSource, processor, this.historyRecorder);
     this.jobStateMgr.add(state);
     return state.getId();
   }
 
   public void startJob(CrawlId jobId) throws Exception
   {
     MongoDBCrawlState state = (MongoDBCrawlState)this.jobStateMgr.get(jobId);
     if (state == null) {
       throw new Exception("Unknown job id: " + jobId);
     }
     assureJobNotRunning(state);
     assureNotClosing(state.getDataSource().getCollection());
 
     refreshDatasource(state);
 
     state.start();
   }
 
   public void stopJob(CrawlId jobId) throws Exception
   {
     MongoDBCrawlState state = (MongoDBCrawlState)this.jobStateMgr.get(jobId);
     if (state == null) {
       throw new Exception("unknown crawl id " + jobId);
     }
     if (!state.getStatus().isRunning()) {
       log.warn("not running");
       return;
     }
     state.stop();
   }
 
   public void abortJob(CrawlId jobId) throws Exception
   {
     MongoDBCrawlState state = (MongoDBCrawlState)this.jobStateMgr.get(jobId);
     if (state == null) {
       throw new Exception("unknown crawl id " + jobId);
     }
     if (!state.getStatus().isRunning()) {
       log.warn("not running");
       return;
     }
     state.abort();
   }
 }

