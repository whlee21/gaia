package gaia.bigdata.api.client;

import gaia.bigdata.api.user.UserRolesDeletionResource;
import gaia.bigdata.services.ServiceType;
import gaia.commons.api.Configuration;
import gaia.commons.services.BaseServiceLocatorSR;
import gaia.commons.services.ServiceLocator;
import gaia.commons.util.RestletContainer;
import gaia.commons.util.RestletUtil;

import org.restlet.resource.ResourceException;

import com.google.inject.Inject;

public class ClientUserRolesDeletionSR extends BaseServiceLocatorSR implements UserRolesDeletionResource {
	private String username;
	private String rolename;

	@Inject
	public ClientUserRolesDeletionSR(Configuration configuration, ServiceLocator serviceLocator) {
		super(configuration, serviceLocator);
	}

	protected void doInit() throws ResourceException {
		username = getRequest().getAttributes().get("username").toString();
		rolename = getRequest().getAttributes().get("rolename").toString();
	}

	public boolean remove() {
		boolean returnValue = false;
		RestletContainer<UserRolesDeletionResource> resourceToRemoveRc = RestletUtil.wrap(UserRolesDeletionResource.class,
				serviceLocator.getServiceURI(ServiceType.USER.name()), "/" + username + "/roles/" + rolename);

		UserRolesDeletionResource resourceToRemove = (UserRolesDeletionResource) resourceToRemoveRc.getWrapped();
		try {
			returnValue = resourceToRemove.remove();
		} catch (Throwable e) {
			throw new RuntimeException(e);
		} finally {
			RestletUtil.release(resourceToRemoveRc);
		}
		return returnValue;
	}
}
