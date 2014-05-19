package gaia.api;

import org.restlet.representation.Representation;
import org.restlet.resource.Delete;
import org.restlet.resource.Get;
import org.restlet.resource.Put;

public interface JDBCDriverResource
{
  @Get("html")
  public  Representation retrieve(Representation paramRepresentation)
    throws Exception;

  @Delete("json")
  public  void remove()
    throws Exception;

  @Put
  public  Representation accept(Representation paramRepresentation)
    throws Exception;
}

