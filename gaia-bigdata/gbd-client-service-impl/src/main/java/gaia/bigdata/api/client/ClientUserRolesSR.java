package gaia.bigdata.api.client;

import gaia.bigdata.api.user.UserRolesResource;
import gaia.bigdata.services.ServiceType;
import gaia.commons.api.Configuration;
import gaia.commons.services.BaseServiceLocatorSR;
import gaia.commons.services.ServiceLocator;
import gaia.commons.util.RestletContainer;
import gaia.commons.util.RestletUtil;

import java.util.List;

import org.restlet.resource.ResourceException;
import org.restlet.security.Role;

import com.google.inject.Inject;

public class ClientUserRolesSR extends BaseServiceLocatorSR implements UserRolesResource {
	private String username;

	@Inject
	public ClientUserRolesSR(Configuration configuration, ServiceLocator serviceLocator) {
		super(configuration, serviceLocator);
	}

	protected void doInit() throws ResourceException {
		username = getRequest().getAttributes().get("username").toString();
	}

	public List<Role> retrieve() {
		List<Role> returnValue = null;
		RestletContainer<UserRolesResource> resourceRc = RestletUtil.wrap(UserRolesResource.class,
				serviceLocator.getServiceURI(ServiceType.USER.name()), "/" + username + "/roles");
		UserRolesResource resource = (UserRolesResource) resourceRc.getWrapped();
		try {
			returnValue = resource.retrieve();
		} catch (Throwable e) {
			throw new RuntimeException(e);
		} finally {
			RestletUtil.release(resourceRc);
		}
		return returnValue;
	}

	public boolean add(List<String> roleNames) {
		boolean returnValue = false;
		RestletContainer<UserRolesResource> resourceRc = RestletUtil.wrap(UserRolesResource.class,
				serviceLocator.getServiceURI(ServiceType.USER.name()), "/" + username + "/roles");
		UserRolesResource resource = (UserRolesResource) resourceRc.getWrapped();
		try {
			returnValue = resource.add(roleNames);
		} catch (Throwable e) {
			throw new RuntimeException(e);
		} finally {
			RestletUtil.release(resourceRc);
		}
		return returnValue;
	}
}
