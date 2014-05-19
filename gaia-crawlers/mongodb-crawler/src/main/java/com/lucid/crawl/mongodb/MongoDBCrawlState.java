 package gaia.crawl.mongodb;
 
 import gaia.crawl.CrawlState;
 
 public class MongoDBCrawlState extends CrawlState
 {
   MongoDBCrawler crawler = null;
   Thread thread = null;
 
   public void start() throws Exception {
     if ((this.thread != null) && (this.thread.isAlive())) {
       throw new Exception("already running");
     }
     this.crawler = new MongoDBCrawler(this);
     this.thread = new Thread(this.crawler);
 
     this.thread.start();
   }
 
   public void stop() throws Exception {
     if ((this.thread == null) || (!this.thread.isAlive())) {
       throw new Exception("not running");
     }
     this.crawler.stop();
   }
 
   public void abort() throws Exception {
     if ((this.thread == null) || (!this.thread.isAlive())) {
       throw new Exception("not running");
     }
     this.crawler.abort();
   }
 }

