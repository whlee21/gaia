package gaia.crawl.aperture;

import gaia.api.Error;
import gaia.Defaults;
import gaia.crawl.CrawlerUtils;
import gaia.crawl.datasource.DataSourceSpec;
import gaia.spec.SpecProperty;
import gaia.spec.Validator;
import gaia.utils.StringUtils;

import java.io.File;
import java.util.List;
import java.util.Map;

public class FileSystemSpec extends ApertureDataSourceSpec {
	public FileSystemSpec() {
		super(DataSourceSpec.Category.FileSystem.toString());
	}

	protected void addCrawlerSupportedProperties() {
		addSpecProperty(new SpecProperty.Separator("file system root"));
		addSpecProperty(new SpecProperty("path", "datasource.path", String.class, null, Validator.NOT_BLANK_VALIDATOR, true));

		addSpecProperty(new SpecProperty("url", "datasource.url", String.class, null, Validator.NOOP_VALIDATOR, false,
				true, SpecProperty.HINTS_DEFAULT));

		addSpecProperty(new SpecProperty("follow_links", "datasource.follow_links", Boolean.class,
				Boolean.valueOf(Defaults.INSTANCE.getBoolean(Defaults.Group.datasource, "follow_links")),
				Validator.BOOLEAN_VALIDATOR, false, SpecProperty.HINTS_ADVANCED));

		addCommonApertureProperties();

		overwriteBoundaryLimits();
	}

	private void overwriteBoundaryLimits() {
		List<String> excludes = Defaults.INSTANCE.getList(Defaults.Group.datasource,
				"gaia.aperture." + DataSourceSpec.Type.file.toString() + "." + "exclude_paths", null);
		if (excludes != null)
			getSpecProperty("exclude_paths").defaultValue = excludes;
	}

	public List<Error> validate(Map<String, Object> m) {
		List<Error> errors = super.validate(m);
		String path = (String) m.get("path");

		if ((m.get("verify_access") != null) && (!StringUtils.getBoolean(m.get("verify_access")).booleanValue())) {
			return errors;
		}

		if (errors.isEmpty()) {
			CrawlerUtils.fileReachabilityCheck(path, errors);
		}
		return errors;
	}

	public Map<String, Object> cast(Map<String, Object> input) {
		Map<String, Object> res = super.cast(input);
		res.remove("url");
		String path = (String) res.get("path");
		File f = CrawlerUtils.resolveRelativePath(path);
		if (f != null)
			res.put("url", f.toURI().toString());
		else {
			res.put("url", path);
		}
		return res;
	}
}
