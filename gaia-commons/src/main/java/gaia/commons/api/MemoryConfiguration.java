package gaia.commons.api;

import java.util.List;
import java.util.Map;
import java.util.Properties;

public class MemoryConfiguration implements Configuration {
	protected Properties properties;
	protected Map<String, List<String>> args;

	protected MemoryConfiguration() {
	}

	public MemoryConfiguration(Properties props, Map<String, List<String>> args) {
		this.properties = props;
		this.args = args;
	}

	public Properties getProperties() {
		return properties;
	}

	public Map<String, List<String>> getStartupArgs() {
		return args;
	}

	public void setStartupArgs(Map<String, List<String>> args) {
		this.args = args;
	}
}
