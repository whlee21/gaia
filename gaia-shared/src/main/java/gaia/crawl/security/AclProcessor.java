package gaia.crawl.security;

import gaia.crawl.metadata.Metadata;
import java.util.List;

public interface AclProcessor {
	public SecurityFilter buildSearchFilter(List<Principal> paramList);

	public Metadata encodeAcl(List<ACE> paramList);
}
