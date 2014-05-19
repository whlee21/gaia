package gaia.bigdata.api.document;

import gaia.bigdata.api.State;
import org.json.JSONException;
import org.restlet.ext.json.JsonRepresentation;
import org.restlet.resource.Post;

public interface DocumentsDeletionResource {
	@Post
	public State delete(JsonRepresentation paramJsonRepresentation)
			throws JSONException;
}
