package gaia.crawl.resource;

import gaia.Constants;
import gaia.crawl.datasource.DataSourceId;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.commons.io.IOUtils;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileResourceManager extends ResourceManager {
	private static final Logger LOG = LoggerFactory.getLogger(FileResourceManager.class);
	File dataDir;
	File metaDir;

	public FileResourceManager(String crawlerName) {
		File dir = new File(Constants.GAIA_CRAWLER_RESOURCES_HOME, crawlerName);
		dataDir = new File(dir, "data");
		metaDir = new File(dir, "meta");
		dataDir.mkdirs();
		metaDir.mkdirs();
	}

	private void writeJSON(File output, Resource res) throws IOException {
		ObjectMapper mapper = new ObjectMapper();
		mapper.writeValue(output, res);
	}

	private Resource fromJSON(File input) throws IOException {
		ObjectMapper mapper = new ObjectMapper();
		Resource res = (Resource) mapper.readValue(input, Resource.class);
		return res;
	}

	public synchronized void uploadResource(String collection, DataSourceId dsId, Resource res, InputStream is,
			boolean overwrite) throws IOException {
		File data = new File(dataDir, res.name);
		if ((data.exists()) && (!overwrite)) {
			throw new IOException("resource " + res.name + " already exists");
		}

		File meta = new File(metaDir, res.name);
		writeJSON(meta, res);
		OutputStream os = null;
		try {
			os = new FileOutputStream(data);
			IOUtils.copy(is, os);
		} finally {
			if (os != null)
				IOUtils.closeQuietly(os);
		}
	}

	public List<Resource> listResources(String collection, DataSourceId dsId) throws IOException {
		List<String> names = Arrays.asList(metaDir.list());
		List<Resource> resources = new ArrayList<Resource>();
		for (String name : names) {
			Resource res = getResource(collection, dsId, name);
			resources.add(res);
		}
		return resources;
	}

	public Resource getResource(String collection, DataSourceId dsId, String name) throws IOException {
		File meta = new File(metaDir, name);
		if (!meta.exists()) {
			throw new IOException("resource '" + name + "' does not exist");
		}
		Resource res = fromJSON(meta);
		return res;
	}

	public InputStream openResource(String collection, DataSourceId dsId, String name) throws IOException {
		File data = new File(dataDir, name);
		if (!data.exists()) {
			throw new IOException("resource '" + name + "' does not exist");
		}
		return new FileInputStream(data);
	}

	public void deleteResource(String collection, DataSourceId dsId, String name) throws IOException {
		File data = new File(dataDir, name);
		if (!data.exists())
			LOG.warn("DELETE: resource '" + name + "' data does not exist, ignoring");
		else {
			data.delete();
		}
		File meta = new File(metaDir, name);
		if (!meta.exists())
			LOG.warn("DELETE: resource '" + name + "' metadata does not exist, ignoring");
		else
			meta.delete();
	}
}
