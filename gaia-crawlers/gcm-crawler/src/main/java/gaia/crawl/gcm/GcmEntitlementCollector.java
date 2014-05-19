package gaia.crawl.gcm;

import gaia.crawl.gcm.api.AuthenticationResponse;
import gaia.crawl.gcm.api.RemoteGCMServer;
import gaia.crawl.security.Principal;
import gaia.crawl.security.SourceEntitlementCollector;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GcmEntitlementCollector implements SourceEntitlementCollector {
	private static final Logger LOG = LoggerFactory.getLogger(GcmEntitlementCollector.class);
	private RemoteGCMServer client;
	private String connectorName;

	public GcmEntitlementCollector(RemoteGCMServer client, String connectorName) {
		this.client = client;
		this.connectorName = connectorName;
	}

	public List<Principal> getEntitlementsForPrincipal(Principal principal) {
		List<Principal> entitlements = new ArrayList<Principal>();
		entitlements.add(principal);
		try {
			AuthenticationResponse resp = this.client.authenticate(this.connectorName, principal.getName(), null, null);

			if (resp.getGroups() != null)
				for (String group : resp.getGroups())
					entitlements.add(new Principal(group, PrincipalType.GROUP.toString()));
		} catch (IOException e) {
			LOG.warn("IOException when calling Google connector authenticate SPI to expand username with groups", e);
		}
		LOG.info("Entitlements for user '" + principal + "': " + entitlements);
		return entitlements;
	}

	public static enum PrincipalType {
		USER, GROUP;
	}
}
