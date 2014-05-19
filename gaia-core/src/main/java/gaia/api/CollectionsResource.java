package gaia.api;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.apache.solr.client.solrj.SolrServerException;
import org.restlet.resource.Get;
import org.restlet.resource.Post;

public interface CollectionsResource
{
  @Get("json")
  public  List<Map<String, Object>> retrieve()
    throws IOException, SolrServerException;

  @Post("json")
  public  Map<String, Object> add(Map<String, Object> paramMap)
    throws Exception;
}

