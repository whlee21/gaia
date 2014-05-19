package gaia.api;

import java.io.IOException;
import java.util.Map;
import org.restlet.resource.Get;

public interface LockStatusResource
{
  @Get("json")
  public  Map<String, Object> check()
    throws IOException;
}

