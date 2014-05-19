package gaia.jmx;

import gaia.crawl.datasource.DataSourceId;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

public interface JmxManager {
	public void unregisterOnShutdown();

	public void registerCollectionMBean(String paramString);

	public void unregisterCollectionMBean(String paramString);

	public void registerDataSourceMBean(DataSourceId paramDataSourceId);

	public void unregisterDataSourceMBean(DataSourceId paramDataSourceId);

	public ObjectName getAggregateObjectName() throws MalformedObjectNameException;

	public ObjectName getCollectionObjectName(String paramString) throws MalformedObjectNameException;

	public ObjectName getDataSourceObjectName(DataSourceId paramDataSourceId) throws MalformedObjectNameException;
}
