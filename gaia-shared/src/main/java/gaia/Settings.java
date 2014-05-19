package gaia;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import gaia.yaml.KVYamlBean;

@Singleton
public class Settings extends KVYamlBean {
	private static final Logger LOG = LoggerFactory.getLogger(Settings.class);

	public Settings() {
	}

	@Inject
	public Settings(@Named("settings-filename") String file) {
		super(file, false);
	}

	protected void initDefaultValues() {
		init(Group.control, "blockUpdates", Boolean.valueOf(false));
	}

	public static enum Group implements KVYamlBean.Group {
		control, ssl;
	}
}
