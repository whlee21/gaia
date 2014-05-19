package gaia.bigdata.api.job;

import gaia.bigdata.api.State;
import org.restlet.resource.Delete;
import org.restlet.resource.Get;

public interface JobResource {
	@Get
	public State retrieve();

	@Delete
	public State cancel();
}
