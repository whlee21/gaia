package gaia.crawl.fs.ds;

import gaia.api.Error;
import gaia.crawl.datasource.DataSource;
import gaia.crawl.fs.FS;
import gaia.crawl.fs.FSObject;
import gaia.security.ad.ADHelper;
import gaia.spec.BooleanValidator;
import gaia.spec.EmptyOrDelegateValidator;
import gaia.spec.RegexValidator;
import gaia.spec.SpecProperty;
import gaia.spec.Validator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CIFSFSSpec extends AuthFSSpec {
	private static Logger LOG = LoggerFactory.getLogger(CIFSFSSpec.class);

	public static String AD_URL = "ad_url";
	public static String AD_PRINCIPAL = "ad_user_principal_name";
	public static String AD_CREDENTIALS = "ad_credentials";
	public static String AD_USER_FILTER = "ad_user_filter";
	public static String AD_GROUP_FILTER = "ad_group_filter";
	public static String AD_USER_BASE_DN = "ad_user_base_dn";
	public static String AD_GROUP_BASE_DN = "ad_group_base_dn";
	public static String AD_CONTEXT_FACTORY = "ad_context_factory";
	public static String AD_SECURITY_AUTH = "ad_security_authentication";
	public static String AD_CACHE_GROUPS = "ad_cache_groups";
	public static String AD_READ_TOKEN_GROUPS = "ad_read_token_groups";
	public static String AD_READ_TIMEOUT = "ad_read_timeout";
	public static String AD_CONNECT_TIMEOUT = "ad_connect_timeout";
	public static String AD_REFERRAL = "ad_referral";

	public CIFSFSSpec() {
		addActiveDirectoryProperties();
	}

	public List<Error> validate(Map<String, Object> map) {
		List<Error> errors = super.validate(map);

		if (!StringUtils.isBlank((String) map.get(AD_URL))) {
			if (StringUtils.isBlank((String) map.get(AD_PRINCIPAL)))
				errors.add(new Error(getSpecProperty(AD_PRINCIPAL).name, Error.E_NULL_VALUE));
			if (StringUtils.isBlank((String) map.get(AD_CREDENTIALS))) {
				errors.add(new Error(getSpecProperty(AD_CREDENTIALS).name, Error.E_NULL_VALUE));
			}
		}
		return errors;
	}

	protected void addCrawlerSupportedProperties() {
		addSpecProperty(new SpecProperty.Separator("SMB settings"));
		addSpecProperty(new SpecProperty("windows_domain", "datasource.windows_domain", String.class, null,
				Validator.NOT_NULL_VALIDATOR, false));

		addCommonFSProperties();
		addGeneralProperties();
	}

	private void addActiveDirectoryProperties() {
		addSpecProperty(new SpecProperty.Separator("Active Directory settings"));

		addSpecProperty(new SpecProperty("enable_security_trimming", "datasource.enable_security_trimming", Boolean.class,
				Boolean.FALSE, Validator.BOOLEAN_VALIDATOR, false, SpecProperty.HINTS_ADVANCED));

		addSpecProperty(new SpecProperty(AD_URL, "Format: ldap://hostname:389 or ldaps://hostname:636 ", String.class,
				null, new EmptyOrDelegateValidator(new RegexValidator("ldaps?://.+"), true), false));

		addSpecProperty(new SpecProperty(AD_PRINCIPAL, "datasource." + AD_PRINCIPAL, String.class, null,
				new EmptyOrDelegateValidator(new RegexValidator(".+@.+"), true), false));

		addSpecProperty(new SpecProperty(AD_CREDENTIALS, "datasource." + AD_CREDENTIALS, String.class, null,
				Validator.TREAT_BLANK_AS_NULL_VALIDATOR, false, SpecProperty.HINTS_PASSWORD));

		addSpecProperty(new SpecProperty(AD_USER_FILTER, "datasource." + AD_USER_FILTER, String.class,
				"(&(objectclass=user)(sAMAccountName={0}))", Validator.TREAT_BLANK_AS_NULL_VALIDATOR, false,
				SpecProperty.HINTS_ADVANCED));

		addSpecProperty(new SpecProperty(AD_GROUP_FILTER, "datasource." + AD_GROUP_FILTER, String.class,
				"(&(objectclass=group))", Validator.TREAT_BLANK_AS_NULL_VALIDATOR, false, SpecProperty.HINTS_ADVANCED));

		addSpecProperty(new SpecProperty(AD_USER_BASE_DN, "datasource." + AD_USER_BASE_DN, String.class, null,
				Validator.TREAT_BLANK_AS_NULL_VALIDATOR, false, SpecProperty.HINTS_ADVANCED));

		addSpecProperty(new SpecProperty(AD_GROUP_BASE_DN, "datasource." + AD_GROUP_BASE_DN, String.class, null,
				Validator.TREAT_BLANK_AS_NULL_VALIDATOR, false, SpecProperty.HINTS_ADVANCED));

		addSpecProperty(new SpecProperty(AD_CACHE_GROUPS, "datasource." + AD_CACHE_GROUPS, Boolean.class, Boolean.FALSE,
				Validator.BOOLEAN_VALIDATOR, false, SpecProperty.HINTS_ADVANCED));

		addSpecProperty(new SpecProperty(AD_READ_TOKEN_GROUPS, "datasource." + AD_READ_TOKEN_GROUPS, Boolean.class,
				Boolean.TRUE, Validator.BOOLEAN_VALIDATOR, false, SpecProperty.HINTS_ADVANCED));

		addSpecProperty(new SpecProperty(AD_CONTEXT_FACTORY, "Initial Context Factory", String.class,
				"com.sun.jndi.ldap.LdapCtxFactory", Validator.TREAT_BLANK_AS_NULL_VALIDATOR, false, SpecProperty.HINTS_ADVANCED));

		addSpecProperty(new SpecProperty(AD_SECURITY_AUTH, "Security Authentication", String.class, "simple",
				Validator.TREAT_BLANK_AS_NULL_VALIDATOR, false, SpecProperty.HINTS_ADVANCED));

		addSpecProperty(new SpecProperty(AD_REFERRAL, "datasource." + AD_REFERRAL, String.class, "follow",
				Validator.TREAT_BLANK_AS_NULL_VALIDATOR, false, SpecProperty.HINTS_ADVANCED));

		addSpecProperty(new SpecProperty(AD_READ_TIMEOUT, "datasource." + AD_READ_TIMEOUT, Integer.class,
				Integer.valueOf(5000), Validator.NON_NEG_INT_STRING_VALIDATOR, false, SpecProperty.HINTS_ADVANCED));

		addSpecProperty(new SpecProperty(AD_CONNECT_TIMEOUT, "datasource." + AD_CONNECT_TIMEOUT, Integer.class,
				Integer.valueOf(3000), Validator.NON_NEG_INT_STRING_VALIDATOR, false, SpecProperty.HINTS_ADVANCED));
	}

	protected void reachabilityCheck(final Map<String, Object> map, final String url, List<Error> errors) {
		Callable task = new Callable() {
			List<Error> errors = new ArrayList<Error>();

			public List<Error> call() {
				DataSource ds = new DataSource("invalid", "invalid", "invalid");
				ds.setProperty("url", url);
				ds.setProperty("username", map.get("username"));
				ds.setProperty("password", map.get("password"));
				ds.setProperty("windows_domain", map.get("windows_domain"));
				try {
					CIFSFS throwaway = new CIFSFS(ds);
					FSObject resource = throwaway.get(url);
					if ((resource.isDirectory()) && (!url.endsWith("/")))
						notReachable(errors, "directory must end with a '/'");
				} catch (Throwable t) {
					LOG.warn("Could not validate CIFS data source", t);
					notReachable(errors, t.getMessage());
				}
				try {
					if (!StringUtils.isBlank((String) map.get(CIFSFSSpec.AD_URL))) {
						String principal = (String) map.get(CIFSFSSpec.AD_PRINCIPAL);
						ADHelper adHelper = CIFSFSSpec.getAdHelper(map);
						String username = principal;
						if (adHelper.getUserFilter().toLowerCase(Locale.ROOT).contains("sAMAccountName".toLowerCase(Locale.ROOT))) {
							username = principal.split("@")[0];
						}
						adHelper.getSidsForUser(username);
					}
				} catch (Throwable t) {
					LOG.warn("Could not validate Active Directory params", t);
					errors.add(new Error(CIFSFSSpec.AD_URL, Error.E_INVALID_VALUE, "Active Directory error: "
							+ t.getMessage()));
				}

				return errors;
			}
		};
		ExecutorService es = Executors.newSingleThreadExecutor();
		CompletionService ecs = new ExecutorCompletionService(es);
		Future future = ecs.submit(task);
		try {
			errors.addAll((Collection) future.get(Long.MAX_VALUE, TimeUnit.SECONDS));
		} catch (TimeoutException t) {
			errors.add(new Error(null, Error.E_INVALID_VALUE, "Data not reachable: timeout"));
		} catch (InterruptedException e) {
			errors.add(new Error(null, Error.E_INVALID_VALUE, "Data not reachable: interrupted"));
		} catch (ExecutionException e) {
			errors.add(new Error(null, Error.E_INVALID_VALUE, "Data not reachable: unknown" + e.getMessage()));
		} finally {
			es.shutdown();
		}
	}

	private void notReachable(List<Error> errors, String reason) {
		errors.add(new Error("url", Error.E_INVALID_VALUE, "root URL is unreachable: " + reason));
	}

	public String getFSPrefix() {
		return "smb://";
	}

	public FS createFS(DataSource ds) throws IOException {
		return new CIFSFS(ds);
	}

	public static ADHelper getAdHelper(Map<String, Object> dsProps) {
		ADHelper helper = new ADHelper((String) dsProps.get(AD_URL), (String) dsProps.get(AD_PRINCIPAL),
				(String) dsProps.get(AD_CREDENTIALS));

		if (!StringUtils.isBlank((String) dsProps.get(AD_USER_BASE_DN)))
			helper.setUserBasedn((String) dsProps.get(AD_USER_BASE_DN));
		if (!StringUtils.isBlank((String) dsProps.get(AD_GROUP_BASE_DN)))
			helper.setGroupBaseDn((String) dsProps.get(AD_GROUP_BASE_DN));
		if (!StringUtils.isBlank((String) dsProps.get(AD_GROUP_FILTER)))
			helper.setAllGroupsFilter((String) dsProps.get(AD_GROUP_FILTER));
		if (!StringUtils.isBlank((String) dsProps.get(AD_USER_FILTER))) {
			helper.setUserFilter((String) dsProps.get(AD_USER_FILTER));
		}
		if (!StringUtils.isBlank((String) dsProps.get(AD_SECURITY_AUTH)))
			helper.setAuthenticationType((String) dsProps.get(AD_SECURITY_AUTH));
		if (!StringUtils.isBlank((String) dsProps.get(AD_CONTEXT_FACTORY))) {
			helper.setInitialContextFactory((String) dsProps.get(AD_CONTEXT_FACTORY));
		}
		if (!StringUtils.isBlank((String) dsProps.get(AD_REFERRAL))) {
			helper.setReferral((String) dsProps.get(AD_REFERRAL));
		}
		Object obj = dsProps.get(AD_READ_TIMEOUT);
		if (obj != null) {
			Integer i = Integer.valueOf(Integer.parseInt(String.valueOf(obj)));
			helper.setReadTimeout(i);
		}

		obj = dsProps.get(AD_CONNECT_TIMEOUT);
		if (obj != null) {
			Integer i = Integer.valueOf(Integer.parseInt(String.valueOf(obj)));
			helper.setConnectTimeout(i);
		}

		obj = dsProps.get(AD_READ_TOKEN_GROUPS);
		Boolean readTokeGroups = BooleanValidator.cast(obj);
		if (!readTokeGroups.booleanValue()) {
			helper.setReadTokenGroups(Boolean.FALSE);
		}

		return helper;
	}

	static {
		System.setProperty("jcifs.smb.client.responseTimeout", "9000");
	}
}
