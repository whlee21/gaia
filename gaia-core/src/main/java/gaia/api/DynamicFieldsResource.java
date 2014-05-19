package gaia.api;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.solr.client.solrj.SolrServerException;
import org.json.JSONException;
import org.quartz.SchedulerException;
import org.restlet.representation.InputRepresentation;
import org.restlet.resource.Delete;
import org.restlet.resource.Get;
import org.restlet.resource.Post;
import org.xml.sax.SAXException;

public interface DynamicFieldsResource
{
  @Post("json")
  public  Object add(InputRepresentation paramInputRepresentation)
    throws IOException, ParserConfigurationException, SAXException, SolrServerException, SchedulerException, JSONException;

  @Get("json")
  public  List<Map<String, Object>> retrieve();

  @Delete("json")
  public  void remove()
    throws IOException, ParserConfigurationException, SAXException, SolrServerException, SchedulerException;
}

