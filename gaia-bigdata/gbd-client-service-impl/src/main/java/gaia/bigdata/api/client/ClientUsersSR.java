package gaia.bigdata.api.client;

import gaia.bigdata.api.user.User;
import gaia.bigdata.api.user.UsersResource;
import gaia.bigdata.services.ServiceType;
import gaia.commons.api.Configuration;
import gaia.commons.services.BaseServiceLocatorSR;
import gaia.commons.services.ServiceLocator;
import gaia.commons.util.RestletContainer;
import gaia.commons.util.RestletUtil;

import java.util.Collection;

import com.google.inject.Inject;

public class ClientUsersSR extends BaseServiceLocatorSR implements UsersResource {
	@Inject
	public ClientUsersSR(Configuration configuration, ServiceLocator serviceLocator) {
		super(configuration, serviceLocator);
	}

	public Collection<User> listUsers() {
		Collection<User> returnValue = null;
		RestletContainer<UsersResource> resourceRc = RestletUtil.wrap(UsersResource.class,
				serviceLocator.getServiceURI(ServiceType.USER.name()), "");
		UsersResource resource = (UsersResource) resourceRc.getWrapped();
		try {
			returnValue = resource.listUsers();
		} catch (Throwable e) {
			throw new RuntimeException(e);
		} finally {
			RestletUtil.release(resourceRc);
		}
		return returnValue;
	}

	public User addUser(User user) {
		User returnValue = null;
		RestletContainer<UsersResource> resourceRc = RestletUtil.wrap(UsersResource.class,
				serviceLocator.getServiceURI(ServiceType.USER.name()), "");
		UsersResource resource = (UsersResource) resourceRc.getWrapped();
		try {
			returnValue = resource.addUser(user);
		} catch (Throwable e) {
			throw new RuntimeException(e);
		} finally {
			RestletUtil.release(resourceRc);
		}
		return returnValue;
	}
}
