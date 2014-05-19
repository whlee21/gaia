package gaia.bigdata.api.analytics;

import java.util.Date;
import java.util.Map;

import org.codehaus.jackson.map.annotate.JsonSerialize;

@JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
public class MetricResponse {
	public String collection;
	public Map<String, MetricResponseSpec> metrics;

	public String toString() {
		return "MetricResponse [collection=" + collection + ", metrics=" + metrics + "]";
	}

	@JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
	public static class MetricResponseSpec {
		public Date startDate;
		public Date endDate;
		public Map<String, Number> aggregates;
		public Map<Long, Object> series;

		public String toString() {
			return "MetricResponseSpec [startDate=" + startDate + ", endDate=" + endDate + ", aggregates=" + aggregates
					+ ", series=" + series + "]";
		}
	}
}
