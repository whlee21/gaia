package gaia.heartbeat;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.netflix.servo.Metric;
import com.netflix.servo.publish.MetricObserver;

public class GaiaMetricObserver implements MetricObserver {
	private GaiaStatsPublisher publisher;

	public GaiaMetricObserver(GaiaStatsPublisher publisher) {
		this.publisher = publisher;
	}

	public String getName() {
		return "GaiaSearch Observer";
	}

	public void update(List<Metric> arg0) {
		Map<String, String> values = new HashMap<String, String>();
		for (Metric metric : arg0) {
			values.put(metric.getConfig().getName(), metric.getValue().toString());
		}
		publisher.publishStats(values);
	}
}
