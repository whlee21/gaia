 package gaia.crawl.aperture;
 
 import gaia.crawl.CrawlerUtils;
import gaia.crawl.datasource.DataSource;
import gaia.crawl.datasource.DataSourceSpec;
import gaia.crawl.datasource.DataSourceUtils;

import java.io.File;
import java.net.URI;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
 
 public class ApertureDataSourceUtil
 {
   private static final Logger LOG = LoggerFactory.getLogger(ApertureDataSourceUtil.class);
 
   public static String getDomainPattern(DataSource ds) {
     if (ds.getType().equals(DataSourceSpec.Type.file.toString())) {
       String f = DataSourceUtils.getPath(ds);
       if ((f == null) || (f.trim().isEmpty())) {
         return null;
       }
       File file = CrawlerUtils.resolveRelativePath(f);
       String uri = null;
       uri = file.toURI().toString();
       try {
         URI u = new URI(uri);
         return u.getRawPath();
       }
       catch (Exception e) {
         LOG.warn("Can't parse uri: " + e);
         return f;
       }
     }
     return DataSourceUtils.getSource(ds);
   }
 }

