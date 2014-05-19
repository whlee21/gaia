package gaia.commons.management;

import gaia.commons.api.APIModule;

public class MonitoringAPIModule extends APIModule {
	private JMXMonitoredMap jmx;

	public MonitoringAPIModule(JMXMonitoredMap jmx) {
		this.jmx = jmx;
	}

	protected void defineBindings() {
		bind(InfoResource.class).to(InfoSR.class);
		bind(JMXMonitoredMap.class).toInstance(this.jmx);
	}
}
