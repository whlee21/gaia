package gaia.bigdata.api.analytics;

import java.util.Date;
import java.util.List;
import java.util.Map;

public class MetricRequest {
	public Map<String, MetricRequestSpec> metrics;

	public String toString() {
		return "MetricRequest [metrics=" + metrics + "]";
	}

	public static class MetricRequestSpec {
		public Date startDate;
		public Date endDate;
		public List<String> aggregates;
		public boolean includeSeries = true;

		public String toString() {
			return "MetricSpec [startDate=" + startDate + ", endDate=" + endDate + ", aggregates=" + aggregates + ", series="
					+ includeSeries + "]";
		}
	}
}
