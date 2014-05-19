package gaia.bigdata.api.gaiasearch;

import gaia.bigdata.api.ProxyAPI;
import gaia.bigdata.api.SDARole;
import gaia.bigdata.services.ServiceType;
import gaia.commons.api.ResourceFinder;
import gaia.commons.services.ServiceLocator;
import gaia.commons.services.URIPayload;

import org.restlet.data.ChallengeScheme;
import org.restlet.routing.Redirector;
import org.restlet.security.ChallengeAuthenticator;
import org.restlet.security.Enroler;
import org.restlet.security.RoleAuthorizer;
import org.restlet.security.Verifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class GaiaSearchProxyAPI extends ProxyAPI {
	private static transient Logger log = LoggerFactory.getLogger(GaiaSearchProxyAPI.class);

	@Inject
	public GaiaSearchProxyAPI(ResourceFinder finder, ServiceLocator serviceLocator, Verifier verifier, Enroler enroler) {
		super(finder, enroler, verifier, serviceLocator);
	}

	protected void initAttachments() {
		URIPayload gaia = serviceLocator.getServiceURI(ServiceType.GAIASEARCH.name());
		if ((gaia != null) && (gaia.uri != null)) {
			log.info("Attaching Gaia to " + gaia);
			Redirector redir = new Redirector(router.getContext(), gaia.uri.toString() + "{rr}", 6);

			ChallengeAuthenticator redirGuard = new ChallengeAuthenticator(getContext(), ChallengeScheme.HTTP_BASIC,
					"SDA Client");

			redirGuard.setVerifier(verifier);
			redirGuard.setEnroler(enroler);

			RoleAuthorizer ra = new RoleAuthorizer();
			ra.getAuthorizedRoles().add(SDARole.GAIASEARCH.getRole());
			ra.getAuthorizedRoles().add(SDARole.ADMINISTRATOR.getRole());
			ra.setNext(redir);
			redirGuard.setNext(ra);
			router.attach("", redirGuard, 1);
		} else {
			log.warn("Unable to locate " + ServiceType.GAIASEARCH.name());
		}
	}

	public String getAPIRoot() {
		return "/gaiasearch";
	}

	public String getAPIName() {
		return ServiceType.GAIASEARCH_PROXY.name();
	}
}
