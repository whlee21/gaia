package gaia.crawl.resource;

import gaia.crawl.datasource.DataSourceId;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public abstract class ResourceManager {
	public abstract void uploadResource(String paramString, DataSourceId paramDataSourceId, Resource paramResource,
			InputStream paramInputStream, boolean paramBoolean) throws IOException;

	public abstract List<Resource> listResources(String paramString, DataSourceId paramDataSourceId) throws IOException;

	public abstract Resource getResource(String paramString1, DataSourceId paramDataSourceId, String paramString2)
			throws IOException;

	public abstract InputStream openResource(String paramString1, DataSourceId paramDataSourceId, String paramString2)
			throws IOException;

	public abstract void deleteResource(String paramString1, DataSourceId paramDataSourceId, String paramString2)
			throws IOException;

	public void deleteResources(String collection, DataSourceId dsId) throws IOException {
		List<Resource> resources = listResources(collection, dsId);
		if (resources.isEmpty()) {
			return;
		}
		for (Resource resource : resources)
			deleteResource(collection, dsId, resource.getName());
	}
}
