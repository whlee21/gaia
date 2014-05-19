package gaia.bigdata.api.user;

import java.util.List;
import org.restlet.resource.Get;
import org.restlet.resource.Post;
import org.restlet.security.Role;

public interface UserRolesResource {
	@Get
	public List<Role> retrieve();

	@Post
	public boolean add(List<String> paramList);
}
