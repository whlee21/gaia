package gaia.heartbeat;

import com.google.inject.Inject;
import gaia.admin.collection.CollectionManager;
import gaia.crawl.DataSourceManager;
import com.netflix.servo.BasicMonitorRegistry;
import com.netflix.servo.MonitorRegistry;
import com.netflix.servo.monitor.Monitors;
import com.netflix.servo.publish.BasicMetricFilter;
import com.netflix.servo.publish.MetricObserver;
import com.netflix.servo.publish.MonitorRegistryMetricPoller;
import com.netflix.servo.publish.PollRunnable;
import com.netflix.servo.publish.PollScheduler;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.apache.solr.core.CoreContainer;

public class HeartbeatManager {
	private CollectionManager collectionManager;
	private CoreContainer cores;
	private DataSourceManager dsManager;
	private GaiaStatsPublisher publisher;
	private MonitorRegistry registry;
	private PollScheduler scheduler;

	@Inject
	public HeartbeatManager(CollectionManager collectionManager, CoreContainer cores, DataSourceManager dsManager) {
		this.collectionManager = collectionManager;
		this.cores = cores;
		this.dsManager = dsManager;

		publisher = new GaiaStatsPublisher();

		if (publisher.isEnabled())
			start();
	}

	private void start() {
		ServerMetrics serverStats = new ServerMetrics(collectionManager, cores, dsManager);
		Map<String, String> additionalInfo = new HashMap<String, String>();
		additionalInfo.put("is_solrcloud", serverStats.isSolrCloud().toString());
		if (serverStats.isSolrCloud().booleanValue()) {
			additionalInfo.put("solrcloud_id", serverStats.solrCloudClusterId());
			additionalInfo.put("num_solrcloud_nodes", Integer.toString(serverStats.numSolrCloudNodes()));
		}
		publisher.sendVersionInfo(additionalInfo);

		registry = new BasicMonitorRegistry();
		registry.register(Monitors.newObjectMonitor(serverStats));

		scheduler = PollScheduler.getInstance();
		scheduler.start();

		MetricObserver gaiaObserver = new GaiaMetricObserver(publisher);
		PollRunnable task = new PollRunnable(new MonitorRegistryMetricPoller(registry), BasicMetricFilter.MATCH_ALL,
				new MetricObserver[] { gaiaObserver });

		scheduler.addPoller(task, 7L, TimeUnit.DAYS);
	}

	public void shutdown() {
		if (null != scheduler)
			scheduler.stop();
	}
}
