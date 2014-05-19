package gaia.crawl.aperture;

import gaia.api.Error;
import gaia.Defaults;
import gaia.crawl.datasource.DataSourceSpec;
import gaia.crawl.http.protocol.HttpProtocol;
import gaia.crawl.http.protocol.HttpProtocolConfig;
import gaia.crawl.http.protocol.ProtocolOutput;
import gaia.crawl.http.protocol.ProtocolStatus;
import gaia.spec.SpecProperty;
import gaia.spec.Validator;
import gaia.utils.StringUtils;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WebSpec extends ApertureDataSourceSpec {
	static final Logger LOG = LoggerFactory.getLogger(WebSpec.class);

	public WebSpec() {
		super(DataSourceSpec.Category.Web.toString());
	}

	protected void addCrawlerSupportedProperties() {
		addSpecProperty(new SpecProperty.Separator("Web root URL"));
		addSpecProperty(new SpecProperty("url", "datasource.url", String.class, null, Validator.URL_VALIDATOR, true));

		addSpecProperty(new SpecProperty.Separator("Web settings"));
		addSpecProperty(new SpecProperty("ignore_robots", "datasource.ignore_robots", Boolean.class,
				Boolean.valueOf(Defaults.INSTANCE.getBoolean(Defaults.Group.datasource, "ignore_robots")),
				Validator.BOOLEAN_VALIDATOR, false));

		addSpecProperty(new SpecProperty.Separator("Web proxy settings"));
		addSpecProperty(new SpecProperty("proxy_host", "datasource.proxy_host", String.class, null,
				Validator.TREAT_BLANK_AS_NULL_VALIDATOR, false));

		addSpecProperty(new SpecProperty("proxy_username", "datasource.proxy_username", String.class, null,
				Validator.TREAT_BLANK_AS_NULL_VALIDATOR, false));

		addSpecProperty(new SpecProperty("proxy_password", "datasource.proxy_password", String.class, null,
				Validator.TREAT_BLANK_AS_NULL_VALIDATOR, false));

		addSpecProperty(new SpecProperty("proxy_port", "datasource.proxy_port", Integer.class, null,
				Validator.INT_STRING_VALIDATOR, false));

		addSpecProperty(new SpecProperty.Separator("Web authentication"));
		addSpecProperty(new SpecProperty("auth", "datasource.auth", List.class, Collections.emptyList(),
				Validator.AUTHENTICATION_VALIDATOR, false));

		addCommonApertureProperties();
		addSpecProperty(new SpecProperty("max_retries", "datasource.max_retries", Integer.class,
				Integer.valueOf(Defaults.INSTANCE.getInt(Defaults.Group.datasource, "max_retries", Integer.valueOf(3))),
				Validator.INT_STRING_VALIDATOR, false, SpecProperty.HINTS_ADVANCED));

		overwriteBoundaryLimits();
	}

	private void overwriteBoundaryLimits() {
		getSpecProperty("bounds").defaultValue = Defaults.INSTANCE.getString(Defaults.Group.datasource, "gaia.aperture."
				+ DataSourceSpec.Type.web.toString() + "." + "bounds");
		getSpecProperty("crawl_depth").defaultValue = Defaults.INSTANCE.getString(Defaults.Group.datasource,
				"gaia.aperture." + DataSourceSpec.Type.web.toString() + "." + "crawl_depth");

		List<String> excludes = Defaults.INSTANCE.getList(Defaults.Group.datasource, "gaia.aperture."
				+ DataSourceSpec.Type.web.toString() + "." + "exclude_paths", null);
		if (excludes != null)
			getSpecProperty("exclude_paths").defaultValue = excludes;
	}

	public List<Error> validate(Map<String, Object> map) {
		List<Error> errors = super.validate(map);
		String url = (String) map.get("url");

		if (url != null) {
			url = (String) Validator.URL_VALIDATOR.cast(null, url);
		}

		if ((map.get("verify_access") != null) && (!StringUtils.getBoolean(map.get("verify_access")).booleanValue())) {
			return errors;
		}

		if (errors.isEmpty()) {
			reachabilityCheck(map, url, errors);
		}
		return errors;
	}

	private void reachabilityCheck(Map<String, Object> map, String rootUrl, List<Error> errors) {
		map = cast(new HashMap<String, Object>(map));
		HttpProtocolConfig cfg = new HttpProtocolConfig(map);

		if ((cfg.getTimeout() < 0) || (cfg.getTimeout() > 60000)) {
			cfg.setTimeout(5000);
		}
		HttpProtocol proto = new HttpProtocol(cfg, null);
		try {
			ProtocolOutput out = proto.getProtocolOutput(new URL(rootUrl), -1L, HttpProtocol.Method.HEAD);
			if (out.getStatus().code != ProtocolStatus.Code.OK) {
				errors.add(new Error("url", Error.E_INVALID_VALUE, "root URL is unreachable: " + out.getStatus().toString()));
			}
		} catch (MalformedURLException e) {
			LOG.warn("Could not validate " + rootUrl, e);
		}
	}
}
