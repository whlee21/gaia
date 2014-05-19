package gaia.bigdata.api.hdfs;

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
public class WebHDFSProxyAPI extends ProxyAPI {
	private static transient Logger log = LoggerFactory.getLogger(WebHDFSProxyAPI.class);

	@Inject
	public WebHDFSProxyAPI(ResourceFinder finder, Enroler enroler, Verifier verifier, ServiceLocator serviceLocator) {
		super(finder, enroler, verifier, serviceLocator);
	}

	protected void initAttachments() {
		URIPayload webHDFS = serviceLocator.getServiceURI(ServiceType.WEBHDFS.name());
		if ((webHDFS != null) && (webHDFS.uri != null)) {
			log.info("Attaching HDFS to " + webHDFS);
			Redirector redir = new Redirector(router.getContext(), webHDFS.uri.toString() + "{rr}", 6);

			ChallengeAuthenticator redirGuard = new ChallengeAuthenticator(getContext(), ChallengeScheme.HTTP_BASIC,
					"SDA Client");

			redirGuard.setVerifier(verifier);
			redirGuard.setEnroler(enroler);

			RoleAuthorizer ra = new RoleAuthorizer();
			ra.getAuthorizedRoles().add(SDARole.WEBHDFS.getRole());
			ra.getAuthorizedRoles().add(SDARole.ADMINISTRATOR.getRole());
			ra.setNext(redir);
			redirGuard.setNext(ra);
			router.attach("", redirGuard, 1);
		} else {
			log.error("WebHDFS not available.  Did you specify the webhdfs property?");
		}
	}

	public String getAPIRoot() {
		return "/webhdfs";
	}

	public String getAPIName() {
		return ServiceType.WEBHDFS_PROXY.name();
	}
}
