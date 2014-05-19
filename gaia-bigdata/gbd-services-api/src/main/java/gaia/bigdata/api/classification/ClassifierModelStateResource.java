package gaia.bigdata.api.classification;

import gaia.bigdata.api.State;
import java.io.IOException;
import java.util.Map;
import org.restlet.resource.Delete;
import org.restlet.resource.Post;

public interface ClassifierModelStateResource {
	@Post
	public State load(Map<String, Object> paramMap)
			throws UnsupportedOperationException, IOException;

	@Delete
	public State unload() throws UnsupportedOperationException;
}
