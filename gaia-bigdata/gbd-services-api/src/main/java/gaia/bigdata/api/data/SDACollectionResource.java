package gaia.bigdata.api.data;

import gaia.bigdata.api.State;
import org.restlet.resource.Delete;
import org.restlet.resource.Get;

public interface SDACollectionResource {
	@Get
	public State retrieve();

	@Delete
	public State remove();
}
