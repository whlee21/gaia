package gaia.crawl.datasource;

import gaia.Defaults;
import gaia.Defaults.Group;
import gaia.utils.StringUtils;
import java.util.Collections;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DataSourceUtils {
	private static final Logger LOG = LoggerFactory.getLogger(DataSourceUtils.class);

	public static int getCrawlDepth(DataSource ds) {
		return ds.getInt("crawl_depth", Defaults.INSTANCE.getInt(Defaults.Group.datasource, "crawl_depth"));
	}

	public static void setCrawlDepth(DataSource ds, int crawlDepth) {
		ds.setProperty("crawl_depth", Integer.valueOf(crawlDepth));
	}

	public static void setCrawlBounds(DataSource ds, DataSourceAPI.Bounds b) {
		ds.setProperty("bounds", b);
	}

	public static DataSourceAPI.Bounds getCrawlBounds(DataSource ds) {
		Object o = ds.getProperty("bounds");
		DataSourceAPI.Bounds defB = null;
		String defBounds = Defaults.INSTANCE.getString(Defaults.Group.datasource, "bounds");
		try {
			defB = DataSourceAPI.Bounds.valueOf(defBounds);
		} catch (Exception e) {
			LOG.warn("Invalid default value of datasource.bounds, resetting to 'none'");
			Defaults.INSTANCE.set(Defaults.Group.datasource, "bounds", "none");
			defB = DataSourceAPI.Bounds.none;
		}
		if (o == null) {
			ds.setProperty("bounds", defB);
			return defB;
		}
		if ((o instanceof DataSourceAPI.Bounds)) {
			return (DataSourceAPI.Bounds) o;
		}
		String s = o.toString();
		DataSourceAPI.Bounds res;
		if (s.trim().isEmpty())
			res = DataSourceAPI.Bounds.none;
		else {
			try {
				res = DataSourceAPI.Bounds.valueOf(s);
			} catch (Exception e) {
				LOG.warn("Invalid crawl_bounds value " + s + ", replaced with " + defB);
				ds.setProperty("bounds", defB);
				res = defB;
			}
		}
		return res;
	}

	public static long getMaxBytes(DataSource ds) {
		return ds.getLong("max_bytes", Defaults.INSTANCE.getInt(Defaults.Group.datasource, "max_bytes"));
	}

	public static void setMaxBytes(DataSource ds, long maxBytes) {
		ds.setProperty("max_bytes", Long.valueOf(maxBytes));
	}

	public static boolean getIndexDirectories(DataSource ds) {
		return ds.getBoolean("index_directories",
				Defaults.INSTANCE.getBoolean(Defaults.Group.datasource, "index_directories", Boolean.valueOf(false)));
	}

	public static void setIndexDirectories(DataSource ds, boolean indexDirectories) {
		ds.setProperty("index_directories", Boolean.valueOf(indexDirectories));
	}

	public static String getPath(DataSource ds) {
		return (String) ds.getProperty("path");
	}

	public static void setPath(DataSource ds, String path) {
		ds.setProperty("path", path);
	}

	public static String getSource(DataSource ds) {
		String res = (String) ds.getProperty("url");
		if (res == null) {
			res = (String) ds.getProperty("path");
		}
		return res;
	}

	public static String getSourceUri(DataSource ds) {
		return getSource(ds);
	}

	public static void setUri(DataSource ds, String url) {
		ds.setProperty("url", url);
	}

	public static List<String> getExcludePattern(DataSource ds) {
		List<String> res = null;
		Object o = ds.getProperty("exclude_paths");
		if (o == null) {
			return Collections.emptyList();
		}
		if ((o instanceof String)) {
			if (((String) o).trim().isEmpty())
				res = Collections.emptyList();
			else {
				res = StringUtils.getList(String.class, o);
			}

			ds.setProperty("exclude_paths", res);
		} else {
			res = (List) o;
		}
		return res;
	}

	public static void setExcludePattern(DataSource ds, List<String> excludePattern) {
		ds.setProperty("exclude_paths", excludePattern);
	}

	public static List<String> getIncludePattern(DataSource ds) {
		List<String> res = null;
		Object o = ds.getProperty("include_paths");
		if (o == null) {
			return Collections.emptyList();
		}
		if ((o instanceof String)) {
			if (((String) o).trim().isEmpty())
				res = Collections.emptyList();
			else {
				res = StringUtils.getList(String.class, o);
			}

			ds.setProperty("include_paths", res);
		} else {
			res = (List) o;
		}
		return res;
	}

	public static void setIncludePattern(DataSource ds, List<String> includePattern) {
		ds.setProperty("include_paths", includePattern);
	}

	public static List<Authentication> getAuthentications(DataSource ds) {
		Object o = ds.getProperty("auth");
		if (o == null) {
			return Collections.emptyList();
		}
		if ((o instanceof List)) {
			if (((List) o).size() == 0) {
				return Collections.emptyList();
			}
			List list = (List) o;
			Object el = list.get(0);
			if ((el instanceof Authentication)) {
				return list;
			}

			return (List) o;
		}
		return Collections.emptyList();
	}

	public static void setAuthentications(DataSource ds, List<Authentication> auths) {
		ds.setProperty("auth", auths);
	}

	public static boolean getVerifyAccess(DataSource ds) {
		Object obj = ds.getProperty("verify_access");
		if (obj == null) {
			return true;
		}
		return StringUtils.getBoolean(obj).booleanValue();
	}
}
