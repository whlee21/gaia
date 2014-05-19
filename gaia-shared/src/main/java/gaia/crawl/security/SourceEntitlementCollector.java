package gaia.crawl.security;

import java.util.List;

public interface SourceEntitlementCollector {
	public List<Principal> getEntitlementsForPrincipal(Principal paramPrincipal);
}
