package gaia.bigdata.api.document;

import gaia.bigdata.ResultType;
import java.util.Map;
import org.restlet.resource.Post;

public interface DocumentsRetrievalResource {
	@Post(":json")
	public Map<ResultType, Object> retrieve(Map<String, Object> paramMap);
}
