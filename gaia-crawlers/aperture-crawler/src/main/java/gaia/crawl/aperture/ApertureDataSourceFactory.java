package gaia.crawl.aperture;

import gaia.api.Error;
import gaia.crawl.CrawlerController;
import gaia.crawl.CrawlerUtils;
import gaia.crawl.DataSourceFactory;
import gaia.crawl.DataSourceFactoryException;
import gaia.crawl.datasource.DataSource;
import gaia.crawl.datasource.DataSourceSpec;
import gaia.crawl.datasource.DataSourceUtils;

import java.io.File;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ApertureDataSourceFactory extends DataSourceFactory {
	private static final Logger LOG  = LoggerFactory.getLogger(ApertureDataSourceFactory.class);
	public ApertureDataSourceFactory(CrawlerController cc) {
		super(cc);

		types.put(DataSourceSpec.Type.file.toString(), new FileSystemSpec());
		types.put(DataSourceSpec.Type.web.toString(), new WebSpec());
	}

	public DataSource create(Map<String, Object> m, String collection) throws DataSourceFactoryException {
		String dsType = (String) m.get("type");
		if (dsType == null) {
			throw new DataSourceFactoryException("Missing datasource type", new Error("type", Error.E_MISSING_VALUE));
		}

		if (dsType.equals(DataSourceSpec.Type.file.toString())) {
			String path = (String) m.get("path");
			if (path != null) {
				File file = CrawlerUtils.resolveRelativePath(path);
				String uri = null;
				try {
					uri = file.toURI().toURL().toExternalForm();
				} catch (MalformedURLException mue) {
					uri = file.toURI().toString();
				}
				m.put("url", uri);
			}
		}
		DataSource ds = super.create(m, collection);

		if (!enabledTypes.isEmpty()) {
			List<String> excludes = DataSourceUtils.getExcludePattern(ds);
			if (!enabledTypes.contains(DataSourceSpec.Type.file.toString())) {
				List<String> newExcludes = new ArrayList<String>(excludes.size() + 1);
				newExcludes.add("file:/");
				DataSourceUtils.setExcludePattern(ds, newExcludes);
			} else if (!enabledTypes.contains(DataSourceSpec.Type.web.toString())) {
				List<String> newExcludes = new ArrayList<String>(excludes.size() + 1);
				newExcludes.add("http[s]?://");
				DataSourceUtils.setExcludePattern(ds, newExcludes);
			}
		}
		return ds;
	}
}
