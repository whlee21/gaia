package gaia.api;

import java.util.List;
import org.restlet.representation.Representation;
import org.restlet.resource.Get;
import org.restlet.resource.Post;

public interface JDBCDriversResource
{
  @Get("json")
  public  List<String> retrieve()
    throws Exception;

  @Post
  public  Representation accept(Representation paramRepresentation)
    throws Exception;
}

