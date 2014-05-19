package gaia.crawl;

import gaia.Constants;
import gaia.Defaults;
import gaia.utils.FileUtils;
import gaia.utils.JarClassLoader;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

@Singleton
public final class CrawlerControllerRegistry {
	private static transient Logger LOG = LoggerFactory.getLogger(CrawlerControllerRegistry.class);
	private Injector injector;
	private Defaults defaults;
	private Set<String> enabledCCs = new HashSet<String>();
	private Map<String, Class<? extends CrawlerController>> controllerClasses = new HashMap<String, Class<? extends CrawlerController>>();

	private Map<String, URL> controllerJars = new HashMap<String, URL>();
	private Map<String, JarClassLoader> libraryCls = new HashMap<String, JarClassLoader>();
	private Map<String, CrawlerController> controllers = new HashMap<String, CrawlerController>();

	@Inject
	public CrawlerControllerRegistry(Injector injector) {
		this.injector = injector;
		defaults = ((Defaults) injector.getInstance(Defaults.class));
		String enabledList = defaults.getString(Defaults.Group.crawlers, Defaults.ENABLED_CRAWLERS_KEY);
		if (!StringUtils.isBlank(enabledList)) {
			String[] list = enabledList.split("[\\s,]+");
			enabledCCs.addAll(Arrays.asList(list));
		}
		initRegistry();
	}

	private void cleanupTempDir() {
		if (JarClassLoader.tmpDir.exists())
			FileUtils.emptyDirectory(JarClassLoader.tmpDir);
	}

	private void initSharedLibs() {
		libraryCls.clear();
		File dir = new File(Constants.GAIA_CRAWLERS_SHARED_HOME);
		if (!dir.exists()) {
			return;
		}
		if (dir.isFile()) {
			LOG.warn("Shared library path points to a file instead of directory, ignoring: " + dir.getAbsolutePath());
			return;
		}
		for (File f : dir.listFiles()) {
			String name = f.getName();
			URL[] urls = null;
			if (f.isFile()) {
				try {
					urls = new URL[] { new URL("file://" + f.getAbsolutePath()) };
				} catch (Exception e) {
					LOG.warn("Exception loading shared lib " + f.getAbsolutePath() + ", skipping: " + e.toString());
					continue;
				}
			} else {
				File[] subs = f.listFiles();
				ArrayList<URL> list = new ArrayList<URL>();
				for (File sub : subs)
					if (!sub.getName().startsWith(".")) {
						try {
							URL u = new URL("file://" + sub.getAbsolutePath());
							list.add(u);
						} catch (Exception e) {
							LOG.warn("Exception loading shared lib " + sub.getAbsolutePath() + ", skipping: " + e.toString());
						}
					}
				if (list.size() > 0) {
					urls = (URL[]) list.toArray(new URL[list.size()]);
				}
			}
			if (urls == null) {
				LOG.warn("No shared libraries in " + f.getAbsolutePath());
			} else {
				JarClassLoader cl = new JarClassLoader(urls, new ClassLoader[0]);
				cl.init();
				libraryCls.put(name, cl);
			}
		}
		LOG.info("Shared connector libraries: " + libraryCls.keySet());
	}

	private void initExternalSharedLibs(String libName, Set<String> paths, Set<String> natives, Set<String> exclusions)
			throws IOException {
		Set<URL> list = new HashSet<URL>();
		for (String p : paths) {
			File dir = new File(p);
			if (!dir.exists()) {
				LOG.warn("External library path doesn't exist: " + dir.getAbsolutePath());
			} else if (!dir.isDirectory()) {
				LOG.warn("External library path is not a directory: " + dir.getAbsolutePath());
			} else {
				File[] subs = dir.listFiles(new FilenameFilter() {
					public boolean accept(File dir, String name) {
						return name.endsWith(".jar");
					}
				});
				list.add(dir.toURI().toURL());
				if (subs != null)
					for (File sub : subs)
						list.add(sub.toURI().toURL());
			}
		}
		if (list.isEmpty()) {
			LOG.warn("No valid external paths - skipping " + libName + " initialization.");
			return;
		}
		URL[] urls = (URL[]) list.toArray(new URL[list.size()]);
		JarClassLoader cl = new JarClassLoader(urls, new ClassLoader[0]);

		if (!natives.isEmpty()) {
			cl.addLibraryPaths(libName, natives);
		}
		cl.init();
		if ((exclusions != null) && (!exclusions.isEmpty())) {
			cl.getExclusions().addAll(exclusions);
		}
		libraryCls.put(libName, cl);
	}

