package gaia.security;

import java.util.Set;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.util.plugin.NamedListInitializedPlugin;

public abstract class ACLTagProvider implements NamedListInitializedPlugin {
	public abstract Set<String> getACLTagsForUser(String paramString);

	public void init(NamedList arg0) {
	}
}
