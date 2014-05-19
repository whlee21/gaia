package gaia.crawl;

import gaia.crawl.datasource.DataSource;
import java.util.List;

public interface DataSourceManager {
	public void syncSave(ConnectorManager paramConnectorManager) throws Exception;

	public int initialLoad(ConnectorManager paramConnectorManager) throws Exception;

	public List<DataSource> getDataSources();

	public void reload(ConnectorManager paramConnectorManager) throws Exception;
}
