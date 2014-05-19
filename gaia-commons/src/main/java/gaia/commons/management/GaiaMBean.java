package gaia.commons.management;

import java.util.Map;

public interface GaiaMBean {
	public String getName();

	public String getDescription();

	public Map<String, Object> getStatistics();
}
