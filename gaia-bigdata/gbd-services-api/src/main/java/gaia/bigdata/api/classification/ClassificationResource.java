package gaia.bigdata.api.classification;

import java.util.Map;
import org.restlet.resource.Post;
import org.restlet.resource.Put;

public interface ClassificationResource {
	@Post
	public ClassifierResult classify(Map<String, Object> paramMap)
			throws Exception;

	@Put
	public ClassifierResult train(int paramInt, Map<String, Object> paramMap)
			throws UnsupportedOperationException;
}
