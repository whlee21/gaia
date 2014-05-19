package gaia.bigdata.api.user;

import java.util.Collection;
import org.restlet.resource.Get;
import org.restlet.resource.Post;

public interface UsersResource {
	@Get
	public Collection<User> listUsers();

	@Post
	public User addUser(User paramUser);
}
