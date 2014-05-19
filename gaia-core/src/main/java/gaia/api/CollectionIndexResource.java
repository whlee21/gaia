 package gaia.api;
 
 import gaia.admin.collection.AdminScheduler;
import gaia.crawl.ConnectorManager;

import org.apache.solr.core.CoreContainer;
import org.restlet.data.Parameter;
import org.restlet.data.Status;
import org.restlet.resource.Delete;
import org.restlet.resource.ServerResource;

import com.google.inject.Inject;
 
 public class CollectionIndexResource extends ServerResource
 {
   private String collection;
   private AdminScheduler adminScheduler;
   private CoreContainer cores;
   private ConnectorManager crawlerManager;
 
   @Inject
   public CollectionIndexResource(CoreContainer cores, AdminScheduler adminScheduler, ConnectorManager crawlerManager)
   {
     this.cores = cores;
     this.adminScheduler = adminScheduler;
     this.crawlerManager = crawlerManager;
   }
 
   public void doInit()
   {
     this.collection = ((String)getRequestAttributes().get("coll_name"));
   }
 
   @Delete("json")
   public void deleteIndex() throws Exception {
     Parameter p = (Parameter)getQuery().getFirst("key");
     String key = p == null ? null : p.getValue();
     if (!"iaccepttherisk".equals(key)) {
       setStatus(Status.CLIENT_ERROR_FORBIDDEN, "you have not entered the correct query paramater: key=iaccepttherisk");
       return;
     }
 
     this.adminScheduler.stopAndRemoveAllSchedules(this.collection);
 
     LweSolrServer solrServer = new LweSolrServer(this.cores, this.collection);
 
     solrServer.deleteByQuery("*:*");
     solrServer.commit(false, true);
 
     this.crawlerManager.resetAll(null, this.collection);
     setStatus(Status.SUCCESS_NO_CONTENT);
   }
 }

