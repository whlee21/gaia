package gaia.ssl;

import java.security.Principal;
import java.security.cert.X509Certificate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SSLAuthorizer {
	private static transient Logger LOG = LoggerFactory.getLogger(SSLAuthorizer.class);
	private static boolean requireSSL;
	private static String[] authorizedClients;
	private static boolean requireAuthorization;

	static void setRequireSSL(boolean paramRequireSSL) {
		requireSSL = paramRequireSSL;
	}

	public static void setAuthorizedClients(String[] paramAuthorizedClients) {
		authorizedClients = paramAuthorizedClients;
	}

	public static void setRequireAuthorization(boolean paramRequireAuthorization) {
		requireAuthorization = paramRequireAuthorization;
	}

	public static boolean authorizeRequest(boolean isSecure, X509Certificate[] certificates, String remoteAddr) {
		if (isSecure) {
			return authorizeSecureRequest(certificates, remoteAddr);
		}
		return authorizeNonSecureRequest(remoteAddr);
	}

	private static boolean authorizeNonSecureRequest(String remoteAddr) {
		if (requireSSL) {
			if (LOG.isDebugEnabled()) {
				LOG.debug("Client (" + remoteAddr
						+ ") tried to access service through non secure channel when ssl is required. Request is blocked.");
			}

			return false;
		}

		return true;
	}

	private static boolean authorizeSecureRequest(X509Certificate[] certificates, String remoteAdds) {
		if (!requireSSL) {
			if (LOG.isDebugEnabled()) {
				LOG.debug("Client (" + remoteAdds + ") tried accessing service through secure channel when ssl is disabled."
						+ " request is blocked.");
			}

			return false;
		}

		if ((certificates != null) && (certificates.length > 0)) {
			X509Certificate certificate = certificates[0];
			Principal principal = certificate.getSubjectDN();
			if (LOG.isDebugEnabled()) {
				LOG.debug("Client (" + remoteAdds + ") certificate principal:" + principal.getName());
			}

			if (requireAuthorization) {
				if ((authorizedClients == null) || (authorizedClients.length == 0)) {
					LOG.info("Authorization is required but there are no authorized certificates configured. Denying access.");

					return false;
				}

				return authorizePrincipal(principal, remoteAdds);
			}

			LOG.info("Authorization is not required but client provided certificate with DN:" + principal.getName());

			return true;
		}

		if (requireAuthorization) {
			LOG.info("Authorization is required and client provided no certifcate.");

			return false;
		}
		if (LOG.isDebugEnabled()) {
			LOG.debug("Authorization is not required.");
		}
		return true;
	}

	private static boolean authorizePrincipal(Principal principal, String remoteAddr) {
		for (String clientdn : authorizedClients) {
			if (clientdn.equals(principal.getName())) {
				if (LOG.isDebugEnabled()) {
					LOG.debug("Client (" + remoteAddr + ") presented an authorized certificate with DN:" + principal.getName());
				}

				return true;
			}
		}
		LOG.info("Client " + remoteAddr + " presented certificate DN: " + principal.getName()
				+ " which is not authorized. Denying access.");

		return false;
	}
}
