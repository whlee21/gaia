package gaia.bigdata.api.analytics;

import org.restlet.resource.Post;

public interface MetricResource {
	@Post
	public MetricResponse getMetrics(MetricRequest paramMetricRequest);
}
