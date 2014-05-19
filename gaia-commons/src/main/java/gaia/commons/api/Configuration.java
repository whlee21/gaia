package gaia.commons.api;

import java.util.List;
import java.util.Map;
import java.util.Properties;

public interface Configuration {
	public Properties getProperties();

	public Map<String, List<String>> getStartupArgs();
}
