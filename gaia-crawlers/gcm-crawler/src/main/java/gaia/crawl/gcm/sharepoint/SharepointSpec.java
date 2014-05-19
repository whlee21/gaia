package gaia.crawl.gcm.sharepoint;

import gaia.Defaults;
import gaia.crawl.datasource.DataSourceSpec;
import gaia.crawl.datasource.FieldMapping;
import gaia.crawl.datasource.FieldMappingUtil;
import gaia.crawl.gcm.GCMSpec;
import gaia.crawl.gcm.LWEGCMAdaptor;
import gaia.spec.AllowedValuesValidator;
import gaia.spec.EmptyOrDelegateValidator;
import gaia.spec.LooseListValidator;
import gaia.spec.SpecProperty;
import gaia.spec.Validator;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class SharepointSpec extends GCMSpec {
	public static final String USE_SP_SEARCH_VISIBILITY = "use_sp_search_visibility";
	public static final String MY_SITE_BASE_URL = "my_site_base_url";
	public static final String ALIASES = "aliases";
	public static final String KDCSERVER = "kdcserver";
	public static final String DOMAIN = "domain";
	public static String SHAREPOINT_URL = "sharepoint_url";
	public static String INCLUDED_URLS = "included_urls";
	public static String EXCLUDED_URLS = "excluded_urls";
	public static String AUTHORIZATION = "authorization";
	public static String FEED_UNPUBLISHED_DOCUMENTS = "feed_unpublished_documents";
	public static String VISITS_PER_URL = "visits_per_url";
	public static String USE_CHECKSUM_DETECTION = "use_checksum_detection";
	public static String JSON_FILE = "tags_file";

	public static String PUSH_ACLS = "push_acls";
	public static String USERNAME_FORMAT_IN_ACE = "username_format_in_ace";
	public static String GROUPNAME_FORMAT_IN_ACE = "groupname_format_in_ace";

	public static String LDAP_SERVER_HOST_ADDRESS = "ldap_server_host_address";
	public static String LDAP_SERVER_PORT_NUMBER = "ldap_server_port_number";
	public static String LDAP_SERVER_USE_SSL = "ldap_server_use_ssl";
	public static String LDAP_SEARCH_BASE = "ldap_search_base";
	public static String LDAP_AUTH_TYPE = "ldap_auth_type";
	public static String LDAP_READ_GROUPS_TYPE = "ldap_read_groups_type";

	public static String LDAP_CACHE_GROUPS_MEMBERSHIP = "ldap_cache_groups_membership";
	public static String LDAP_CACHE_SIZE = "ldap_cache_size";
	public static String LDAP_CACHE_REFRESH_INTERVAL = "ldap_cache_refresh_interval";

	public SharepointSpec(LWEGCMAdaptor adaptor) {
		super(adaptor);
	}

	protected void addCrawlerSupportedProperties() {
		addSpecProperty(new SpecProperty.Separator("SharePoint settings"));

		addSpecProperty(new SpecProperty(SHAREPOINT_URL, "datasource." + SHAREPOINT_URL, URL.class, null,
				Validator.NOT_BLANK_VALIDATOR, true));

		addSpecProperty(new SpecProperty("username", "datasource.username", String.class, null,
				Validator.NOT_BLANK_VALIDATOR, true));

		addSpecProperty(new SpecProperty("password", "datasource.password", String.class, null,
				Validator.NOT_BLANK_VALIDATOR, true, SpecProperty.HINTS_PASSWORD));

		addSpecProperty(new SpecProperty("domain", "datasource.domain", String.class, "", Validator.NOT_NULL_VALIDATOR,
				true));

		addSpecProperty(new SpecProperty("kdcserver", "datasource.kdcserver", String.class, "",
				Validator.NOT_NULL_VALIDATOR, false, SpecProperty.HINTS_ADVANCED));

		addSpecProperty(new SpecProperty("my_site_base_url", "datasource.my_site_base_url", URL.class, null,
				new EmptyOrDelegateValidator(Validator.URL_VALIDATOR, false), false));

		addSpecProperty(new SpecProperty(INCLUDED_URLS, "datasource." + INCLUDED_URLS, List.class, Collections.emptyList(),
				new LooseListValidator(String.class), false));

		List excludes = Defaults.INSTANCE.getList(Defaults.Group.datasource,
				"gaia.gcm." + DataSourceSpec.Type.sharepoint.toString() + "." + EXCLUDED_URLS, null);
		if (excludes != null) {
			excludes = Collections.emptyList();
		}
		addSpecProperty(new SpecProperty(EXCLUDED_URLS, "datasource." + EXCLUDED_URLS, List.class, excludes,
				new LooseListValidator(String.class), false));

		addSpecProperty(new SpecProperty("use_sp_search_visibility", "datasource.use_sp_search_visibility", Boolean.class,
				Boolean.TRUE, Validator.BOOLEAN_VALIDATOR, false, SpecProperty.HINTS_ADVANCED));

		addSpecProperty(new SpecProperty("aliases", "datasource.aliases", Map.class, null, new URLMapValidator(), false));

		addSpecProperty(new SpecProperty(FEED_UNPUBLISHED_DOCUMENTS, "datasource." + FEED_UNPUBLISHED_DOCUMENTS,
				Boolean.class, Boolean.TRUE, Validator.BOOLEAN_VALIDATOR, false, SpecProperty.HINTS_ADVANCED));

		addSpecProperty(new SpecProperty(VISITS_PER_URL, "datasource." + VISITS_PER_URL, Integer.class,
				Integer.valueOf(10), Validator.NON_NEG_INT_STRING_VALIDATOR, false, SpecProperty.HINTS_ADVANCED));

		addSpecProperty(new SpecProperty(USE_CHECKSUM_DETECTION, "datasource." + USE_CHECKSUM_DETECTION, Boolean.class,
				Boolean.FALSE, Validator.BOOLEAN_VALIDATOR, false, SpecProperty.HINTS_ADVANCED));

		addSpecProperty(new SpecProperty(PUSH_ACLS, "datasource." + PUSH_ACLS, Boolean.class, Boolean.FALSE,
				Validator.BOOLEAN_VALIDATOR, false, SpecProperty.HINTS_ADVANCED));

		addSpecProperty(new SpecProperty("enable_security_trimming", "datasource.enable_security_trimming", Boolean.class,
				Boolean.FALSE, Validator.BOOLEAN_VALIDATOR, false, SpecProperty.HINTS_ADVANCED));

		List usernameFormats = Arrays.asList(new Object[] { "username", "domain\\username", "username@domain" });
		addSpecProperty(new SpecProperty(USERNAME_FORMAT_IN_ACE, "datasource." + USERNAME_FORMAT_IN_ACE, String.class,
				"username", new AllowedValuesValidator(usernameFormats), false, false, SpecProperty.HINTS_ADVANCED,
				usernameFormats));

		List groupnameFormats = Arrays.asList(new Object[] { "groupname", "domain\\groupname", "groupname@domain" });
		addSpecProperty(new SpecProperty(GROUPNAME_FORMAT_IN_ACE, "datasource." + GROUPNAME_FORMAT_IN_ACE, String.class,
				"domain\\groupname", new AllowedValuesValidator(groupnameFormats), false, false, SpecProperty.HINTS_ADVANCED,
				groupnameFormats));

		addSpecProperty(new SpecProperty(JSON_FILE, "datasource." + JSON_FILE, String.class, null,
				Validator.NOOP_VALIDATOR, false, SpecProperty.HINTS_ADVANCED));

		addSpecProperty(new SpecProperty.Separator("LDAP Settings"));
		addSpecProperty(new SpecProperty(LDAP_SERVER_HOST_ADDRESS, "datasource." + LDAP_SERVER_HOST_ADDRESS, String.class,
				null, Validator.NOOP_VALIDATOR, false, SpecProperty.HINTS_ADVANCED));

		addSpecProperty(new SpecProperty(LDAP_SERVER_PORT_NUMBER, "datasource." + LDAP_SERVER_PORT_NUMBER, Integer.class,
				Integer.valueOf(389), Validator.NON_NEG_INT_STRING_VALIDATOR, false, SpecProperty.HINTS_ADVANCED));

		addSpecProperty(new SpecProperty(LDAP_SERVER_USE_SSL, "datasource." + LDAP_SERVER_USE_SSL, Boolean.class,
				Boolean.FALSE, Validator.BOOLEAN_VALIDATOR, false, SpecProperty.HINTS_ADVANCED));

		List authTypes = Arrays.asList(new Object[] { "Simple", "Anonymous" });
		addSpecProperty(new SpecProperty(LDAP_AUTH_TYPE, "datasource." + LDAP_AUTH_TYPE, String.class, "Simple",
				new AllowedValuesValidator(authTypes), false, false, SpecProperty.HINTS_ADVANCED, authTypes));

		addSpecProperty(new SpecProperty(LDAP_SEARCH_BASE, "datasource." + LDAP_SEARCH_BASE, String.class, null,
				Validator.NOOP_VALIDATOR, false, SpecProperty.HINTS_ADVANCED));

		List readGroupsTypes = Arrays.asList(new Object[] { "TOKEN_GROUPS", "IN_CHAIN", "RECURSIVE" });
		addSpecProperty(new SpecProperty(LDAP_READ_GROUPS_TYPE, "datasource." + LDAP_READ_GROUPS_TYPE, String.class,
				"TOKEN_GROUPS", new AllowedValuesValidator(readGroupsTypes), false, false, SpecProperty.HINTS_ADVANCED,
				readGroupsTypes));

		addSpecProperty(new SpecProperty(LDAP_CACHE_GROUPS_MEMBERSHIP, "datasource." + LDAP_CACHE_GROUPS_MEMBERSHIP,
				Boolean.class, Boolean.TRUE, Validator.BOOLEAN_VALIDATOR, false, SpecProperty.HINTS_ADVANCED));

		addSpecProperty(new SpecProperty(LDAP_CACHE_SIZE, "datasource." + LDAP_CACHE_SIZE, Integer.class,
				Integer.valueOf(1000), Validator.NON_NEG_INT_STRING_VALIDATOR, false, SpecProperty.HINTS_ADVANCED));

		addSpecProperty(new SpecProperty(LDAP_CACHE_REFRESH_INTERVAL, "datasource." + LDAP_CACHE_REFRESH_INTERVAL,
				Integer.class, Integer.valueOf(7200), Validator.NON_NEG_INT_STRING_VALIDATOR, false,
				SpecProperty.HINTS_ADVANCED));

		super.addCrawlerSupportedProperties();
	}

	public List<Error> validate(Map<String, Object> map) {
		return super.validate(map);
	}

	public FieldMapping getDefaultFieldMapping() {
		FieldMapping fieldMap = new FieldMapping();
		fieldMap.defineMapping("GCM_Sharepoint:author", "attr_author");
		fieldMap.defineMapping("GCM_last-modified", "lastModified");
		fieldMap.defineMapping("GCM_mimetype", "mimeType");
		fieldMap.defineMapping("GCM_Description", "body");
		fieldMap.defineMapping("GCM_Body", "body");
		fieldMap.defineMapping("GCM_Created Date", "dateCreated");
		fieldMap.defineMapping("GCM_Title", "title");
		fieldMap.defineMapping("GCM_displayurl", "url");
		FieldMappingUtil.addTikaFieldMapping(fieldMap, false);
		return fieldMap;
	}

	private static class URLMapValidator extends Validator {
		public Object cast(SpecProperty specProp, Object value) {
			return value;
		}

		public List<Error> validate(SpecProperty specProp, Object aliases) {
			List res = new ArrayList();
			if ((aliases != null) && ((aliases instanceof Map))) {
				Map aliasMap = (Map) aliases;
				for (Map.Entry entry : aliasMap.entrySet()) {
					checkURL(specProp, res, entry.getKey().toString());
					checkURL(specProp, res, entry.getValue().toString());
				}
			} else if (aliases != null) {
				res.add(new Error(specProp.name, Error.E_INVALID_VALUE, "aliases is a map where key and value are of type url"));
			}
			return res;
		}

		private void checkURL(SpecProperty specProp, List<Error> res, String value) {
			try {
				new URL(value);
			} catch (Exception e) {
				res.add(new Error(specProp.name, Error.E_INVALID_VALUE, "invalid URL " + value + ":" + e.getMessage()));
			}
		}
	}
}
