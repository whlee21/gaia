package gaia.crawl.dih;

import gaia.crawl.datasource.DataSourceId;
import gaia.crawl.resource.Resource;
import gaia.crawl.resource.ResourceManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.Driver;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOCase;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.solr.core.SolrCore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JdbcDriversResourceManager extends ResourceManager {
	private static transient Logger LOGGER = LoggerFactory.getLogger(JdbcDriversResourceManager.class);

	private static IOFileFilter jarsFilter = FileFilterUtils.and(new IOFileFilter[] {
			FileFilterUtils.suffixFileFilter(".jar", IOCase.INSENSITIVE), FileFilterUtils.fileFileFilter() });
	private File dir;

	public JdbcDriversResourceManager(File dir) {
		this.dir = dir;
		dir.mkdirs();
	}

	public void uploadResource(String collection, DataSourceId dsId, Resource res, InputStream is, boolean overwrite)
			throws IOException {
		File colDir = collectionDir(collection);
		OutputStream os = null;
		try {
			File jarFile = new File(colDir, res.getName());
			if ((jarFile.exists()) && (!overwrite)) {
				throw new IOException("resource " + res.getName() + " already exists");
			}
			os = new FileOutputStream(jarFile);
			IOUtils.copy(is, os);
		} finally {
			if (os != null)
				IOUtils.closeQuietly(os);
		}
	}

	public List<Resource> listResources(String collection, DataSourceId dsId) throws IOException {
		File colDir = collectionDir(collection);
		Collection<File> files = FileUtils.listFiles(colDir, jarsFilter, null);
		List<Resource> resources = new ArrayList<Resource>();
		for (File file : files) {
			Map<String, String> props = new HashMap<String, String>();
			props.put("type", "jar");
			Resource res = new Resource(file.getName(), props);
			resources.add(res);
		}

		List<String> classes = getClasses(collection);
		for (String clazz : classes) {
			Map<String, String> props = new HashMap<String, String>();
			props.put("type", "class");
			Resource res = new Resource(clazz, props);
			resources.add(res);
		}

		return resources;
	}

	private List<String> getClasses(String collection) throws IOException {
		List<String> classes = new ArrayList<String>();

		String tmpDir = System.getProperty("java.io.tmpdir", "/tmp");
		File dir = new File(tmpDir, "dih-validate-" + new Date().getTime());
		dir.mkdirs();

		DIHImporter importer = null;
		try {
			importer = new DIHImporter(dir.getAbsolutePath());
			importer.init();
			importer.copyJdbcJars(collection);
			importer.startEmbeddedSolrServer();

			SolrCore core = importer.server.getCoreContainer().getCore("collection1");
			ClassLoader classLoader = null;
			try {
				classLoader = core.getResourceLoader().getClassLoader();
			} finally {
				core.close();
			}

			ServiceLoader<Driver> driversLoader = ServiceLoader.load(Driver.class, classLoader);
			for (Driver driver : driversLoader)
				classes.add(driver.getClass().getCanonicalName());
		} finally {
			if (importer != null) {
				importer.shutdown();
			}
			FileUtils.deleteQuietly(dir);
		}

		return classes;
	}

	public Resource getResource(String collection, DataSourceId dsId, String name) throws IOException {
		File colDir = collectionDir(collection);
		File jarFile = new File(colDir, name);
		if (!jarFile.exists()) {
			throw new IOException("resource '" + name + "' does not exist");
		}
		return new Resource(jarFile.getName(), null);
	}

	public InputStream openResource(String collection, DataSourceId dsId, String name) throws IOException {
		File colDir = collectionDir(collection);
		File jarFile = new File(colDir, name);
		if (!jarFile.exists()) {
			throw new IOException("resource '" + name + "' does not exist");
		}
		return FileUtils.openInputStream(jarFile);
	}

	public void deleteResource(String collection, DataSourceId dsId, String name) throws IOException {
		File colDir = collectionDir(collection);
		File jarFile = new File(colDir, name);
		if (!jarFile.exists()) {
			throw new IOException("resource '" + name + "' does not exist");
		}
		jarFile.delete();
	}

	private File collectionDir(String collection) {
		if (collection == null)
			throw new IllegalArgumentException("Missing collection paramater");
		File colDir = new File(dir, collection);
		colDir.mkdirs();
		return colDir;
	}
}
