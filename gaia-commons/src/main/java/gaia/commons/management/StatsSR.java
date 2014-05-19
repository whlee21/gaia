package gaia.commons.management;

import java.util.HashMap;
import java.util.Map;

import com.google.inject.Inject;
import gaia.commons.api.Configuration;
import gaia.commons.services.BaseServerResource;

public class StatsSR extends BaseServerResource implements StatsResource {
	private JMXMonitoredMap jmx;

	@Inject
	public StatsSR(Configuration configuration, JMXMonitoredMap jmx) {
		super(configuration);
		this.jmx = jmx;
	}

	public Map<String, Object> getStatistics() {
		Map<String, Object> result = new HashMap<String, Object>(jmx.size());
		for (Map.Entry<String, GaiaMBean> entry : jmx.entrySet()) {
			result.put(entry.getKey(), ((GaiaMBean) entry.getValue()).getStatistics());
		}
		return result;
	}
}
