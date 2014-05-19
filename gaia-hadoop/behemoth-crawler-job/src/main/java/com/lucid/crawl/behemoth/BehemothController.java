 package com.lucid.crawl.behemoth;
 
 import com.lucid.crawl.CrawlId;
 import com.lucid.crawl.CrawlProcessor;
 import com.lucid.crawl.CrawlStateManager;
 import com.lucid.crawl.CrawlStatus;
 import com.lucid.crawl.CrawlStatus.JobState;
 import com.lucid.crawl.CrawlerController;
 import com.lucid.crawl.DataSourceFactory;
 import com.lucid.crawl.batch.BatchManager;
 import com.lucid.crawl.datasource.DataSource;
 import com.lucid.crawl.datasource.DataSourceId;
 
 public class BehemothController extends CrawlerController
 {
   protected DataSourceFactory dsFactory;
 
   public BehemothController()
   {
     this.batchMgr = BatchManager.create("lucid.map.reduce.hdfs", getClass().getClassLoader());
     this.dsFactory = new BehemothDataSourceFactory(this);
   }
 
   public DataSourceFactory getDataSourceFactory()
   {
     return this.dsFactory;
   }
 
   public void reset(String collection, DataSourceId dsId)
     throws Exception
   {
   }
 
   public void resetAll(String collection)
     throws Exception
   {
   }
 
   public CrawlId defineJob(DataSource ds, CrawlProcessor processor) throws Exception
   {
     CrawlId result = new CrawlId(ds.getDataSourceId());
     BehemothCrawlState state = new BehemothCrawlState();
     state.init(ds, processor, this.historyRecorder);
     this.jobStateMgr.add(state);
     return result;
   }
 
   public void startJob(CrawlId descrId) throws Exception
   {
     BehemothCrawlState state = (BehemothCrawlState)this.jobStateMgr.get(descrId);
     if (state == null) {
       throw new Exception("Unknown job id: " + descrId);
     }
     assureJobNotRunning(state);
     assureNotClosing(state.getDataSource().getCollection());
     refreshDatasource(state);
     state.start();
   }
 
   public void stopJob(CrawlId jobId)
     throws Exception
   {
     BehemothCrawlState state = (BehemothCrawlState)this.jobStateMgr.get(jobId);
     if (state == null) {
       throw new Exception("Unknown job id: " + jobId);
     }
 
     if (state.getStatus().getState() != CrawlStatus.JobState.RUNNING) {
       return;
     }
 
     state.stop();
   }
 
   public void abortJob(CrawlId id) throws Exception
   {
     stopJob(id);
   }
 }

