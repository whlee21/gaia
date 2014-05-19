package gaia.api;

import java.io.IOException;
import java.util.Map;
import org.restlet.resource.Get;

public interface MasterConfResource
{
  @Get("json")
  public  Map<String, Object> retrieve()
    throws IOException;
}

