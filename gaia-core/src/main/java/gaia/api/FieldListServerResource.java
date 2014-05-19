 package gaia.api;
 
 import gaia.utils.SolrTools;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.solr.core.CoreContainer;
import org.apache.solr.core.SolrCore;
import org.apache.solr.search.SolrIndexSearcher;
import org.apache.solr.util.RefCounted;
import org.restlet.resource.Get;
import org.restlet.resource.ServerResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
 
 public class FieldListServerResource extends ServerResource
   implements FieldListResource
 {
   private static transient Logger LOG = LoggerFactory.getLogger(FieldListServerResource.class);
   private CoreContainer cores;
 
   @Inject
   public FieldListServerResource(CoreContainer cores)
   {
     this.cores = cores;
   }
 
   @Get("json")
   public List<String> retrieve()
   {
     List<String> json = new ArrayList<String>();
     String collection = (String)getRequestAttributes().get("coll_name");
     SolrCore solrCore = cores.getCore(collection);
     try
     {
       RefCounted<SolrIndexSearcher> searcher = solrCore.getNewestSearcher(true);
       try {
         json.addAll(SolrTools.getAllFieldNames(((SolrIndexSearcher)searcher.get()).getIndexReader()));
       }
       finally {
       }
       for (String fieldName : solrCore.getLatestSchema().getFields().keySet()) {
         if (!json.contains(fieldName)) {
           json.add(fieldName);
         }
       }
       Collections.sort(json, String.CASE_INSENSITIVE_ORDER);
     } finally {
       solrCore.close();
     }
     return json;
   }
 }

