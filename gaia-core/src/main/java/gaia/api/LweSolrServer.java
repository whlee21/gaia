 package gaia.api;
 
 import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
 import org.apache.solr.core.CoreContainer;
 
 public class LweSolrServer extends EmbeddedSolrServer
 {
   private static final long serialVersionUID = 1L;
 
   public LweSolrServer(CoreContainer coreContainer, String coreName)
   {
     super(coreContainer, getCoreName(coreContainer, coreName));
   }
 
   private static String getCoreName(CoreContainer coreContainer, String coreName) {
     String name = coreName;
     if (name.equals(coreContainer.getDefaultCoreName())) {
       name = "";
     }
     return name;
   }
 }

