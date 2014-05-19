package gaia.crawl.fs;

import gaia.crawl.datasource.DataSource;
import gaia.crawl.fs.ds.FSSpec;

public interface FSFactory {
	public FS createFS(DataSource paramDataSource) throws Exception;

	public FSSpec getSpec(DataSource paramDataSource);
}
