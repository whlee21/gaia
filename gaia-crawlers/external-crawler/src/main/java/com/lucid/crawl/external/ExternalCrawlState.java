 package gaia.crawl.external;
 
 import gaia.crawl.CrawlProcessor;
 import gaia.crawl.CrawlState;
 import gaia.crawl.HistoryRecorder;
 import gaia.crawl.datasource.DataSource;
 
 public class ExternalCrawlState extends CrawlState
 {
   ExternalCrawler crawler;
 
   public synchronized void init(DataSource ds, CrawlProcessor processor, HistoryRecorder historyRecorder)
     throws Exception
   {
     super.init(ds, processor, historyRecorder);
   }
 }

