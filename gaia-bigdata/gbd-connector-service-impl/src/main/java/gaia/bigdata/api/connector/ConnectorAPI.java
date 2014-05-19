 package gaia.bigdata.api.connector;
 
 import gaia.bigdata.api.connector.gaiasearch.ConnectorServerResource;
import gaia.bigdata.api.connector.gaiasearch.ConnectorsServerResource;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import gaia.commons.api.API;
import gaia.commons.api.ResourceFinder;
import gaia.commons.services.ServiceLocator;
import gaia.commons.services.URIPayload;
import gaia.bigdata.services.ServiceType;

import org.restlet.security.Role;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
 
 @Singleton
 public class ConnectorAPI extends API
 {
   private static transient Logger log = LoggerFactory.getLogger(ConnectorAPI.class);
   private ServiceLocator serviceLocator;
 
   @Inject
   public ConnectorAPI(ResourceFinder finder, ServiceLocator serviceLocator)
   {
     super(finder);
     this.serviceLocator = serviceLocator;
   }
 
   protected void initAttachments()
   {
     attach("/{collection}", ConnectorsServerResource.class);
     attach("/{collection}/{id}", ConnectorServerResource.class);
     URIPayload gaia = serviceLocator.getServiceURI(ServiceType.GAIASEARCH.name());
     if ((gaia != null) && (gaia.uri != null)) {
       log.info("Attaching Gaia to " + gaia.uri);
       redir(gaia.uri, router, "/{collection}/{id}/schedule", "/collections/{collection}/datasources/{id}/schedule{rr}", new Role[0]);
       redir(gaia.uri, router, "/{collection}/{id}/job", "/collections/{collection}/datasources/{id}/job{rr}", new Role[0]);
       redir(gaia.uri, router, "/{collection}/{id}/status", "/collections/{collection}/datasources/{id}/status{rr}", new Role[0]);
       redir(gaia.uri, router, "/{collection}/{id}/history", "/collections/{collection}/datasources/{id}/history{rr}", new Role[0]);
       redir(gaia.uri, router, "/{collection}/{id}/crawldata", "/collections/{collection}/datasources/{id}/crawldata{rr}", new Role[0]);
       redir(gaia.uri, router, "/{collection}/{id}/index", "/collections/{collection}/datasources/{id}/index{rr}", new Role[0]);
     } else {
       log.warn("Unable to locate " + ServiceType.GAIASEARCH);
     }
   }
 
   public String getAPIRoot()
   {
     return "/connector";
   }
 
   public String getAPIName()
   {
     return ServiceType.CONNECTOR.name();
   }
 }

