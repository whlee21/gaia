package gaia.bigdata.api.user;

import org.restlet.resource.Delete;
import org.restlet.resource.Get;
import org.restlet.resource.Put;

public interface UserResource {
	@Get
	public User retrieve();

	@Delete
	public User remove();

	@Put
	public User update(User paramUser);
}
