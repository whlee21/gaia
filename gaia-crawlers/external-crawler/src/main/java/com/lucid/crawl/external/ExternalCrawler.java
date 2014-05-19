 package gaia.crawl.external;
 
 import gaia.crawl.CrawlProcessor;
 import gaia.crawl.CrawlState;
 import gaia.crawl.CrawlStatus;
 import gaia.crawl.CrawlStatus.JobState;
 import gaia.crawl.CrawlerController;
 import gaia.crawl.UpdateController;
 import gaia.crawl.datasource.DataSource;
 import java.io.IOException;
 import java.net.HttpURLConnection;
 import java.net.URL;
 import org.apache.commons.lang.StringUtils;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
 
 public class ExternalCrawler
 {
   private static transient Logger log = LoggerFactory.getLogger(ExternalCrawler.class);
   private DataSource ds;
   private CrawlState crawlState;
   private CrawlerController cc;
   private UpdateController update;
 
   public ExternalCrawler(CrawlerController cc, DataSource ds, CrawlState crawlState)
   {
     this.cc = cc;
     this.ds = ds;
     this.crawlState = crawlState;
   }
 
   public void crawl(boolean runCallback) {
     if (this.crawlState.getProcessor() != null)
       this.update = this.crawlState.getProcessor().getUpdateController();
     else {
       try {
         this.update = UpdateController.create(this.cc, this.ds);
       } catch (Exception e) {
         log.warn("Exception creating UpdateController: " + e.toString());
         this.crawlState.getStatus().failed(e);
         return;
       }
 
     }
 
     finishUpdateController();
     try
     {
       this.update.start();
     } catch (Exception e) {
       try {
         this.update.finish(false);
       } catch (Exception e1) {
         log.warn("Exception finishing UpdateController: " + e1.toString());
       }
       this.crawlState.getStatus().failed(e);
       return;
     }
     this.crawlState.getStatus().running();
 
     String callback = (String)this.ds.getProperty("callback");
     if ((!StringUtils.isBlank(callback)) && (runCallback)) {
       URL u = null;
       try {
         u = new URL(callback);
       } catch (Exception e) {
         log.warn("Invalid callback URL for ds=" + this.ds.getDataSourceId() + ", callback will be ignored: " + e.toString());
       }
 
       if (u != null) {
         if (!u.getProtocol().toLowerCase().startsWith("http"))
           log.warn("Unsupported callback protocol: " + u.getProtocol() + " for ds=" + this.ds.getDataSourceId() + ", callback will be ignored.");
         else {
           try
           {
             HttpURLConnection conn = (HttpURLConnection)u.openConnection();
             conn.setRequestMethod("GET");
             conn.setInstanceFollowRedirects(true);
             conn.setConnectTimeout(5000);
             conn.setReadTimeout(1000);
             conn.connect();
             log.info("Callback for ds=" + this.ds.getDataSourceId() + " succeeded with status " + conn.getResponseMessage());
 
             conn.disconnect();
             conn = null;
           } catch (IOException e) {
             log.warn("Callback for ds=" + this.ds.getDataSourceId() + " to " + u + " failed: " + e.toString());
           }
         }
       }
     }
 
     CrawlStatus.JobState endState = CrawlStatus.JobState.RUNNING;
 
     if (this.crawlState.getStatus().getState() == CrawlStatus.JobState.STOPPING)
       endState = CrawlStatus.JobState.STOPPED;
     else if (this.crawlState.getStatus().getState() == CrawlStatus.JobState.ABORTING)
       endState = CrawlStatus.JobState.ABORTED;
   }
 
   private void finishUpdateController()
   {
     boolean commit = this.ds.getBoolean("commit_on_finish", true);
     try {
       if (this.update.isStarted())
         this.update.finish(commit);
     }
     catch (IOException e) {
       log.warn("Exception finishing UpdateController for ds=" + this.ds.getDataSourceId() + ": " + e.toString());
     }
   }
 
   public void stop() {
     finishUpdateController();
     if (this.crawlState.getStatus().isRunning())
       this.crawlState.getStatus().end(CrawlStatus.JobState.STOPPED);
   }
 }

