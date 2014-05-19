 package com.lucid.crawl.behemoth;
 
 import com.digitalpebble.behemoth.util.CorpusGenerator.Counters;
 import com.lucid.crawl.CrawlState;
 import com.lucid.crawl.CrawlStatus;
 import com.lucid.crawl.CrawlStatus.Counter;
 import com.lucid.crawl.datasource.DataSource;
 import java.io.File;
 import java.io.FilenameFilter;
 import java.io.IOException;
 import java.net.URI;
 import org.apache.hadoop.conf.Configuration;
 import org.apache.hadoop.mapred.Counters;
 import org.apache.hadoop.mapred.Counters.Counter;
 import org.apache.hadoop.mapred.Counters.Group;
 import org.apache.hadoop.mapred.InputSplit;
 import org.apache.hadoop.mapred.Reporter;
 import org.apache.hadoop.mapred.RunningJob;
 import org.apache.hadoop.mapred.Task.Counter;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
 
 public class BehemothCrawlState extends CrawlState
   implements Reporter
 {
   private static final Logger LOG = LoggerFactory.getLogger(BehemothCrawlState.class);
 
   BehemothCrawler crawler = null;
   RunningJob currentJob = null;
   protected String reporterStatus;
   long seqFileRecords;
   long tikaRecords;
   long indexRecords;
   boolean directAccess;
   TASK currentTask = TASK.NONE;
   Thread t = null;
   volatile Counters counters = new Counters();
 
   public synchronized void start() throws Exception {
     if ((this.t != null) && (this.t.isAlive())) {
       throw new Exception("already running");
     }
     this.crawler = new BehemothCrawler(this);
     this.status.reset();
     this.counters = new Counters();
     this.currentJob = null;
     this.currentTask = TASK.NONE;
     this.t = new Thread(this.crawler);
     this.t.setDaemon(true);
     this.t.start();
   }
 
   public synchronized void setDataSource(DataSource ds) throws Exception
   {
     if ((this.t != null) && (this.t.isAlive())) {
       throw new Exception("job is running");
     }
     this.t = null;
     this.crawler = new BehemothCrawler(this);
     this.counters = new Counters();
     this.status.reset();
     this.directAccess = ds.getBoolean("direct_access");
     super.setDataSource(ds);
   }
 
   public synchronized void stop() throws Exception {
     if ((this.t == null) || (!this.t.isAlive()) || (this.crawler == null)) {
       throw new Exception("job is not running");
     }
     this.crawler.stop();
   }
 
   public CrawlStatus getStatus()
   {
     this.seqFileRecords = this.counters.getCounter(CorpusGenerator.Counters.DOC_COUNT);
     switch (2.$SwitchMap$com$lucid$crawl$behemoth$BehemothCrawlState$TASK[this.currentTask.ordinal()]) {
     case 1:
       this.status.setCounter(CrawlStatus.Counter.New, this.seqFileRecords);
       if (this.directAccess)
         this.status.setMessage("Preparing a list of files (" + this.seqFileRecords + " docs)");
       else {
         this.status.setMessage("Converting to sequence files (" + this.seqFileRecords + " docs)");
       }
       break;
     case 2:
       if (this.currentJob != null) {
         this.status.setMessage("Extracting content.  Hadoop ID: " + this.currentJob.getID());
         Counters.Group g = null;
         try {
           g = this.currentJob.getCounters().getGroup("TIKA");
           long newCnt = g.getCounter("DOC-PARSED");
           newCnt += g.getCounter("TEXT ALREADY AVAILABLE");
           long failedCnt = g.getCounter("PARSING_ERROR");
           failedCnt += g.getCounter("DOC-NO_DATA");
           long filteredCnt = g.getCounter("FILTERED-CONTENT-LENGTH");
           filteredCnt += g.getCounter("FILTERED-CONTENT-TYPE");
           this.status.setCounter(CrawlStatus.Counter.New, newCnt);
           this.status.setCounter(CrawlStatus.Counter.Failed, failedCnt);
           this.status.setCounter(CrawlStatus.Counter.Filter_Denied, filteredCnt);
         } catch (Exception e) {
           LOG.warn("Unable to obtain counters from the running job " + this.currentJob.getTrackingURL(), e);
         }
       }
       break;
     case 3:
       if (this.currentJob != null) {
         long eligible = this.status.getCounter(CrawlStatus.Counter.New) + this.status.getCounter(CrawlStatus.Counter.Failed);
         try {
           Counters cnt = this.currentJob.getCounters();
           long newCnt = cnt.getCounter(Task.Counter.MAP_OUTPUT_RECORDS);
           this.status.setMessage("Indexing to LucidWorks (" + newCnt + " of " + eligible + ").  Hadoop ID: " + this.currentJob.getID());
         } catch (Exception e) {
           LOG.warn("Unable to obtain counters from the running job " + this.currentJob.getTrackingURL(), e);
           this.status.setMessage("Indexing to LucidWorks (? of " + eligible + ").  Hadoop ID: " + this.currentJob.getID());
         }
       }
       break;
     case 4:
     }
 
     return this.status;
   }
 
   public void setStatus(String s)
   {
     this.reporterStatus = s;
   }
 
   public Counters.Counter getCounter(Enum<?> anEnum)
   {
     return this.counters.findCounter(anEnum);
   }
 
   public Counters.Counter getCounter(String s, String s1)
   {
     return this.counters.findCounter(s, s1);
   }
 
   public void incrCounter(Enum<?> anEnum, long l)
   {
     this.counters.incrCounter(anEnum, l);
   }
 
   public void incrCounter(String s, String s1, long l)
   {
     this.counters.incrCounter(s, s1, l);
   }
 
   public InputSplit getInputSplit() throws UnsupportedOperationException
   {
     throw new UnsupportedOperationException();
   }
 
   public void progress()
   {
   }
 
   public Configuration createConfig(BehemothCrawler behemothCrawler)
     throws Exception
   {
     ClassLoader ctxCl = Thread.currentThread().getContextClassLoader();
     try {
       Thread.currentThread().setContextClassLoader(behemothCrawler.getClass().getClassLoader());
 
       Configuration res = new Configuration();
       res.setClassLoader(behemothCrawler.getClass().getClassLoader());
       String hadoopConfDir = behemothCrawler.dataSource.getString("hadoop_conf");
       String jobTracker;
       if (hadoopConfDir != null) {
         File confDir = new File(hadoopConfDir);
         if ((confDir.exists()) && (confDir.isDirectory())) {
           File[] files = confDir.listFiles(new FilenameFilter()
           {
             public boolean accept(File file, String name) {
               return (name.equals("core-site.xml")) || (name.equals("mapred-site.xml")) || (name.equals("hdfs-site.xml"));
             }
           });
           for (int i = 0; i < files.length; i++) {
             File file = files[i];
             log.info("Adding Hadoop resource: " + file.getAbsolutePath());
             res.addResource(file.toURI().toURL());
           }
         } else {
           throw new IOException("Couldn't find Hadoop configuration directory");
         }
       }
       else
       {
         jobTracker = behemothCrawler.dataSource.getString("job_tracker");
         if (jobTracker != null) {
           res.set("mapred.job.tracker", jobTracker);
         }
         String fsName = behemothCrawler.dataSource.getString("fs_default_name");
         if (fsName != null) {
           res.set("fs.default.name", fsName);
         }
       }
       return res;
     } finally {
       Thread.currentThread().setContextClassLoader(ctxCl);
     }
   }
 
   public static enum TASK
   {
     NONE, 
     SEQ_FILE, 
     TIKA, 
     INDEX;
   }
 }

