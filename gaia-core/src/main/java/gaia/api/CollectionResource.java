package gaia.api;

import java.io.IOException;
import java.util.Map;
import org.apache.solr.client.solrj.SolrServerException;
import org.restlet.resource.Delete;
import org.restlet.resource.Get;

public interface CollectionResource
{
  @Get("json")
  public  Map<String, Object> retrieve()
    throws IOException, SolrServerException;

  @Delete("json")
  public  void remove(Map<String, Object> paramMap)
    throws Exception;
}

