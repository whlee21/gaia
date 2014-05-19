package gaia.api;

import java.io.IOException;
import java.util.Map;
import org.restlet.resource.Get;

public interface VersionResource
{
  @Get("json")
  public  Map<String, Object> list()
    throws IOException;
}

