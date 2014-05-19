 package gaia.crawl.external;
 
 import gaia.crawl.CrawlerController;
 import gaia.crawl.DataSourceFactory;
 import gaia.crawl.datasource.DataSourceSpec.Type;
 import java.util.Map;
 
 public class ExternalDataSourceFactory extends DataSourceFactory
 {
   public ExternalDataSourceFactory(CrawlerController cc)
   {
     super(cc);
     this.types.put(DataSourceSpec.Type.external.toString(), new ExternalSpec());
   }
 }

