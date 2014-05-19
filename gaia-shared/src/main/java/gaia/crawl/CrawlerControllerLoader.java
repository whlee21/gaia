package gaia.crawl;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gaia.Constants;
import gaia.utils.JarClassLoader;

public class CrawlerControllerLoader extends JarClassLoader {
	private static final Logger LOG = LoggerFactory.getLogger(CrawlerControllerLoader.class);
	public static final String ATTR_ALIAS = "Crawler-Alias";
	public static final String ATTR_CLASS = "Crawler-Class";
	public static final String ATTR_DEPENDS = "Crawler-Depends";
	public static final String ATTR_EXCLUDE = "Crawler-Exclude";
	public static final String ATTR_NATIVE = "Crawler-Native";
	private Map<String, String> impls;
	private Map<String, JarClassLoader> libraryCls;
	private List<JarClassLoader> depends = new LinkedList<JarClassLoader>();
	private String jarName;
	private File resourcesDir;
	private ParentClassLoader parentCl;

	public CrawlerControllerLoader(URL url, File resourcesDir, Map<String, JarClassLoader> libraryCls,
			ClassLoader[] parents) {
		super(url != null ? new URL[] { url } : new URL[0], parents);
		this.resourcesDir = resourcesDir;
		this.libraryCls = libraryCls;
		parentCl = new ParentClassLoader(this);
	}

	public Map<String, String> getImpls() {
		return impls;
	}

	public void init() {
		impls = new HashMap<String, String>();
		if ((initialUrls != null) && (initialUrls.length > 0)) {
			String name = initialUrls[0].getFile();
			try {
				jarName = new File(name).toURI().toURL().getFile();
			} catch (MalformedURLException mue) {
				LOG.warn("Can't convert jar name to OS-independent format: " + mue.toString());
				jarName = name;
			}
		}
		LOG.debug("jarName: " + jarName);
		super.init();
	}

	protected void processManifest(JarFile jarFile) throws IOException {
		File f = new File(jarFile.getName());
		String fileName = f.toURI().toURL().getFile();
		if (!fileName.equals(jarName)) {
			if (DEBUG) {
				LOG.info("--skip manifest " + fileName + " != " + jarName);
			}
			return;
		}
		if (DEBUG) {
			LOG.info("--loading manifest " + fileName);
		}

		Manifest m = jarFile.getManifest();
		if (m == null) {
			if (DEBUG) {
				LOG.info("-- no manifest, skipping " + fileName);
			}
			return;
		}
		boolean found = false;

		Attributes a = m.getMainAttributes();
		String alias = null;
		String clazz = null;
		String exclude = null;
		String nativeNames = null;
		String dependsList = null;
		alias = a.getValue("Crawler-Alias");
		clazz = a.getValue("Crawler-Class");
		exclude = a.getValue("Crawler-Exclude");
		nativeNames = a.getValue("Crawler-Native");
		dependsList = a.getValue("Crawler-Depends");
		if ((alias != null) && (clazz != null)) {
			alias = alias.trim();
			clazz = clazz.trim();
			if ((alias.length() > 0) && (clazz.length() > 0)) {
				found = true;
				addCrawler(alias, clazz);
			}
		}
		if (exclude != null) {
			String[] vals = exclude.split("\\s+");
			for (String v : vals) {
				if (v.endsWith(".")) {
					v = v.substring(0, v.length() - 1);
				}
				exclusions.add(v.trim());
			}
		}
		if (nativeNames != null) {
			processNatives(nativeNames, Constants.GAIA_CRAWLER_RESOURCES_HOME);
		}
		if (dependsList != null) {
			if (DEBUG) {
				LOG.info("-- adding depends: " + dependsList);
			}
			if (dependsList.trim().length() == 0) {
				LOG.warn("Crawler-Depends declared but empty, skipping..");
			} else {
				String[] deps = dependsList.split("[,\\s]+");
				for (String d : deps) {
					JarClassLoader cl = (JarClassLoader) libraryCls.get(d);
					if (cl == null) {
						LOG.warn("Dependency '" + d + "' of " + fileName + " NOT FOUND");

						impls.clear();
					} else {
						depends.add(cl);
						cl.getParents().add(parentCl);
					}
				}
			}
		}

		for (Map.Entry<String, Attributes> e : m.getEntries().entrySet()) {
			String section = (String) e.getKey();

			exclude = ((Attributes) e.getValue()).getValue("Crawler-Exclude");
			if (exclude != null) {
				String[] vals = exclude.split("\\s+");
				for (String v : vals) {
					if (v.endsWith(".")) {
						v = v.substring(0, v.length() - 1);
					}
					exclusions.add(v.trim());
				}
			}
			alias = ((Attributes) e.getValue()).getValue("Crawler-Alias");
			if (alias == null) {
				alias = section;
			}
			clazz = ((Attributes) e.getValue()).getValue("Crawler-Class");
			if (clazz == null) {
				if (DEBUG) {
					LOG.info("--invalid section " + section + ", skipping file " + fileName);
				}
			} else {
				alias = alias.trim();
				clazz = clazz.trim();
				if ((alias.length() > 0) && (clazz.length() > 0)) {
					addCrawler(alias, clazz);
					if (DEBUG) {
						LOG.info("--found " + alias + "=" + clazz);
					}
					found = true;
				}
				nativeNames = ((Attributes) e.getValue()).getValue("Crawler-Native");
				if (nativeNames != null)
					processNatives(nativeNames, Constants.GAIA_CRAWLER_RESOURCES_HOME);
			}
		}
		if (!found)
			LOG.info("Missing Crawler-Class manifest attributes in " + jarFile.getName());
	}

