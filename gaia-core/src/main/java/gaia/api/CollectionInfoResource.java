package gaia.api;

import java.io.IOException;
import java.util.Map;
import org.apache.solr.client.solrj.SolrServerException;
import org.restlet.resource.Get;

public interface CollectionInfoResource
{
  @Get("json")
  public  Map<String, Object> retrieve()
    throws IOException, SolrServerException;
}

