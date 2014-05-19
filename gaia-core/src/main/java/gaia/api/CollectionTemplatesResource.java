package gaia.api;

import java.util.List;
import org.restlet.resource.Get;

public interface CollectionTemplatesResource
{
  @Get("json")
  public  List<String> retrieve();
}