	private void initRegistry() {
		controllerClasses.clear();
		controllerJars.clear();
		cleanupTempDir();
		initSharedLibs();

		String maprHome = System.getProperty("mapr.home");
		if ((maprHome != null) && (maprHome.trim().length() > 0)) {
			if (!maprHome.endsWith(File.separator)) {
				maprHome = maprHome + File.separator;
			}
			HashSet<String> libs = new HashSet<String>();
			String hadoopPath = maprHome + "hadoop" + File.separator + "hadoop-0.20.2" + File.separator;

			libs.add(hadoopPath + "conf");
			libs.add(hadoopPath + "lib");
			libs.add(hadoopPath + File.separator + "lib" + File.separator + "jsp-2.1");
			HashSet<String> natives = new HashSet<String>();
			natives.add(maprHome + "lib");
			HashSet<String> exclusions = new HashSet<String>();
			exclusions.add("org.apache.commons.logging");
			try {
				initExternalSharedLibs("mapr-client", libs, natives, exclusions);
			} catch (IOException e) {
				LOG.warn("Could not initialize mapr-client libraries: " + e.getMessage(), e);
			}
		}

		File dir = new File(Constants.GAIA_CRAWLERS_HOME);
		File resources = new File(Constants.GAIA_CRAWLER_RESOURCES_HOME);
		LOG.info("Loading crawler plugins from " + dir.getAbsolutePath());
		if ((!dir.exists()) || (!dir.isDirectory())) {
			LOG.warn("No crawler plugins in " + dir + ", initializing default built-in crawlers.");
		} else {
			File[] files = dir.listFiles();
			for (File f : files) {
				if (f.getName().endsWith(".jar")) {
					if (!f.getAbsolutePath().equals(Constants.GAIA_CRAWLERS_SHARED_HOME)) {
						try {
							URL u = new URL("file://" + f.getAbsolutePath());
							Map<String, Class<? extends CrawlerController>> clazzes = CrawlerControllerLoader.loadClasses(u,
									resources, libraryCls);

							if ((clazzes == null) || (clazzes.isEmpty())) {
								LOG.info("No valid crawler plugins in " + u);
							} else {
								controllerClasses.putAll(clazzes);
								for (String alias : clazzes.keySet()) {
									controllerJars.put(alias, u);
								}
								LOG.info("Loaded " + clazzes.keySet() + " from " + u);
							}
						} catch (Throwable e) {
							LOG.warn("Loading crawler plugins from " + f + " failed: " + e.toString());
						}
					}
				}
			}
			if (controllerClasses.isEmpty()) {
				LOG.warn("Could not load any crawler plugins, loading built-in crawlers.");
			}
		}
		builtinInit(ConnectorManager.DEFAULT_BUILTIN_CRAWLERS);

		String extra = defaults.getString(Defaults.Group.crawlers, Defaults.EXTRA_CRAWLERS_KEY);
		List<String[]> extraLst = new ArrayList<String[]>();
		if (!StringUtils.isBlank(extra)) {
			String[] kvs = extra.split(",");
			for (int i = 0; i < kvs.length; i++) {
				String[] kv = kvs[i].split("=");
				if (kv.length != 2) {
					LOG.warn("Invalid extra.crawlers specification, skipping: " + kvs[i]);
				} else
					extraLst.add(kv);
			}
			if (extraLst.size() > 0) {
				String[][] extraCrawlers = (String[][]) extraLst.toArray(new String[0][]);
				LOG.info("Initializing extra built-in crawlers: " + extra);
				builtinInit(extraCrawlers);
			}
		}

		for (Map.Entry<String, Class<? extends CrawlerController>> e : controllerClasses.entrySet())
			try {
				initCrawler((String) e.getKey(), e.getValue());
			} catch (Throwable t) {
				LOG.warn("Initialization of '" + (String) e.getKey() + "' failed: " + t.toString(), t);
			}
	}

