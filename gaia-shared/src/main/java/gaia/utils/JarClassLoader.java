package gaia.utils;

import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.apache.commons.compress.utils.IOUtils;
import org.apache.commons.lang.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gaia.Constants;

public class JarClassLoader extends URLClassLoader {
	private static final Logger LOG = LoggerFactory.getLogger(JarClassLoader.class);

	protected static boolean DEBUG = false;
	private static Field fieldSysPath = null;
	private static Method loadLibrary0 = null;

	public static final File tmpDir = new File(System.getProperty("java.io.tmpdir"), new StringBuilder()
			.append("gaiasearch-ccr-").append(Constants.GAIA_DATA_HOME.hashCode()).toString());
	protected static final String osName;
	protected static final String archName;
	protected static final String platformName;
	protected static final String simplePlatformName;
	protected Set<String> libraryPaths = new HashSet<String>();

	protected List<ClassLoader> parents = new LinkedList<ClassLoader>();
	protected URL[] initialUrls;
	protected Set<String> exclusions = new HashSet<String>();

	public static String getOSName() {
		return osName;
	}

	public static String getSimplePlatformName() {
		return simplePlatformName;
	}

	public static String getPlatformName() {
		return platformName;
	}

	public static boolean loadNativeLibrary(Class<?> fromClass, File file) throws Exception {
		return ((Boolean) loadLibrary0.invoke(null, new Object[] { fromClass, file })).booleanValue();
	}

	public synchronized void addLibraryPaths(String jarName, Set<String> paths) {
		if (fieldSysPath == null) {
			LOG.warn(new StringBuilder().append(jarName)
					.append(" adding native libs ignored - java.library.path can't be modified during runtime").toString());
			return;
		}
		String libraryPath = System.getProperty("java.library.path");
		boolean modified = false;
		for (String p : paths) {
			if (!libraryPath.contains(p)) {
				libraryPath = new StringBuilder().append(libraryPath).append(File.pathSeparator).append(p).toString();
				libraryPaths.add(p);
				modified = true;
			}
		}
		if (modified) {
			LOG.info(new StringBuilder().append("Adding native library paths: ").append(paths).toString());
			System.setProperty("java.library.path", libraryPath);
			try {
				fieldSysPath.set(null, null);
				LOG.info(new StringBuilder().append("Updated native library paths: ").append(libraryPath).toString());
			} catch (IllegalArgumentException e) {
				LOG.warn(new StringBuilder()
						.append("Unable to modify java.library.path, crawler native libraries may be missing: ")
						.append(e.toString()).toString());
			} catch (IllegalAccessException e) {
				LOG.warn(new StringBuilder()
						.append("Unable to modify java.library.path, crawler native libraries may be missing: ")
						.append(e.toString()).toString());
			}
		}
	}

