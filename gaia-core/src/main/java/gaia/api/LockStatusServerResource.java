 package gaia.api;
 
 import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.restlet.resource.Get;
import org.restlet.resource.ServerResource;
 
 public class LockStatusServerResource extends ServerResource
   implements LockStatusResource
 {
//   protected static final Settings settings = (Settings)LWEGuiceServletConfig.injectorHack().getInstance(Settings.class);
 
   @Get("json")
   public Map<String, Object> check() throws IOException
   {
     Map<String, Object> response = new HashMap<String, Object>();
//     boolean locked = settings.getBoolean(Settings.Group.control, "blockUpdates");
//     response.put("locked", Boolean.valueOf(locked));
//     if (locked) {
//       String reason = settings.getString(Settings.Group.control, "blockUpdatesReason");
// 
//       response.put("reason", reason != null ? reason : "Unknown.");
//     }
     return response;
   }
 }