	private void builtinInit(String[][] builtIns) {
		for (int i = 0; i < builtIns.length; i++) {
			String cls = builtIns[i][0];
			String alias = builtIns[i][1];
			if ((!controllerClasses.containsKey(alias)) && (!controllerClasses.containsKey(cls))) {
				try {
					initCrawler(alias, cls);
					controllerJars.remove(alias);
					LOG.info("Loaded '" + alias + "' built-in crawler.");
				} catch (Exception e) {
					LOG.warn("Built-in crawler '" + alias + "' (" + cls + ") is missing: " + e.toString() + ", ignoring...");
				}
			}
		}
	}

	public Map<String, Class<? extends CrawlerController>> initCrawlersFromJar(String url) throws Exception {
		if (StringUtils.isBlank(url)) {
			throw new Exception("Non-empty argument (path or url) required.");
		}
		if ((url.indexOf("://") == -1) && (!url.startsWith("file:"))) {
			url = "file:" + url;
		}
		URL u = null;
		try {
			u = new URL(url);
		} catch (MalformedURLException mue) {
			throw new Exception(mue);
		}
		File resources = new File(Constants.GAIA_CRAWLER_RESOURCES_HOME);
		try {
			Map<String, Class<? extends CrawlerController>> clazzes = CrawlerControllerLoader.loadClasses(u, resources,
					libraryCls);

			if ((clazzes == null) || (clazzes.isEmpty())) {
				LOG.info("No valid crawler plugins in " + u.toExternalForm());
				return null;
			}
			controllerClasses.putAll(clazzes);
			for (String alias : clazzes.keySet()) {
				controllerJars.put(alias, u);
			}
			LOG.info("Loaded " + clazzes.keySet() + " from " + u.toExternalForm());
			return clazzes;
		} catch (Exception e) {
			LOG.warn("Loading crawler plugins from " + u.toExternalForm() + " failed: " + e.toString());
		}
		return null;
	}

	public CrawlerController initCrawler(String alias, String className) throws Exception {
		CrawlerController cc = get(alias);
		if (cc != null) {
			if (cc.getClass().getName().equals(className)) {
				return cc;
			}
		}
		CrawlerControllerLoader cl = new CrawlerControllerLoader(null, new File(Constants.GAIA_CRAWLER_RESOURCES_HOME),
				libraryCls, new ClassLoader[] { Thread.currentThread().getContextClassLoader(),
						CrawlerControllerLoader.class.getClassLoader(), ClassLoader.getSystemClassLoader() });

		cl.init();
		cl.addCrawler(alias, className);
		Class<? extends CrawlerController> clazz = (Class<? extends CrawlerController>) Class.forName(className, true, cl);
		return initCrawler(alias, clazz);
	}

	private CrawlerController initCrawler(String alias, Class<? extends CrawlerController> clazz) {
		if ((!enabledCCs.isEmpty()) && (!enabledCCs.contains(alias))
				&& (!enabledCCs.contains(clazz.getName()))) {
			LOG.info("Skipping initialization of disabled crawler '" + alias + "'.");
			return null;
		}

		synchronized (controllers) {
			try {
				CrawlerController cc = (CrawlerController) injector.getInstance(clazz);

				controllers.put(alias, cc);

				cc.getDataSourceFactory().setEnabledTypes(
						defaults.getString(Defaults.Group.crawlers, alias + "." + Defaults.ENABLED_DATASOURCES_KEY));

				cc.getDataSourceFactory().setRestrictedTypes(
						defaults.getString(Defaults.Group.crawlers, alias + "." + Defaults.RESTRICTED_DATASOURCES_KEY));

				String enabledCCs = defaults.getString(Defaults.Group.crawlers, Defaults.ENABLED_CRAWLERS_KEY);
				if ((!StringUtils.isBlank(enabledCCs)) && (!enabledCCs.contains(alias))) {
					defaults.set(Defaults.Group.crawlers, Defaults.ENABLED_CRAWLERS_KEY, enabledCCs + ", " + alias);
				}
				LOG.info("Initialized crawler '" + alias + "'.");
				return cc;
			} catch (Throwable t) {
				LOG.warn("Initialization of crawler '" + alias + "' failed: " + t.toString(), t);

				return null;
			}
		}
	}