	private static void close(Closeable closeable) {
		if (closeable != null)
			try {
				closeable.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
	}

	private static boolean isJar(String fileName) {
		return (fileName != null) && (fileName.toLowerCase().endsWith(".jar"));
	}

	private static File jarEntryAsFile(JarFile jarFile, JarEntry jarEntry) throws IOException {
		InputStream input = null;
		OutputStream output = null;
		try {
			String name = jarEntry.getName().replace('/', '_');
			int i = name.lastIndexOf(".");
			String extension = i > -1 ? name.substring(i) : "";
			if (!tmpDir.exists()) {
				tmpDir.mkdirs();
			}
			File file = File.createTempFile(new StringBuilder().append(name.substring(0, name.length() - extension.length()))
					.append(".").toString(), extension, tmpDir);

			file.deleteOnExit();
			input = jarFile.getInputStream(jarEntry);
			output = new FileOutputStream(file);

			byte[] buffer = new byte[4096];
			int readCount;
			while ((readCount = input.read(buffer)) != -1) {
				output.write(buffer, 0, readCount);
			}
			return file;
		} finally {
			close(input);
			close(output);
		}
	}

	public JarClassLoader(URL[] urls, ClassLoader[] parents) {
		super(new URL[0], (parents != null) && (parents.length > 0) ? parents[0] : null);

		initialUrls = urls;
		if ((parents != null) && (parents.length > 0))
			this.parents.addAll(Arrays.asList(parents));
	}

	public void init() {
		try {
			for (URL u : initialUrls) {
				String jarName = u.getFile();
				if (isJar(jarName))
					addJarResource(new File(u.getPath()));
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		if (DEBUG)
			LOG.info(new StringBuilder().append("Resources: ").append(Arrays.toString(getURLs())).append(", parents: ")
					.append(parents).toString());
	}

	public List<ClassLoader> getParents() {
		return parents;
	}

	public Set<String> getExclusions() {
		return exclusions;
	}

	public void addJarResource(File file) throws IOException {
		if (DEBUG) {
			LOG.info(new StringBuilder().append(toString()).append(" Jar: adding ").append(file).toString());
		}
		JarFile jarFile = new JarFile(file);
		addURL(file.toURI().toURL());
		processManifest(jarFile);

		jarFile.close();
		jarFile = new JarFile(file);
		Enumeration<JarEntry> jarEntries = jarFile.entries();
		String nativeNames = null;
		while (jarEntries.hasMoreElements()) {
			JarEntry jarEntry = (JarEntry) jarEntries.nextElement();
			if ((!jarEntry.isDirectory()) && (isJar(jarEntry.getName()))) {
				if (DEBUG) {
					LOG.info(new StringBuilder().append(toString()).append(" Jar: adding jarEntryAsFile ")
							.append(jarEntry.getName()).toString());
				}
				addJarResource(jarEntryAsFile(jarFile, jarEntry));
			} else {
				String name = jarEntry.getName();
				if (DEBUG) {
					LOG.info(new StringBuilder().append(toString()).append(" plain resource: ").append(jarEntry.getName())
							.toString());
				}
				String prefix = null;
				if (name.contains(simplePlatformName)) {
					if (DEBUG) {
						LOG.info(new StringBuilder().append(toString()).append(" probable native resource ").append(name)
								.append(", will add natives").toString());
					}
					int idx = name.indexOf(simplePlatformName);
					prefix = name.substring(0, idx);
				} else if (name.contains(platformName)) {
					if (DEBUG) {
						LOG.info(new StringBuilder().append(toString()).append(" probable native resource ").append(name)
								.append(", will add natives").toString());
					}
					int idx = name.indexOf(platformName);
					prefix = name.substring(0, idx);
				}
				if (prefix != null) {
					if (nativeNames != null)
						nativeNames = new StringBuilder().append(nativeNames).append(",").append(prefix).toString();
					else {
						nativeNames = prefix;
					}
				}
			}
		}
		if (nativeNames != null)
			processNatives(nativeNames, Constants.GAIA_CRAWLER_RESOURCES_HOME);
	}

	protected void processManifest(JarFile jarFile) throws IOException {
	}

	protected void processNatives(String nativePrefixes, String resourcesDir) throws IOException {
		if ((nativePrefixes == null) || (nativePrefixes.trim().length() == 0)) {
			LOG.warn("native prefixes was an empty value, skipping..");
			return;
		}
		String[] natives = nativePrefixes.trim().split("[,\\s]+");
		if (natives.length == 0) {
			LOG.warn("native prefix was an empty value, skipping..");
			return;
		}
		Set<String> names = new HashSet<String>(Arrays.asList(natives));
		for (URL u : initialUrls) {
			Set<String> allDirs = new HashSet<String>();
			Set<String> allLibs = new HashSet<String>();
			JarFile jf = new JarFile(new File(u.getFile()));
			String[] pathEls = u.getFile().split("/");
			String jarName = pathEls[(pathEls.length - 1)];
			File outDir = new File(resourcesDir, new StringBuilder().append("native").append(File.separator).append(jarName)
					.toString());

			outDir.mkdirs();
			LOG.info(new StringBuilder().append(jarName).append(": preparing native library path for platform ")
					.append(simplePlatformName).toString());
			for (Enumeration<JarEntry> e = jf.entries(); e.hasMoreElements();) {
				JarEntry je = e.nextElement();
				for (String name : names) {
					if (name.endsWith("/")) {
						name = name.substring(0, name.length() - 1);
					}
					String resName = je.getName();
					if (resName.startsWith("/")) {
						resName = resName.substring(1);
					}
					if (resName.startsWith(name)) {
						File out = new File(outDir, resName.substring(name.length() + 1));
						if (je.isDirectory()) {
							out.mkdirs();
						} else {
							out.getParentFile().mkdirs();
							String subDir = resName.substring(name.length() + 1);
							String[] els = subDir.split("/");
							if (els.length > 1) {
								if (els[(els.length - 2)].equals(platformName)) {
									allDirs.add(out.getParentFile().getAbsolutePath());
									allLibs.add(out.getName());
								} else if (subDir.contains(simplePlatformName)) {
									allDirs.add(out.getParentFile().getAbsolutePath());
									allLibs.add(out.getName());
								}
							} else {
								allDirs.add(out.getParentFile().getAbsolutePath());
								allLibs.add(out.getName());
							}
							FileOutputStream fos = new FileOutputStream(out);
							IOUtils.copy(jf.getInputStream(je), fos);
							fos.flush();
							fos.close();
						}
					}
				}
			}
			if (!allDirs.isEmpty()) {
				addLibraryPaths(jarName, allDirs);
				for (String name : allLibs) {
					String libPath = findLibrary(name);
					if (libPath != null) {
						File f = new File(libPath);
						if (f.getName().contains(".")) {
							try {
								if (((Boolean) loadLibrary0.invoke(null, new Object[] { getClass(), f })).booleanValue())
									LOG.info(new StringBuilder().append("Loaded native libary ").append(name).toString());
								else
									LOG.info(new StringBuilder().append("Failed to load native library ").append(name).toString());
							} catch (Throwable t) {
								LOG.info(new StringBuilder().append("Exception loading library ").append(name).append(" (libPath=")
										.append(libPath).append(")").toString(), t);
							}
						}
					}
				}
			} else {
				LOG.warn(new StringBuilder().append(jarName)
						.append(": native prefix specified but no valid directory found in jar.").toString());
			}

		}
	}

	public synchronized Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
		return loadClassWithParents(name, resolve, parents);
	}

	public synchronized Class<?> loadClassWithParents(String name, boolean resolve, List<ClassLoader> localParents)
			throws ClassNotFoundException {
		Class<?> clazz = null;
		try {
			if (DEBUG) {
				LOG.info(new StringBuilder().append(toString()).append(" loadClass: ").append(name).append(" JCL")
						.append(excluded(name) ? " excluded" : "").toString());
			}
			if (!excluded(name)) {
				clazz = findLoadedClass(name);
				if (clazz == null) {
					clazz = findClass(name);
					if (resolve)
						resolveClass(clazz);
				}
			}
		} catch (ClassNotFoundException e) {
		}
		if (clazz != null) {
			if (DEBUG) {
				LOG.info(new StringBuilder().append(toString()).append(" loadClass: ").append(name).append(" JCL got ")
						.append(clazz).append(" OK").toString());
			}
			return clazz;
		}
		if (DEBUG) {
			LOG.info(new StringBuilder().append(toString()).append(" loadClass: ").append(name).append(" trying parents ")
					.append(parents).toString());
		}
		if ((localParents != null) && (!localParents.isEmpty())) {
			for (ClassLoader cl : localParents) {
				if (cl != null) {
					try {
						clazz = cl.loadClass(name);
					} catch (ClassNotFoundException e2) {
					}
					if (clazz != null) {
						if (DEBUG) {
							LOG.info(new StringBuilder().append(toString()).append(" loadClass: ").append(name).append(" parent: ")
									.append(cl.getClass().getName()).append(" got ").append(clazz).append(" OK").toString());
						}
						return clazz;
					}
					if (DEBUG) {
						LOG.info(new StringBuilder().append(toString()).append(" loadClass: ").append(name).append(" parent: ")
								.append(cl.getClass().getName()).append(" not found...").toString());
					}
				}
			}
		}
		if (DEBUG) {
			LOG.info(new StringBuilder().append(toString()).append(" loadClass: ").append(name).append(" parents not found.")
					.toString());
		}

		if (DEBUG)
			LOG.info(new StringBuilder().append(toString()).append(" loadClass: ").append(name).append(" trying super")
					.toString());
		try {
			clazz = super.loadClass(name, resolve);
		} catch (ClassNotFoundException e1) {
		}
		if (clazz != null) {
			if (DEBUG) {
				LOG.info(new StringBuilder().append(toString()).append(" loadClass: ").append(name).append(" super got ")
						.append(clazz).append(" OK").toString());
			}
			return clazz;
		}
		if (DEBUG) {
			LOG.info(new StringBuilder().append(toString()).append(" loadClass: ").append(name)
					.append(" super got null, returning null").toString());
			LOG.info(new StringBuilder().append(toString()).append("\nParents: ").append(parents).append("\nResources: ")
					.append(Arrays.toString(getURLs())).toString());
		}
		return null;
	}

	protected boolean excluded(String name) {
		String[] segs = name.split("\\.");
		StringBuilder sb = new StringBuilder();
		for (String s : segs) {
			if (sb.length() > 0) {
				sb.append(".");
			}
			sb.append(s);
			if (exclusions.contains(sb.toString())) {
				return true;
			}
		}
		return false;
	}

	public URL getResource(String s) {
		URL url = null;
		url = findResource(s);
		if ((url == null) && (s.startsWith("/"))) {
			if (DEBUG) {
				LOG.info(new StringBuilder().append(toString()).append(" getResource(").append(s)
						.append("): removing leading /").toString());
			}
			url = findResource(s.substring(1));
		}
		if (url == null) {
			if (DEBUG) {
				LOG.info(new StringBuilder().append(toString()).append(" getResource(").append(s)
						.append("): got null in JCL, trying parents").toString());
			}
			for (ClassLoader parent : parents) {
				url = parent.getResource(s);
				if (url != null) {
					if (!DEBUG)
						break;
					LOG.info(new StringBuilder().append(toString()).append(" getResource(").append(s).append(") got ")
							.append(url).append(" from parent ").append(parent).toString());
					break;
				}
			}

		}

		if (DEBUG) {
			LOG.info(new StringBuilder().append(toString()).append(" getResource(").append(s).append(") returning ")
					.append(url).toString());
		}
		return url;
	}

	public InputStream getResourceAsStream(String s) {
		URL url = getResource(s);
		try {
			return url == null ? null : url.openStream();
		} catch (IOException e) {
		}
		return null;
	}

	public String findLibrary(String s) {
		String res = super.findLibrary(s);
		if (res == null) {
			if (DEBUG) {
				LOG.info(new StringBuilder().append(toString()).append(" findLibrary(").append(s)
						.append(") got null from super, trying libraryPaths locations: ").append(libraryPaths).toString());
			}
			for (String p : libraryPaths) {
				File f = new File(p, s);
				if (f.exists()) {
					if (DEBUG) {
						LOG.info(new StringBuilder().append(toString()).append(" findLibrary(").append(s)
								.append(") got from libraryPaths - ").append(f.getAbsolutePath()).toString());
					}
					return f.getAbsolutePath();
				}
			}
			if (DEBUG) {
				LOG.info(new StringBuilder().append(toString()).append(" findLibrary(").append(s)
						.append(") got null from super and null from libraryPaths, trying jar locations...").toString());
			}
			for (URL u : getURLs()) {
				File f = UrlUtils.url2File(u);
				if (f != null) {
					File libFile = new File(f.getParentFile(), s);
					if ((libFile.exists()) && (libFile.isFile())) {
						if (DEBUG) {
							LOG.info(new StringBuilder().append(toString()).append(" findLibrary(").append(s)
									.append(") got from location of ").append(f.getName()).append(" - ")
									.append(libFile.getAbsolutePath()).toString());
						}
						return libFile.getAbsolutePath();
					}
				}
			}
		}
		if (DEBUG) {
			LOG.info(new StringBuilder().append(toString()).append(" findLibrary(").append(s).append(") got ").append(res)
					.toString());
		}
		return res;
	}

	static {
		if ("true".equals(System.getProperty("gaia.classloader.debug"))) {
			DEBUG = true;
		}

		if (SystemUtils.IS_OS_WINDOWS)
			osName = "Windows";
		else if (SystemUtils.IS_OS_MAC)
			osName = "Mac";
		else if (SystemUtils.IS_OS_LINUX)
			osName = "Linux";
		else {
			osName = System.getProperty("os.name").replaceAll("\\W", "");
		}
		String arch = System.getProperty("os.arch");
		if (arch.equals("amd64"))
			archName = "x86_64";
		else if (arch.equals("x86"))
			archName = "i386";
		else {
			archName = "x86_64";
		}
		platformName = new StringBuilder().append(osName).append("-").append(archName).append("-")
				.append(System.getProperty("sun.arch.data.model")).toString().replace(' ', '_');

		simplePlatformName = new StringBuilder().append(osName).append("/").append(archName).toString();
		try {
			fieldSysPath = ClassLoader.class.getDeclaredField("sys_paths");
			fieldSysPath.setAccessible(true);
		} catch (SecurityException e) {
			LOG.warn(new StringBuilder()
					.append("Unable to modify java.library.path, crawler native libraries may be missing: ").append(e.toString())
					.toString());
		} catch (NoSuchFieldException e) {
			LOG.warn(new StringBuilder()
					.append("Unable to modify java.library.path, crawler native libraries may be missing: ").append(e.toString())
					.toString());
		}
		try {
			loadLibrary0 = ClassLoader.class.getDeclaredMethod("loadLibrary0", new Class[] { Class.class, File.class });
			loadLibrary0.setAccessible(true);
		} catch (SecurityException e) {
			LOG.warn(new StringBuilder()
					.append("Unable to load libraries from absolute paths, crawler native libraries may be missing: ")
					.append(e.toString()).toString());
		} catch (NoSuchMethodException e) {
			LOG.warn(new StringBuilder()
					.append("Unable to load libraries from absolute paths, crawler native libraries may be missing: ")
					.append(e.toString()).toString());
		}
	}
}
