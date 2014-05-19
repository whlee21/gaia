package gaia.api;

import java.io.IOException;
import java.util.Map;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;
import org.quartz.SchedulerException;
import org.restlet.resource.Delete;
import org.restlet.resource.Get;
import org.restlet.resource.Post;
import org.restlet.resource.Put;
import org.w3c.dom.DOMException;
import org.xml.sax.SAXException;

public interface ACLFilteringResource {
	@Get("json")
	public Map<String, Object> retrieve();

	@Delete
	public void remove() throws DOMException, XPathExpressionException, IOException, ParserConfigurationException,
			SAXException, SchedulerException;

	@Post("json")
	public void create(Map<String, Object> paramMap) throws DOMException, XPathExpressionException, IOException,
			ParserConfigurationException, SAXException, SchedulerException;

	@Put("json")
	public void update(Map<String, Object> paramMap) throws DOMException, XPathExpressionException, IOException,
			ParserConfigurationException, SAXException, SchedulerException;
}
