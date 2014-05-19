package gaia.security;

import gaia.crawl.security.SecurityFilter;
import java.util.Set;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.util.plugin.NamedListInitializedPlugin;

public abstract class ACLQueryFilterer implements NamedListInitializedPlugin {
	public void init(NamedList arg0) {
	}

	public abstract SecurityFilter buildFilter(Set<String> paramSet, String paramString);
}
