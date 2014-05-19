 package gaia.api;
 
 import gaia.utils.VersionUtil;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.restlet.resource.Get;
import org.restlet.resource.ServerResource;
 
 public class VersionServerResource extends ServerResource
   implements VersionResource
 {
   private final Map<String, Object> info;
 
   public VersionServerResource()
   {
     Map<String, Object> tmp = new HashMap<String, Object>();
 
     tmp.put("lucidworks", VersionUtil.getGaiaWorksVersionInfo());
     tmp.put("solr", VersionUtil.getSolrVersionInfo());
 
     this.info = Collections.unmodifiableMap(tmp);
   }
 
   @Get("json")
   public Map<String, Object> list()
   {
     return this.info;
   }
 }

