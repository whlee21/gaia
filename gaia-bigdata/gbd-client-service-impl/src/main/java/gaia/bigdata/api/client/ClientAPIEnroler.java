package gaia.bigdata.api.client;

import gaia.bigdata.api.user.UserRolesResource;
import gaia.bigdata.services.ServiceType;
import gaia.commons.services.ServiceLocator;
import gaia.commons.util.RestletContainer;
import gaia.commons.util.RestletUtil;

import java.util.Iterator;
import java.util.List;

import org.codehaus.jackson.map.ObjectMapper;
import org.restlet.data.ClientInfo;
import org.restlet.security.Enroler;
import org.restlet.security.Role;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class ClientAPIEnroler implements Enroler {
	private static transient Logger log = LoggerFactory.getLogger(ClientAPIEnroler.class);
	private ServiceLocator serviceLocator;

	@Inject
	public ClientAPIEnroler(ServiceLocator serviceLocator) {
		this.serviceLocator = serviceLocator;
	}

	public void enrole(ClientInfo clientInfo) {
		String identifier = clientInfo.getUser().getIdentifier();
		RestletContainer<UserRolesResource> rolesRc = RestletUtil.wrap(UserRolesResource.class,
				serviceLocator.getServiceURI(ServiceType.USER.name()), "/" + identifier + "/roles");
		UserRolesResource roles = (UserRolesResource) rolesRc.getWrapped();
		List<Role> clientRoles;
		ObjectMapper mapper;
		try {
			List<Role> retrieve = roles.retrieve();
			if ((retrieve != null) && (!retrieve.isEmpty())) {
				clientRoles = clientInfo.getRoles();
				log.info("Adding roles " + retrieve + " for " + clientInfo.getUser().getIdentifier());
				mapper = new ObjectMapper();
				for (Iterator<Role> iter = retrieve.iterator(); iter.hasNext();) {
					Object role = iter.next();
					Role sdaRole = (Role) mapper.convertValue(role, Role.class);
					clientRoles.add(sdaRole);
				}
			}
		} catch (Throwable e) {
			throw new RuntimeException(e);
		} finally {
			RestletUtil.release(rolesRc);
		}
	}
}
