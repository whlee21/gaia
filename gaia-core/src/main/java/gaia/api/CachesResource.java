package gaia.api;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import javax.xml.parsers.ParserConfigurationException;
import org.restlet.resource.Get;
import org.restlet.resource.Post;
import org.xml.sax.SAXException;

public interface CachesResource {
	@Post("json")
	public Map<String, Object> add(Map<String, Object> paramMap) throws IOException, ParserConfigurationException,
			SAXException;

	@Get("json")
	public List<Map<String, Object>> retrieve();
}
