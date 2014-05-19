package gaia.crawl.fs.ds;

import gaia.api.Error;
import gaia.Defaults;
import gaia.crawl.datasource.DataSource;
import gaia.crawl.fs.FS;
import gaia.crawl.fs.ftp.FtpFS;
import gaia.spec.SpecProperty;
import gaia.spec.Validator;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Map;

import com.enterprisedt.net.ftp.FileTransferClient;

public class FtpSpec extends FSSpec {
	public static final String DEFAULT_USER_KEY = "ftp.default.user";
	public static final String DEFAULT_PASSWORD_KEY = "ftp.default.password";

	protected void addCrawlerSupportedProperties() {
		addCommonFSProperties();
		addSpecProperty(new SpecProperty.Separator("FTP authentication"));
		addSpecProperty(new SpecProperty("username", "datasource.username", String.class, Defaults.INSTANCE.getString(
				Defaults.Group.crawlers, DEFAULT_USER_KEY), Validator.NOT_NULL_VALIDATOR, false));

		addSpecProperty(new SpecProperty("password", "datasource.password", String.class, Defaults.INSTANCE.getString(
				Defaults.Group.crawlers, DEFAULT_PASSWORD_KEY), Validator.NOT_NULL_VALIDATOR, false, new String[] {
				SpecProperty.Hint.summary.toString(), SpecProperty.Hint.secret.toString() }));

		addGeneralProperties();
	}

	public List<Error> validate(Map<String, Object> map) {
		map.put("max_threads", Integer.valueOf(1));
		String user = (String) map.get("username");
		if ((user == null) || (user.trim().length() == 0)) {
			map.put("username", Defaults.INSTANCE.getString(Defaults.Group.ftp, "username"));
			map.put("password", Defaults.INSTANCE.getString(Defaults.Group.ftp, "password"));
		}
		List<Error> errors = super.validate(map);
		return errors;
	}

	public String getFSPrefix() {
		return "ftp://";
	}

	public FS createFS(DataSource ds) throws Exception {
		return new FtpFS(ds);
	}

	protected void reachabilityCheck(Map<String, Object> map, String url, List<Error> errors) {
		String user = (String) map.get("username");
		String pass = (String) map.get("password");
		URL u = null;
		try {
			u = new URL(url);
		} catch (MalformedURLException mue) {
			errors.add(new Error("url", "error.invalid.value", mue.toString()));
			return;
		}
		try {
			FileTransferClient ftp = FtpFS.createClient(u, user, pass);
			ftp.disconnect();
		} catch (Throwable t) {
			errors.add(new Error("url", "error.invalid.value", t.toString()));
		}
	}
}
