package gaia.bigdata.api.classification;

import gaia.bigdata.api.State;
import org.restlet.resource.Get;

public interface ClassifierStateResource {
	@Get
	public State info();
}
