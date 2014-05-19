 package gaia.api;
 
 import gaia.utils.MasterConfUtil;
 import java.io.IOException;
 import java.util.Map;
 import org.restlet.resource.Get;
 import org.restlet.resource.ServerResource;
 
 public class MasterConfServerResource extends ServerResource
   implements MasterConfResource
 {
   @Get("json")
   public Map<String, Object> retrieve()
     throws IOException
   {
     return MasterConfUtil.read();
   }
 }

