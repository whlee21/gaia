package gaia.security.ad;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ADHelper {
	private static Logger LOG = LoggerFactory.getLogger(ADHelper.class);
	public static final String ATTR_NAME_DISTINGUISHED_NAME = "distinguishedName";
	public static final String ATTR_NAME_MEMBER_OF = "memberOf";
	public static final String ATTR_NAME_SID = "objectSid;binary";
	public static final String ATTR_PRIMARY_GROUP_ID = "primaryGroupID";
	public static final String ATTR_NAME_UPN = "userPrincipalName";
	public static final String ATTR_SAMACCOUNT_NAME = "sAMAccountName";
	public static final String ATTR_TOKEN_GROUPS = "tokenGroups;binary";
	private static String allUsersFilter = "(objectclass=user)";
	public static final String LDAP_MATCHING_RULE_IN_CHAIN = "(member:1.2.840.113556.1.4.1941:={0})";
	private String providerUrl = null;
	private String principal = null;
	private String credentials = null;

	private String initialContextFactory = "com.sun.jndi.ldap.LdapCtxFactory";
	private String authenticationType = "simple";

	private String userBasedn = null;
	private String groupBaseDn = null;

	private String allGroupsFilter = "(objectclass=group)";
	private String userFilter = "(&(objectclass=user)(userPrincipalName={0}))";

	private int readTimeout = 5000;
	private int connectTimeout = 3000;

	private String referral = "follow";
	private boolean readTokenGroups = true;
	private ADGroupHierarchyHelper helper;

	public ADHelper(String providerUrl, String principal, String credentials) {
		this.providerUrl = providerUrl;
		this.principal = principal;
		this.credentials = credentials;

		if (principal == null) {
			throw new IllegalArgumentException("principal not specified");
		}
		if (!principal.contains("@")) {
			throw new IllegalArgumentException(new StringBuilder().append("Illegal username, no '@' found in:")
					.append(principal).toString());
		}

		userBasedn = getBaseDNFromPrincipal(principal);
		groupBaseDn = getBaseDNFromPrincipal(principal);
	}

	private String getBaseDNFromPrincipal(String principal) {
		String domainPart = principal.split("@")[1];
		String[] dcArray = domainPart.split("\\.");
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < dcArray.length; i++) {
			sb.append("DC=").append(dcArray[i]);
			if (i < dcArray.length - 1) {
				sb.append(",");
			}
		}
		return sb.toString();
	}

	public void readGroups() {
		LOG.info("Reading and caching all AD groups");
		long startTime = System.currentTimeMillis();
		int num = 0;
		DirContext ctx = null;
		try {
			ctx = getDirContext();
			ADGroupHierarchyHelper newHelper = new ADGroupHierarchyHelper();

			SearchControls controls = new SearchControls();
			controls.setSearchScope(2);
			controls.setReturningAttributes(new String[] { "objectSid;binary", "distinguishedName", "memberOf" });

			LOG.info("Querying AD: base_dn='{}', filter='{}', attrs='{}'", new String[] { groupBaseDn, allGroupsFilter,
					Arrays.toString(controls.getReturningAttributes()) });

			Object response = ctx.search(groupBaseDn, allGroupsFilter, controls);

			Enumeration e = (Enumeration) response;
			while (e.hasMoreElements()) {
				num++;
				SearchResult result = (SearchResult) e.nextElement();

				if (result.getAttributes().get("distinguishedName") != null) {
					String dn = (String) result.getAttributes().get("distinguishedName").get();

					if (result.getAttributes().get("objectSid;binary") != null) {
						String sid = convertSID((byte[]) result.getAttributes().get("objectSid;binary").get());

						ADGroup group = newHelper.getOrCreateGroup(dn);
						group.setSid(sid);

						Attribute parentGroupsAttribute = result.getAttributes().get("memberOf");

						if (parentGroupsAttribute != null) {
							Enumeration parentGroups = parentGroupsAttribute.getAll();
							while (parentGroups.hasMoreElements()) {
								String parentName = (String) parentGroups.nextElement();
								ADGroup parent = newHelper.getOrCreateGroup(parentName);
								parent.addChildren(group);
								group.addParent(parent);
							}
						}
					}
				}
			}
			newHelper.rebuild();
			helper = newHelper;
		} catch (NamingException e) {
			LOG.warn("Could not retrieve group information from LDAP.", e);
		} finally {
			if (ctx != null)
				try {
					ctx.close();
				} catch (Throwable t) {
				}
		}
		long endTime = System.currentTimeMillis();
		LOG.info(new StringBuilder().append("Reading ").append(num).append(" AD groups took ")
				.append(endTime - startTime).append(" ms").toString());
	}

	private DirContext getDirContext() throws NamingException {
		Hashtable<String, String> env = new Hashtable<String, String>(11);
		env.put("java.naming.provider.url", providerUrl);
		env.put("java.naming.security.authentication", authenticationType);
		env.put("com.sun.jndi.ldap.read.timeout", Integer.toString(readTimeout));
		env.put("com.sun.jndi.ldap.connect.timeout", Integer.toString(connectTimeout));
		env.put("java.naming.factory.initial", initialContextFactory);
		if (principal != null) {
			env.put("java.naming.security.principal", principal);
		}
		if (credentials != null) {
			env.put("java.naming.security.credentials", credentials);
		}

		if (referral != null) {
			env.put("java.naming.referral", referral);
		}

		DirContext dirContext = new InitialDirContext(env);
		return dirContext;
	}

	public Set<String> getSidsForUser(String userName) throws NamingException {
		long startTime = System.currentTimeMillis();

		HashSet<String> sids = new HashSet<String>();
		DirContext ctx = getDirContext();
		try {
			String userSearchfilter = new MessageFormat(userFilter).format(new Object[] { userName });

			SearchControls controls = new SearchControls();
			controls.setSearchScope(2);
			controls.setReturningAttributes(new String[] { "objectSid;binary", "distinguishedName", "memberOf",
					"primaryGroupID" });

			controls.setCountLimit(1L);

			LOG.info("Querying AD: base_dn='{}', filter='{}', attrs='{}'", new String[] { userBasedn, userSearchfilter,
					Arrays.toString(controls.getReturningAttributes()) });

			Object response = ctx.search(userBasedn, userSearchfilter, controls);

			Enumeration e = (Enumeration) response;
			while (e.hasMoreElements()) {
				SearchResult result = (SearchResult) e.nextElement();

				if (result.getAttributes().get("objectSid;binary") != null) {
					String sidUser = convertSID((byte[]) result.getAttributes().get("objectSid;binary").get());

					sids.add(sidUser);

					if (result.getAttributes().get("primaryGroupID") != null) {
						sids.add(getPrimaryGroupSid(sidUser, result.getAttributes().get("primaryGroupID").get().toString()));
					}

				}

				if (helper != null) {
					Attribute groupsAttribute = result.getAttributes().get("memberOf");
					if (groupsAttribute != null) {
						Enumeration groups = groupsAttribute.getAll();

						while (groups.hasMoreElements()) {
							String group = (String) groups.nextElement();
							sids.addAll(helper.getEffectiveSidsforGroupDN(group));
						}
					}
				} else {
					String dn = result.getAttributes().get("distinguishedName").get().toString();
					if (readTokenGroups) {
						sids.addAll(getGroupsSidsUsingTokenGroupsAttribute(dn));
					} else
						sids.addAll(getGroupsSidsUsingInChainFilter(dn));
				}
			}
		} finally {
			ctx.close();
		}

		long endTime = System.currentTimeMillis();
		LOG.info(new StringBuilder().append("Retrieving all Active Directory SIDs for user ").append(userName)
				.append(" took ").append(endTime - startTime).append(" ms").toString());
		LOG.info(new StringBuilder().append("SIDs for user ").append(userName).append(" - ").append(sids.toString())
				.toString());

		return sids;
	}

	private Set<String> getGroupsSidsUsingInChainFilter(String dn) throws NamingException {
		long startTime = System.currentTimeMillis();
		int num = 0;

		Set<String> groupsSids = new HashSet<String>();

		DirContext ctx = getDirContext();
		try {
			String groupsSearchFilter = new MessageFormat("(member:1.2.840.113556.1.4.1941:={0})")
					.format(new Object[] { dn });

			SearchControls controls = new SearchControls();
			controls.setSearchScope(2);
			controls.setReturningAttributes(new String[] { "objectSid;binary", "distinguishedName" });

			LOG.info("Querying AD: base_dn='{}', filter='{}', attrs='{}'", new String[] { groupBaseDn, groupsSearchFilter,
					Arrays.toString(controls.getReturningAttributes()) });

			Object response = ctx.search(groupBaseDn, groupsSearchFilter, controls);

			Enumeration e = (Enumeration) response;

			while (e.hasMoreElements()) {
				num++;
				SearchResult result = (SearchResult) e.nextElement();

				if (result.getAttributes().get("objectSid;binary") != null) {
					String sidGroup = convertSID((byte[]) result.getAttributes().get("objectSid;binary").get());
					groupsSids.add(sidGroup);
				}
			}
		} finally {
			ctx.close();
		}

		long endTime = System.currentTimeMillis();
		LOG.info(new StringBuilder().append("Retrieving ").append(num).append(" AD groups for user ").append(dn)
				.append(" using IN_CHAIN filter took ").append(endTime - startTime).append(" ms").toString());

		return groupsSids;
	}

	private Set<String> getGroupsSidsUsingTokenGroupsAttribute(String dn) throws NamingException {
		long startTime = System.currentTimeMillis();

		Set<String> sids = new HashSet<String>();

		DirContext ctx = getDirContext();
		try {
			SearchControls controls = new SearchControls();
			controls.setSearchScope(0);
			controls.setReturningAttributes(new String[] { "tokenGroups;binary" });

			LOG.info("Querying AD: base_dn='{}', filter='{}', attrs='{}'",
					new String[] { dn, allUsersFilter, Arrays.toString(controls.getReturningAttributes()) });

			Object response = ctx.search(dn, allUsersFilter, controls);

			Enumeration en = (Enumeration) response;
			while (en.hasMoreElements()) {
				SearchResult result = (SearchResult) en.nextElement();
				Attributes attrs = result.getAttributes();
				if (attrs != null)
					for (NamingEnumeration ae = attrs.getAll(); ae.hasMore();) {
						Attribute attr = (Attribute) ae.next();
						for (NamingEnumeration e = attr.getAll(); e.hasMore();) {
							byte[] sid = (byte[]) e.next();
							String sidString = convertSID(sid);
							sids.add(sidString);
						}
					}
			}
		} finally {
			ctx.close();
		}

		long endTime = System.currentTimeMillis();
		LOG.info(new StringBuilder().append("Retrieving ").append(sids.size()).append(" tokenGroups sids for user ")
				.append(dn).append(" took ").append(endTime - startTime).append(" ms").toString());

		return sids;
	}

	public List<String> getUsers() throws NamingException {
		long startTime = System.currentTimeMillis();

		List<String> results = new ArrayList<String>();

		DirContext ctx = getDirContext();
		try {
			SearchControls controls = new SearchControls();
			controls.setSearchScope(2);
			controls.setReturningAttributes(new String[] { "userPrincipalName" });

			LOG.info("Querying AD: base_dn='{}', filter='{}', attrs='{}'", new String[] { userBasedn, allUsersFilter,
					Arrays.toString(controls.getReturningAttributes()) });

			Object response = ctx.search(userBasedn, allUsersFilter, controls);

			Enumeration e = (Enumeration) response;
			while (e.hasMoreElements()) {
				SearchResult result = (SearchResult) e.nextElement();

				if (result.getAttributes().get("userPrincipalName") != null) {
					String upn = (String) result.getAttributes().get("userPrincipalName").get();
					results.add(upn);
				}
			}
		} finally {
			ctx.close();
		}

		long endTime = System.currentTimeMillis();
		LOG.info(new StringBuilder().append("Retrieving all users from active directory took ")
				.append(endTime - startTime).append(" ms").toString());

		return results;
	}

	public static void main(String[] args) throws NamingException {
		if (args.length < 5) {
			System.out
					.println("Usage: ADHelper <cache|donotcache> <ldap-url> <ldap-user> <ldap-password> <user-to-check> [user-base-dn] [group-base-dn]");
			return;
		}
		String mode = args[0];
		if ((!"cache".equals(mode)) && (!"donotcache".equals(mode))) {
			System.out.println("First arg should be either 'cache' or 'donotcache'");
			return;
		}
		ADHelper helper;
		if (args.length == 5) {
			helper = new ADHelper(args[1], args[2], args[3]);
		} else if (args.length == 6) {
			helper = new ADHelper(args[1], args[2], args[3]);
			helper.setUserBasedn(args[5]);
		} else {
			helper = new ADHelper(args[1], args[2], args[3]);
			helper.setUserBasedn(args[5]);
			helper.setUserBasedn(args[6]);
		}
		if (mode.equals("cache")) {
			helper.readGroups();
		}
		Set<String> sids = new TreeSet<String>(helper.getSidsForUser(args[4]));
		System.out.println(new StringBuilder().append("User sids:").append(sids).toString());
	}

	public static String convertSID(byte[] value) {
		StringBuilder sid = new StringBuilder("S-");
		int version = value[0];
		sid.append(version);
		int subAuthorityCount = value[1];
		long identifierAuthority = value[7];
		sid.append("-").append(Long.toString(identifierAuthority));

		for (int i = 0; i < subAuthorityCount; i++) {
			long l = getUnisgnedInt(value, i);
			sid.append("-").append(l);
		}
		return sid.toString();
	}

	public static String getPrimaryGroupSid(String sid, String rid) {
		int i = sid.lastIndexOf("-");
		if (i == -1) {
			throw new RuntimeException(new StringBuilder().append("Illegal sid: ").append(sid).toString());
		}
		StringBuilder groupSid = new StringBuilder(sid.substring(0, i + 1));
		groupSid.append(rid);
		return groupSid.toString();
	}

	private static long getUnisgnedInt(byte[] value, int i) {
		long rid = 0L;
		for (int k = 3; k > -1; k--) {
			rid <<= 8;
			rid += (value[(i * 4 + 8 + k)] & 0xFF);
		}
		return rid;
	}

	public void setAllGroupsFilter(String allGroupsFilter) {
		this.allGroupsFilter = allGroupsFilter;
	}

	public void setAuthenticationType(String authenticationType) {
		this.authenticationType = authenticationType;
	}

	public void setUserBasedn(String userBasedn) {
		this.userBasedn = userBasedn;
	}

	public void setGroupBaseDn(String groupBaseDn) {
		this.groupBaseDn = groupBaseDn;
	}

	public void setInitialContextFactory(String initialContextFactory) {
		this.initialContextFactory = initialContextFactory;
	}

	public void setUserFilter(String userSearch) {
		userFilter = userSearch;
	}

	public String getUserFilter() {
		return userFilter;
	}

	public void setReadTimeout(Integer readTimeout) {
		readTimeout = readTimeout.intValue();
	}

	public void setConnectTimeout(Integer connectTimeout) {
		connectTimeout = connectTimeout.intValue();
	}

	public void setReferral(String referral) {
		this.referral = referral;
	}

	public void setReadTokenGroups(Boolean readTokenGroups) {
		readTokenGroups = readTokenGroups.booleanValue();
	}
}
