package gaia.api;

import java.io.IOException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;
import org.quartz.SchedulerException;
import org.restlet.resource.Get;
import org.restlet.resource.Put;
import org.w3c.dom.DOMException;
import org.xml.sax.SAXException;

public interface HandlerComponentsResource
{
  @Get("json")
  public  String[] retrieve();

  @Put("json")
  public  void update(String[] paramArrayOfString)
    throws DOMException, XPathExpressionException, IOException, ParserConfigurationException, SAXException, SchedulerException;
}