	public CrawlerController get(String crawler) {
		CrawlerController cc = (CrawlerController) controllers.get(crawler);
		if (cc != null) {
			return cc;
		}

		for (CrawlerController ccx : controllers.values()) {
			if (ccx.getClass().getName().equals(crawler)) {
				return ccx;
			}
		}
		return null;
	}

	public synchronized Map<String, CrawlerController> getControllers() {
		return new HashMap<String, CrawlerController>(controllers);
	}

	Map<String, JarClassLoader> getSharedLibraryClassloaders() {
		return libraryCls;
	}

	public boolean isLoaded(String crawler) {
		synchronized (controllers) {
			if (controllers.get(crawler) == null) {
				for (CrawlerController ccx : controllers.values()) {
					if (ccx.getClass().getName().equals(crawler)) {
						return true;
					}
				}
				return false;
			}
			return true;
		}
	}

	public void abortAllJobs() throws Exception {
		boolean someAborted = false;
		for (CrawlerController cc : controllers.values()) {
			List<CrawlStatus> jobs = cc.listJobs();
			for (CrawlStatus status : jobs) {
				someAborted = true;
				try {
					cc.abortJob(status.getId());
				} catch (Throwable t) {
					LOG.warn("Could not abort job " + status.getId() + ".", t);
				}
			}
		}
		if (someAborted) {
			int loop = 10;
			ArrayList<CrawlId> ids = new ArrayList<CrawlId>();
			do {
				ids.clear();
				for (CrawlerController cc : controllers.values()) {
					List<CrawlStatus> jobs = cc.listJobs();
					for (CrawlStatus status : jobs) {
						if (status.isRunning()) {
							ids.add(status.getId());
						}
					}
				}
				if (ids.size() > 0) {
					LOG.info("Some jobs still running: " + ids);
					try {
						Thread.sleep(2000L);
					} catch (InterruptedException ie) {
					}
				}
			} while ((ids.size() > 0) && (loop-- > 0));
			if (ids.size() > 0)
				LOG.warn("Giving up, could not abort these jobs: " + ids);
		}
	}

	public void shutdown() throws Exception {
		LOG.info("Shutting down " + controllers.size() + " crawler controllers");
		abortAllJobs();
		synchronized (controllers) {
			for (CrawlerController cs : controllers.values())
				try {
					cs.close();
				} catch (JobStateException jse) {
					LOG.error("Close() of " + cs.getClass().getName() + " failed, jobs still running " + jse.getJobs());
				} catch (Exception e) {
					LOG.error("Exception in close", e);
				}
		}
	}

	public void reset(boolean initRegistry) throws Exception {
		LOG.info("Resetting " + controllers.size() + " crawler controllers");
		abortAllJobs();
		synchronized (controllers) {
			for (CrawlerController cs : controllers.values()) {
				try {
					cs.close();
					cs.setClosing(null, false);
				} catch (JobStateException jse) {
					LOG.error("Close() of " + cs.getClass().getName() + " failed, jobs still running " + jse.getJobs());
				} catch (Exception e) {
					LOG.error("Exception in close", e);
				}
			}
		}
		if (initRegistry) {
			controllers.clear();
			initRegistry();
		}
	}

	public boolean reload(String alias) throws Exception {
		Class clazz = (Class) controllerClasses.get(alias);
		URL u = (URL) controllerJars.get(alias);
		if (u != null) {
			controllers.remove(alias);
			controllerClasses.remove(alias);
			controllerJars.remove(alias);
			initCrawlersFromJar(u.toExternalForm());
			return true;
		}
		if (clazz != null) {
			controllers.remove(alias);
			controllerClasses.remove(alias);
			initCrawler(alias, clazz.getName());
			return true;
		}
		for (String[] e : ConnectorManager.DEFAULT_BUILTIN_CRAWLERS) {
			if (e[1].equals(alias)) {
				initCrawler(e[1], e[0]);
				return true;
			}
		}
		return false;
	}
}
