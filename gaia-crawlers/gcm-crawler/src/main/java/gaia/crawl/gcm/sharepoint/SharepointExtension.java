package gaia.crawl.gcm.sharepoint;

import gaia.crawl.datasource.DataSourceSpec;
import gaia.crawl.gcm.GCMExtension;
import gaia.crawl.gcm.LWEGCMAdaptor;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;

public class SharepointExtension extends LWEGCMAdaptor implements GCMExtension {
	private static String NL = "/$$CRLF$$/";
	private static String EQ = "/$$EQUAL$$/";

	public void register(Map<String, DataSourceSpec> types) {
		LWEGCMAdaptor.register("sharepoint", this);
		types.put("sharepoint", new SharepointSpec(this));
	}

	protected void customizeProperties(Map<String, Object> dsProperties, HashMap<String, String> gcmProperties) {
		super.customizeProperties(dsProperties, gcmProperties);

		Object kdcserver = dsProperties.get("kdcserver");
		if (kdcserver == null) {
			gcmProperties.put("kdcserver", "");
		}

		Object includedUrls = dsProperties.get(SharepointSpec.INCLUDED_URLS);
		if ((includedUrls != null) && ((includedUrls instanceof List)) && (!((List) includedUrls).isEmpty())) {
			gcmProperties.put(SharepointSpec.INCLUDED_URLS, StringUtils.join((List) includedUrls, " "));
		} else {
			gcmProperties.put(SharepointSpec.INCLUDED_URLS, "/");
		}
		Object excludedUrls = dsProperties.get(SharepointSpec.EXCLUDED_URLS);
		if ((excludedUrls != null) && ((excludedUrls instanceof List)) && (!((List) excludedUrls).isEmpty())) {
			gcmProperties.put(SharepointSpec.EXCLUDED_URLS, StringUtils.join((List) excludedUrls, " "));
		} else {
			gcmProperties.put(SharepointSpec.EXCLUDED_URLS, "");
		}

		Object aliases = dsProperties.get("aliases");
		if (aliases != null) {
			gcmProperties.put("aliases", convertAliases(aliases));
		}

		Object useVisibility = dsProperties.get("use_sp_search_visibility");
		if ((useVisibility != null) && (Boolean.TRUE.equals(Boolean.valueOf(useVisibility.toString())))) {
			gcmProperties.put("use_sp_search_visibility", "on");
		}

		Object feedUnpublished = dsProperties.get(SharepointSpec.FEED_UNPUBLISHED_DOCUMENTS);
		if ((feedUnpublished != null) && (Boolean.TRUE.equals(Boolean.valueOf(feedUnpublished.toString())))) {
			gcmProperties.put(SharepointSpec.FEED_UNPUBLISHED_DOCUMENTS, "on");
		}

		Object pushAcls = dsProperties.get(SharepointSpec.PUSH_ACLS);
		if ((pushAcls != null) && (Boolean.TRUE.equals(Boolean.valueOf(pushAcls.toString())))) {
			gcmProperties.put(SharepointSpec.PUSH_ACLS, "on");

			Object useSSL = dsProperties.get(SharepointSpec.LDAP_SERVER_USE_SSL);
			if ((useSSL != null) && (Boolean.TRUE.equals(Boolean.valueOf(useSSL.toString()))))
				gcmProperties.put(SharepointSpec.LDAP_SERVER_USE_SSL, "SSL");
			else {
				gcmProperties.put(SharepointSpec.LDAP_SERVER_USE_SSL, "Standard");
			}

			Object useCacheForLdap = dsProperties.get(SharepointSpec.LDAP_CACHE_GROUPS_MEMBERSHIP);
			if ((useCacheForLdap != null) && (Boolean.TRUE.equals(Boolean.valueOf(useCacheForLdap.toString())))) {
				gcmProperties.put(SharepointSpec.LDAP_CACHE_GROUPS_MEMBERSHIP, "on");
			} else {
				gcmProperties.remove(SharepointSpec.LDAP_CACHE_SIZE);
				gcmProperties.remove(SharepointSpec.LDAP_CACHE_REFRESH_INTERVAL);
			}
		} else {
			String[] aclProps = { SharepointSpec.USERNAME_FORMAT_IN_ACE, SharepointSpec.GROUPNAME_FORMAT_IN_ACE,
					SharepointSpec.LDAP_SERVER_HOST_ADDRESS, SharepointSpec.LDAP_SERVER_PORT_NUMBER,
					SharepointSpec.LDAP_SERVER_USE_SSL, SharepointSpec.LDAP_SEARCH_BASE, SharepointSpec.LDAP_AUTH_TYPE,
					SharepointSpec.LDAP_CACHE_GROUPS_MEMBERSHIP, SharepointSpec.LDAP_CACHE_SIZE,
					SharepointSpec.LDAP_CACHE_REFRESH_INTERVAL };

			for (String prop : aclProps) {
				gcmProperties.remove(prop);
			}
		}

		gcmProperties.put("connector_type", "sharepoint-connector");
		gcmProperties.put(SharepointSpec.AUTHORIZATION, "content");
	}

