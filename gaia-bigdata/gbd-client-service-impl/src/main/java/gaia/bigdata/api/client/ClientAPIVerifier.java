package gaia.bigdata.api.client;

import gaia.bigdata.api.user.User;
import gaia.bigdata.api.user.UserResource;
import gaia.bigdata.services.ServiceType;
import gaia.commons.services.ServiceLocator;
import gaia.commons.util.RestletContainer;
import gaia.commons.util.RestletUtil;
import gaia.commons.util.StringUtil;

import java.nio.CharBuffer;

import org.restlet.data.Status;
import org.restlet.resource.ResourceException;
import org.restlet.security.SecretVerifier;
import org.restlet.security.Verifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class ClientAPIVerifier extends SecretVerifier implements Verifier {
	private static transient Logger log = LoggerFactory.getLogger(ClientAPIVerifier.class);
	private ServiceLocator serviceLocator;

	@Inject
	public ClientAPIVerifier(ServiceLocator serviceLocator) {
		this.serviceLocator = serviceLocator;
	}

	public int verify(String identifier, char[] secret) {
		int result = 5;
		RestletContainer<UserResource> urRc = RestletUtil.wrap(UserResource.class,
				serviceLocator.getServiceURI(ServiceType.USER.name()), "/" + identifier);

		if (urRc != null) {
			UserResource ur = (UserResource) urRc.getWrapped();
			try {
				if (ur != null) {
					User user = ur.retrieve();
					if (user != null) {
						CharBuffer wrap = CharBuffer.wrap(secret);
						result = user.password.contentEquals(StringUtil.md5Hash(wrap)) == true ? 4 : -1;
					} else {
						result = -1;
					}
				} else {
					log.warn("Unable to find the User Service");
				}
			} catch (ResourceException e) {
				if (e.getStatus().equals(Status.CLIENT_ERROR_NOT_FOUND))
					result = -1;
			} catch (Throwable e) {
				throw new RuntimeException(e);
			} finally {
				RestletUtil.release(urRc);
			}
		} else {
			log.warn("Can't find the USER service");
		}
		return result;
	}
}
