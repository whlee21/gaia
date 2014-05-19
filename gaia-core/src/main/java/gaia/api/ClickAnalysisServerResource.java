package gaia.api;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.Map;

import org.restlet.resource.ServerResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import gaia.Constants;
import gaia.Defaults;
import gaia.utils.JarClassLoader;

public class ClickAnalysisServerResource extends ServerResource implements ClickAnalysisResource {
	private static final Logger LOG = LoggerFactory.getLogger(ClickAnalysisServerResource.class);
	private static Class<? extends ClickAnalysisResource> implClazz;
	private static final String implName = "gaia.api.ClickAnalysisServerResourceImpl";
	ClickAnalysisResource resource;

	@Inject
	public ClickAnalysisServerResource(Defaults defaults) throws InstantiationException, IllegalAccessException,
			SecurityException, NoSuchMethodException, IllegalArgumentException, InvocationTargetException {
		Constructor<? extends ClickAnalysisResource> ctor = implClazz.getConstructor(new Class[] { Defaults.class });
		resource = ((ClickAnalysisResource) ctor.newInstance(new Object[] { defaults }));
	}

	public Map<String, Object> status() throws Exception {
		resource.setRequest(getRequest());
		return resource.status();
	}

	public Map<String, Object> process(Map<String, Object> args) throws Exception {
		resource.setRequest(getRequest());
		return resource.process(args);
	}

	public String stop() throws Exception {
		resource.setRequest(getRequest());
		return resource.stop();
	}

	static {
		try {
			File hadoopFile = new File(Constants.GAIA_HADOOP_JOB_HOME + File.separator + "hadoop-deps.jar");
			if (!hadoopFile.exists()) {
				throw new Exception("Hadoop-deps jar not found in " + hadoopFile.toURI());
			}
			File clickFile = new File(Constants.GAIA_HADOOP_JOB_HOME + File.separator + "click-tools.job");
			if (!clickFile.exists()) {
				throw new Exception("Click tools jar not found in " + clickFile.toURI());
			}
			URL hadoopJar = hadoopFile.toURI().toURL();
			URL clickJar = clickFile.toURI().toURL();
			JarClassLoader loader = new JarClassLoader(new URL[] { hadoopJar, clickJar }, new ClassLoader[] {
					ClickAnalysisServerResource.class.getClassLoader(), ClassLoader.getSystemClassLoader() });

			implClazz = (Class<? extends ClickAnalysisResource>) loader.loadClass(implName);
			LOG.info("Loaded Hadoop deps from " + hadoopJar);
		} catch (Throwable t) {
			LOG.warn("Can't load Hadoop deps under a different classloader - using main classloader: " + t.toString());
			try {
				implClazz = (Class<? extends ClickAnalysisResource>) Class.forName(implName);
			} catch (ClassNotFoundException e) {
				LOG.error("Unexplicable exception: can't find a class that should always be there! " + e);
			}
		}
	}
}
