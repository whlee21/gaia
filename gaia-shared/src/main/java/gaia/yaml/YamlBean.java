package gaia.yaml;

import gaia.Constants;
import gaia.utils.OSFileWriter;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.JavaBeanDumper;
import org.yaml.snakeyaml.JavaBeanLoader;
import org.yaml.snakeyaml.introspector.BeanAccess;

import com.google.inject.Singleton;

@Singleton
public abstract class YamlBean {
	private static Logger LOG = LoggerFactory.getLogger(YamlBean.class);

	protected String file = null;
	protected LocationIfRelative location;

	public YamlBean() {
	}

	public YamlBean(String file, boolean ignoreExistingContents) {
		construct(file, ignoreExistingContents, LocationIfRelative.DATA);
	}

	public YamlBean(String file, boolean ignoreExistingContents, LocationIfRelative location) {
		construct(file, ignoreExistingContents, location);
	}

	protected void construct(String file, boolean ignoreExistingContents, LocationIfRelative location) {
		this.file = file;
		this.location = location;

		File f = new File(file);
		if (!f.isAbsolute()) {
			f = getAbsolutePath(file, location, f);
		}

		if ((!ignoreExistingContents) && (f.exists())) {
			YamlBean yb = null;
			try {
				LOG.debug("Loading " + getClass().getSimpleName() + " state from " + f.getAbsolutePath());

				InputStream is = new FileInputStream(f);
				InputStreamReader reader = new InputStreamReader(is, "UTF-8");
				try {
					JavaBeanLoader loader = new JavaBeanLoader(getClass(), BeanAccess.FIELD);

					yb = (YamlBean) loader.load(reader);
				} finally {
					reader.close();
					is.close();
				}

				file = yb.file;
				location = yb.location;

				load(yb);
				LOG.info("Loaded " + getClass().getSimpleName());
			} catch (IOException e) {
				LOG.warn("Can't load " + getClass().getSimpleName() + " from " + f.getAbsolutePath());
			}
		} else {
			if (f.exists()) {
				LOG.debug("Ignoring existing " + getClass().getSimpleName() + " state of " + f.getAbsolutePath());
			} else {
				File dir = f.getParentFile();
				if (!dir.exists()) {
					LOG.info("Creating yaml data directory " + dir.getAbsolutePath());
					dir.mkdirs();
				}
				LOG.info("Creating new " + getClass().getSimpleName() + " state in " + f.getAbsolutePath());
			}
			init();
		}
	}

	protected File getAbsolutePath(String file, LocationIfRelative location, File f) {
		if (location == LocationIfRelative.DATA)
			f = new File(Constants.STORAGE_PATH, file);
		else if (location == LocationIfRelative.CONF)
			f = new File(Constants.GAIA_CONF_HOME + File.separator + "search", file);
		else {
			throw new IllegalStateException("Cound not recognize location: " + location);
		}
		return f;
	}

	protected abstract void load(YamlBean paramYamlBean);

	protected abstract void init();

	public final synchronized String getFile() {
		return file;
	}

	protected final synchronized void setFile(String name) {
		file = name;
	}

	public final synchronized LocationIfRelative getLocation() {
		return location;
	}

	protected synchronized void save(String filename) throws IOException {
		LOG.debug("Saving {} state to {}", getClass().getSimpleName(), filename);

		File file = new File(filename);
		OSFileWriter fw = new OSFileWriter(file);

		FileOutputStream fos = new FileOutputStream(fw.getWriteFile());
		OutputStreamWriter writer = new OutputStreamWriter(fos, "UTF-8");
		try {
			JavaBeanDumper dumper = new JavaBeanDumper(BeanAccess.FIELD);
			dumper.dump(this, writer);
		} finally {
			writer.close();
			fos.close();
			fw.flush();
		}
		if (!file.exists())
			LOG.error(getClass().getSimpleName() + " file not created at " + file.getAbsolutePath());
	}

	public synchronized void save() {
		try {
			File f = new File(file);
			if ((!f.isAbsolute()) && (!f.isAbsolute())) {
				f = getAbsolutePath(file, location, f);
			}

			save(f.getAbsolutePath());
		} catch (IOException e) {
			LOG.error("Can't save " + getClass().getSimpleName() + " state to " + file, e);
		}
	}

	public static enum LocationIfRelative {
		DATA, CONF;
	}
}
