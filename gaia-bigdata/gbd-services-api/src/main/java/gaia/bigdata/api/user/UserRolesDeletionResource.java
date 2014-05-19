package gaia.bigdata.api.user;

import org.restlet.resource.Delete;

public interface UserRolesDeletionResource {
	@Delete
	public boolean remove();
}
