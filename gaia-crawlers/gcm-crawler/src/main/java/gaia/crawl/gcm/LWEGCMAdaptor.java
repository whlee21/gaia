package gaia.crawl.gcm;

import gaia.crawl.io.Content;
import gaia.crawl.metadata.Metadata;
import gaia.crawl.security.ACE;
import gaia.crawl.security.AclProcessor;
import gaia.crawl.security.BasicAclProcessor;
import gaia.crawl.security.Principal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class LWEGCMAdaptor {
	private static final HashMap<String, LWEGCMAdaptor> adaptors = new HashMap<String, LWEGCMAdaptor>();

	private static final List<String> authzFilters = new ArrayList<String>();

	public static LWEGCMAdaptor getAdaptor(String dsType) {
		return (LWEGCMAdaptor) adaptors.get(dsType);
	}

	protected Set<String> getIgnoreProperties() {
		Set<String> ignore = new HashSet<String>();

		ignore.add("indexing");
		ignore.add("name");
		ignore.add("crawler");
		ignore.add("parsing");
		ignore.add("caching");
		ignore.add("collection");
		ignore.add("type");
		ignore.add("category");
		ignore.add("commit_on_finish");
		ignore.add("commit_within");
		ignore.add("max_docs");
		ignore.add("add_failed_docs");
		ignore.add("verify_access");
		return ignore;
	}

	protected Map<String, String> getTranslateMap() {
		Map<String, String> confTranslations = new HashMap<String, String>();
		confTranslations.put("connector_type", "connectorType");
		return confTranslations;
	}

	public final Map<String, String> getGCMProperties(Map<String, Object> dsProperties) {
		Map<String, String> gcmProperties = new HashMap<String, String>();
		copy(dsProperties, gcmProperties, getIgnoreProperties());
		customizeProperties(dsProperties, gcmProperties);
		translate(gcmProperties);
		return gcmProperties;
	}

	public boolean useAuthnSpiForDocumentsSecurity(Map<String, Object> dsProperties) {
		return false;
	}

	protected void customizeProperties(Map<String, Object> dsProperties, HashMap<String, String> gcmProperties) {
	}

	private void translate(Map<String, String> gcmProperties) {
		Map<String, String> translateMap = getTranslateMap();
		for (Map.Entry<String, String> translate : translateMap.entrySet()) {
			String value = (String) gcmProperties.remove(translate.getKey());
			if (value != null)
				gcmProperties.put(translate.getValue(), value);
		}
	}

	private void copy(Map<String, Object> dsProperties, HashMap<String, String> gcmProperties,
			Set<String> ignoreProperties) {
		for (Map.Entry<String, Object> source : dsProperties.entrySet())
			if ((!ignoreProperties.contains(source.getKey())) && (source.getValue() != null)) {
				gcmProperties.put(source.getKey(), source.getValue().toString());
			}
	}

	public static void register(String type, LWEGCMAdaptor adaptor) {
		adaptors.put(type, adaptor);
	}

	public void postProcess(Content c) {
		Metadata meta = c.getMetadata();

		List<ACE> aces = new ArrayList<ACE>();

		for (String key : authzFilters) {
			if (meta.contains(key)) {
				String[] oldValues = meta.getValues(key);
				if ((oldValues != null) && (oldValues.length != 0)) {
					Set<String> newValues = new HashSet<String>();
					for (int i = 0; i < oldValues.length; i++) {
						String rawValue = oldValues[i];
						String value = cleanAuthzValue(rawValue);
						newValues.add(value);
					}

					for (String name : newValues) {
						Principal principal = null;
						if (key.contains("groups")) {
							principal = new Principal(name, "GROUP");
						}
						if (key.contains("users")) {
							principal = new Principal(name, "USER");
						}
						if (principal != null) {
							aces.add(new ACE(principal, ACE.Type.ALLOW));
						}

					}

					meta.remove(key);
				}
			}
		}
		AclProcessor processor = new BasicAclProcessor("acl", "GCM");
		Metadata aclMeta = processor.encodeAcl(aces);

		for (Map.Entry<String, String[]> entry : aclMeta.entrySet())
			if (entry.getValue() != null)
				for (String value : (String[]) entry.getValue())
					meta.add((String) entry.getKey(), value);
	}

	private String cleanAuthzValue(String rawValue) {
		if (rawValue.endsWith("=peeker"))
			return null;
		return rawValue.replaceFirst("\\=(owner|reader|writer)", "");
	}

	static {
		authzFilters.add("GCM_google:aclusers");
		authzFilters.add("GCM_google:aclgroups");
	}
}