	public boolean useAuthnSpiForDocumentsSecurity(Map<String, Object> dsProperties) {
		Object enableSecurityTrimming = dsProperties.get("enable_security_trimming");
		if ((enableSecurityTrimming != null) && (Boolean.TRUE.equals(Boolean.valueOf(enableSecurityTrimming.toString())))) {
			return true;
		}
		return false;
	}

	protected Set<String> getIgnoreProperties() {
		Set<String> result = new HashSet<String>(super.getIgnoreProperties());
		result.addAll(Arrays.asList(new String[] { "aliases", "use_sp_search_visibility",
				SharepointSpec.FEED_UNPUBLISHED_DOCUMENTS, SharepointSpec.PUSH_ACLS, SharepointSpec.LDAP_SERVER_USE_SSL,
				SharepointSpec.LDAP_CACHE_GROUPS_MEMBERSHIP, "enable_security_trimming" }));

		return result;
	}

	protected Map<String, String> getTranslateMap() {
		Map<String, String> translations = super.getTranslateMap();
		translations.put(SharepointSpec.SHAREPOINT_URL, "sharepointUrl");
		translations.put("my_site_base_url", "mySiteBaseURL");
		translations.put(SharepointSpec.INCLUDED_URLS, "includedURls");
		translations.put("aliases", "aliases");
		translations.put(SharepointSpec.EXCLUDED_URLS, "excludedURls");
		translations.put("use_sp_search_visibility", "useSPSearchVisibility");
		translations.put(SharepointSpec.FEED_UNPUBLISHED_DOCUMENTS, "feedUnPublishedDocuments");
		translations.put(SharepointSpec.VISITS_PER_URL, "visitsPerUrl");
		translations.put(SharepointSpec.USE_CHECKSUM_DETECTION, "useChecksumDetection");

		translations.put(SharepointSpec.PUSH_ACLS, "pushAcls");
		translations.put(SharepointSpec.USERNAME_FORMAT_IN_ACE, "usernameFormatInAce");
		translations.put(SharepointSpec.GROUPNAME_FORMAT_IN_ACE, "groupnameFormatInAce");

		translations.put(SharepointSpec.LDAP_SERVER_HOST_ADDRESS, "ldapServerHostAddress");
		translations.put(SharepointSpec.LDAP_SERVER_PORT_NUMBER, "portNumber");
		translations.put(SharepointSpec.LDAP_SERVER_USE_SSL, "connectMethod");
		translations.put(SharepointSpec.LDAP_AUTH_TYPE, "authenticationType");
		translations.put(SharepointSpec.LDAP_READ_GROUPS_TYPE, "readAdGroupsType");
		translations.put(SharepointSpec.LDAP_SEARCH_BASE, "searchBase");

		translations.put(SharepointSpec.LDAP_CACHE_GROUPS_MEMBERSHIP, "useCacheToStoreLdapUserGroupsMembership");
		translations.put(SharepointSpec.LDAP_CACHE_SIZE, "initialCacheSize");
		translations.put(SharepointSpec.LDAP_CACHE_REFRESH_INTERVAL, "cacheRefreshInterval");

		return translations;
	}

	private String convertAliases(Object aliases) {
		if ((aliases != null) && ((aliases instanceof Map))) {
			Map aliasMap = (Map) aliases;
			StringBuilder sb = new StringBuilder();
			for (Map.Entry entry : aliasMap.entrySet()) {
				sb.append(entry.getKey().toString()).append(EQ).append(entry.getValue().toString()).append(NL);
			}

			return sb.toString();
		}
		return null;
	}
}
