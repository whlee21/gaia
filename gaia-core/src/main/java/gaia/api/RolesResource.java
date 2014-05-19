package gaia.api;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;
import org.quartz.SchedulerException;
import org.restlet.resource.Get;
import org.restlet.resource.Post;
import org.w3c.dom.DOMException;
import org.xml.sax.SAXException;

public interface RolesResource
{
  @Post("json")
  public  Map<String, Object> add(Map<String, Object> paramMap)
    throws DOMException, XPathExpressionException, IOException, ParserConfigurationException, SAXException, SchedulerException;

  @Get("json")
  public  List<Map<String, Object>> retrieve();
}

