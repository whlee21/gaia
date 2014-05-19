package gaia.security.ad;

import gaia.security.ACLTagProvider;
import java.util.Collections;
import java.util.Set;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.NamedList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ADACLTagProvider extends ACLTagProvider {
	private Logger LOG = LoggerFactory.getLogger(ACLTagProvider.class);
	public static final String DEFAULT_TAG_EVERYONE = "S-1-1-0";
	public static final String SID_EVERYONE = "sidEveryone";
	protected ADHelper helper;
	protected String sidEveryone;

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
		helper.readGroups();
	}

	protected void initConfig(NamedList config) {
		SolrParams params = SolrParams.toSolrParams(config);
		String userSearch = params.get("userFilter", "(&(objectclass=user)(userPrincipalName={0}))");
		String groupSearch = params.get("groupFilter", "(objectclass=group)");
		String providerUrl = getParam(params, "java.naming.provider.url", null);
		String authenticationType = getParam(params, "java.naming.security.authentication", "simple");
		String principal = getParam(params, "java.naming.security.principal", null);
		String credentials = getParam(params, "java.naming.security.credentials", null);
		String userBase = getParam(params, "userBaseDN", null);
		String groupBase = getParam(params, "groupBaseDN", null);
		String initialContextFactory = getParam(params, "java.naming.factory.initial", null);

		sidEveryone = getParam(params, "sidEveryone", "S-1-1-0");

		if ((principal == null) || (credentials == null)) {
			LOG
					.error("java.naming.security.principal or java.naming.security.credentials was not specified in config");
		}

		helper = new ADHelper(providerUrl, principal, credentials);
		helper.setAuthenticationType(authenticationType);
		if (initialContextFactory != null)
			helper.setInitialContextFactory(initialContextFactory);
		if (userBase != null)
			helper.setUserBasedn(userBase);
		if (groupBase != null)
			helper.setGroupBaseDn(groupBase);
		helper.setAllGroupsFilter(groupSearch);
		helper.setUserFilter(userSearch);
	}

	private String getParam(SolrParams params, String paramName, String defaultValue) {
		return params.get(paramName, System.getProperty(paramName, defaultValue));
	}
}
