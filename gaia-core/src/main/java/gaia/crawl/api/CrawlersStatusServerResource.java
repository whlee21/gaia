 package gaia.crawl.api;
 
 import gaia.crawl.ConnectorManager;

import java.util.Map;

import org.restlet.resource.ResourceException;
import org.restlet.resource.ServerResource;

import com.google.inject.Inject;
 
 public class CrawlersStatusServerResource extends ServerResource
   implements CrawlersStatusResource
 {
   ConnectorManager cm;
 
   @Inject
   public CrawlersStatusServerResource(ConnectorManager cm)
   {
     this.cm = cm;
   }
 
   public Map<String, Object> getCrawlersStatus() throws ResourceException
   {
     return cm.getConnectorStatus().toMap();
   }
 }