	public void addCrawler(String alias, String clazz) throws IOException {
		impls.put(alias, clazz);
		if ((resourcesDir == null) || (!resourcesDir.exists()) || (resourcesDir.isFile())) {
			return;
		}

		File subDir = new File(resourcesDir, alias);
		if ((!subDir.exists()) || (subDir.isFile())) {
			return;
		}
		File[] files = subDir.listFiles();
		if ((files == null) || (files.length == 0)) {
			return;
		}
		for (File f : files) {
			addURL(f.toURI().toURL());
			LOG.debug("Added resource: " + alias + " / " + f.getName());
		}
	}

	public Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
		for (JarClassLoader cl : depends) {
			try {
				Class<?> c = cl.loadClass(name, resolve);
				if (c != null)
					return c;
			} catch (ClassNotFoundException cnfe) {
			}
		}
		return loadClassInternal(name, resolve);
	}

	private Class<?> loadClassInternal(String name, boolean resolve) throws ClassNotFoundException {
		return super.loadClass(name, resolve);
	}

	public URL getResource(String s) {
		for (JarClassLoader cl : depends)
			try {
				URL u = cl.getResource(s);
				if (u != null) {
					if (DEBUG) {
						LOG.info(toString() + " getResource(" + s + ") got " + u + " from JCL " + cl.toString());
					}
					return u;
				}
			} catch (Exception e) {
			}
		if (DEBUG) {
			LOG.info(toString() + " getResource(" + s + "): got null in CCL, trying super");
		}
		return getResourceInternal(s);
	}

	private URL getResourceInternal(String s) {
		return super.getResource(s);
	}

	public String findLibrary(String s) {
		for (JarClassLoader cl : depends) {
			String res = cl.findLibrary(s);
			if (res != null) {
				return res;
			}
		}
		return super.findLibrary(s);
	}

	public static Map<String, Class<? extends CrawlerController>> loadClasses(URL jar, File resourcesDir,
			Map<String, JarClassLoader> libraryCls) throws Exception {
		CrawlerControllerLoader cl = new CrawlerControllerLoader(jar, resourcesDir, libraryCls, new ClassLoader[] {
				Thread.currentThread().getContextClassLoader(), CrawlerControllerLoader.class.getClassLoader(),
				ClassLoader.getSystemClassLoader() });

		cl.init();
		Map<String, Class<? extends CrawlerController>> res = new HashMap<String, Class<? extends CrawlerController>>();
		for (Map.Entry<String, String> e : cl.getImpls().entrySet()) {
			try {
				Class<? extends CrawlerController> clazz = (Class<? extends CrawlerController>) Class.forName(e.getValue(),
						true, cl);
				res.put(e.getKey(), clazz);
			} catch (Throwable t) {
				LOG.warn("Cannot load class " + (String) e.getValue() + ": " + t.toString(), t);
			}
		}

		return res;
	}

	private static class ParentClassLoader extends ClassLoader {
		CrawlerControllerLoader ccl;

		ParentClassLoader(CrawlerControllerLoader ccl) {
			this.ccl = ccl;
		}

		protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
			return ccl.loadClassInternal(name, resolve);
		}

		public URL getResource(String name) {
			return null;
		}
	}
}
