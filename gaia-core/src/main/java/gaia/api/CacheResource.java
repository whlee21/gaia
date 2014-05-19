package gaia.api;

import java.io.IOException;
import java.util.Map;
import javax.xml.parsers.ParserConfigurationException;
import org.restlet.resource.Delete;
import org.restlet.resource.Get;
import org.restlet.resource.Put;
import org.xml.sax.SAXException;

public interface CacheResource {
	@Get("json")
	public Map<String, Object> retrieve();

	@Put("json")
	public void update(Map<String, Object> paramMap) throws ParserConfigurationException, IOException, SAXException;

	@Delete
	public void remove() throws ParserConfigurationException, IOException, SAXException;
}
