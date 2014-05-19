package gaia.api;

import java.util.List;
import org.restlet.resource.Get;

public interface JDBCDriversClassesResource
{
  @Get("json")
  public  List<String> retrieve()
    throws Exception;
}

