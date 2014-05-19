package gaia.api;

import java.util.Map;
import org.restlet.resource.Get;

public interface SecurityTrimmingResource
{
  @Get("json")
  public  Map<String, ?> retrieve();
}

