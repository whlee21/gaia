 package com.lucid.crawl.behemoth;
 
 import com.lucid.crawl.CrawlerController;
 import com.lucid.crawl.DataSourceFactory;
 import java.util.Map;
 
 public class BehemothDataSourceFactory extends DataSourceFactory
 {
   public BehemothDataSourceFactory(CrawlerController cc)
   {
     super(cc);
     this.types.put("high_volume_hdfs", new BehemothAccessSpec());
   }
 }

