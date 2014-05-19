package gaia.bigdata.api.client;

import gaia.bigdata.api.user.User;
import gaia.bigdata.api.user.UserResource;
import gaia.bigdata.services.ServiceType;
import gaia.commons.api.Configuration;
import gaia.commons.services.BaseServiceLocatorSR;
import gaia.commons.services.ServiceLocator;
import gaia.commons.util.RestletContainer;
import gaia.commons.util.RestletUtil;

import org.restlet.resource.ResourceException;

import com.google.inject.Inject;

public class ClientUserSR extends BaseServiceLocatorSR implements UserResource {
	private String username;

	@Inject
	public ClientUserSR(Configuration configuration, ServiceLocator serviceLocator) {
		super(configuration, serviceLocator);
	}

	protected void doInit() throws ResourceException {
		username = getRequest().getAttributes().get("username").toString();
	}

	public User retrieve() {
		User returnValue = null;
		RestletContainer<UserResource> urRc = RestletUtil.wrap(UserResource.class,
				serviceLocator.getServiceURI(ServiceType.USER.name()), "/" + username);
		UserResource ur = (UserResource) urRc.getWrapped();
		try {
			returnValue = ur.retrieve();
		} catch (Throwable e) {
			throw new RuntimeException(e);
		} finally {
			RestletUtil.release(urRc);
		}
		return returnValue;
	}

	public User remove() {
		User returnValue = null;
		RestletContainer<UserResource> urRc = RestletUtil.wrap(UserResource.class,
				serviceLocator.getServiceURI(ServiceType.USER.name()), "/" + username);
		UserResource ur = (UserResource) urRc.getWrapped();
		try {
			returnValue = ur.remove();
		} catch (Throwable e) {
			throw new RuntimeException(e);
		} finally {
			RestletUtil.release(urRc);
		}
		return returnValue;
	}

	public User update(User user) {
		User returnValue = null;
		RestletContainer<UserResource> urRc = RestletUtil.wrap(UserResource.class,
				serviceLocator.getServiceURI(ServiceType.USER.name()), "/" + username);
		UserResource ur = (UserResource) urRc.getWrapped();
		try {
			returnValue = ur.update(user);
		} catch (Throwable e) {
			throw new RuntimeException(e);
		} finally {
			RestletUtil.release(urRc);
		}
		return returnValue;
	}
}
