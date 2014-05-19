 package gaia.crawl.mongodb;
 
 import gaia.crawl.CrawlerController;
 import gaia.crawl.DataSourceFactory;
 import java.util.Map;
 
 public class MongoDBDataSourceFactory extends DataSourceFactory
 {
   protected MongoDBDataSourceFactory(CrawlerController cc)
   {
     super(cc);
     this.types.put("mongodb", new MongoDBItemSpec());
   }
 }

