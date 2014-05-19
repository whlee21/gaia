package gaia.bigdata.api.document;

import gaia.bigdata.api.State;
import java.util.Map;
import org.restlet.ext.json.JsonRepresentation;
import org.restlet.resource.Delete;
import org.restlet.resource.Get;
import org.restlet.resource.Put;

public interface DocumentResource {
	@Get
	public Object retrieve(Map<String, Object> paramMap);

	@Delete
	public State remove();

	@Put
	public State update(JsonRepresentation paramJsonRepresentation);
}
