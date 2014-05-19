package gaia.api;

import java.io.IOException;
import java.util.Map;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.solr.client.solrj.SolrServerException;
import org.json.JSONException;
import org.quartz.SchedulerException;
import org.restlet.resource.Get;
import org.restlet.resource.Put;
import org.xml.sax.SAXException;

public interface SettingsResource
{
  @Put("json")
  public  void update(Map<String, Object> paramMap)
    throws IOException, ParserConfigurationException, SAXException, SolrServerException, JSONException, SchedulerException;

  @Get("json")
  public  Map<String, Object> retrieve()
    throws IOException;
}

