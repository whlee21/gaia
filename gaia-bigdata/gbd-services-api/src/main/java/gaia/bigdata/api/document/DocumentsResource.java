package gaia.bigdata.api.document;

import gaia.bigdata.api.State;
import org.restlet.ext.json.JsonRepresentation;
import org.restlet.resource.Delete;
import org.restlet.resource.Post;
import org.restlet.resource.Put;

public interface DocumentsResource {
	@Post
	public State add(JsonRepresentation paramJsonRepresentation);

	@Put
	public State update(JsonRepresentation paramJsonRepresentation);

	@Delete
	public State remove();
}
