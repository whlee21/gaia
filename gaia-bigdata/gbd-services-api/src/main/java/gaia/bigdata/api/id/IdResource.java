package gaia.bigdata.api.id;

import org.restlet.resource.Get;

public interface IdResource {
	@Get
	public String generateId();
}
