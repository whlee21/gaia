package gaia.security.ad;

import java.util.Collections;
import java.util.Set;
import org.apache.solr.common.util.NamedList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NotCachingADACLTagProvider extends ADACLTagProvider {
	private Logger LOG = LoggerFactory.getLogger(ADACLTagProvider.class);

	public Set<String> getACLTagsForUser(String userName) {
		if (userName != null) {
			try {
				Set<String> tags = helper.getSidsForUser(userName);
				if (tags.size() > 0) {
					tags.add(sidEveryone);
				}
				return tags;
			} catch (Throwable e) {
				LOG.error("Could not retrieve user data from ActiveDirectory: " + e.getMessage(), e);
				return Collections.emptySet();
			}
		}

		return null;
	}

	public void init(NamedList config) {
		initConfig(config);
	}
}
