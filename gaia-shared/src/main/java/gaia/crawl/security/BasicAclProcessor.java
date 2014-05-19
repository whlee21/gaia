package gaia.crawl.security;

import gaia.crawl.metadata.Metadata;
import java.util.List;
import org.apache.commons.lang.StringUtils;
import org.apache.solr.client.solrj.util.ClientUtils;

public class BasicAclProcessor implements AclProcessor {
	private String aclField;
	private String prefix;

	public BasicAclProcessor(String aclField, String prefix) {
		this.aclField = aclField;
		this.prefix = prefix;
	}

	public SecurityFilter buildSearchFilter(List<Principal> entitlements) {
		if ((entitlements == null) || (entitlements.size() == 0)) {
			return new SecurityFilter("-*:*");
		}
		StringBuffer sb = new StringBuffer("{!lucene q.op=OR}");
		for (Principal principal : entitlements) {
			String allowPrefix = new StringBuilder()
					.append(prefix)
					.append("_ALLOW_")
					.append(
							principal.getNamespace() == null ? "" : new StringBuilder().append(principal.getNamespace()).append("_")
									.toString()).toString();
			sb.append(new StringBuilder()
					.append(" ")
					.append(aclField)
					.append(":")
					.append(
							ClientUtils.escapeQueryChars(new StringBuilder().append(allowPrefix).append(principal.getName())
									.toString())).toString());
			String denyPrefix = new StringBuilder()
					.append(prefix)
					.append("_DENY_")
					.append(
							principal.getNamespace() == null ? "" : new StringBuilder().append(principal.getNamespace()).append("_")
									.toString()).toString();
			sb.append(new StringBuilder()
					.append(" -")
					.append(aclField)
					.append(":")
					.append(
							ClientUtils.escapeQueryChars(new StringBuilder().append(denyPrefix).append(principal.getName())
									.toString())).toString());
		}
		return new SecurityFilter(sb.toString());
	}

	public Metadata encodeAcl(List<ACE> acl) {
		Metadata metadata = new Metadata();
		for (ACE ace : acl) {
			StringBuffer sb = new StringBuffer(prefix);
			if (ace.getType() == ACE.Type.ALLOW) {
				sb.append("_ALLOW");
			}
			if (ace.getType() == ACE.Type.DENY) {
				sb.append("_DENY");
			}
			String principalNamespace = ace.getPrincipal().getNamespace();
			if (!StringUtils.isBlank(principalNamespace)) {
				sb.append(new StringBuilder().append("_").append(principalNamespace).toString());
			}
			String principalName = ace.getPrincipal().getName();
			if (!StringUtils.isBlank(principalName)) {
				sb.append(new StringBuilder().append("_").append(principalName).toString());
				metadata.add(aclField, sb.toString());
			}
		}
		return metadata;
	}
}
