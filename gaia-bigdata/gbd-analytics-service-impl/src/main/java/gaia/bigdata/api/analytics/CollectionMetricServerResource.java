package gaia.bigdata.api.analytics;

import gaia.bigdata.analytics.AnalyticsService;
import gaia.bigdata.analytics.MetricAggregator;
import gaia.bigdata.analytics.MetricAggregators;
import gaia.bigdata.api.analytics.MetricRequest.MetricRequestSpec;
import gaia.bigdata.api.analytics.MetricResponse.MetricResponseSpec;
import gaia.bigdata.hbase.Record;
import gaia.bigdata.hbase.metrics.MetricKey;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.restlet.data.Status;
import org.restlet.resource.Post;
import org.restlet.resource.ResourceException;
import org.restlet.resource.ServerResource;

import com.google.inject.Inject;

public class CollectionMetricServerResource extends ServerResource implements MetricResource {
	private String collection;
	private final AnalyticsService service;

	@Inject
	public CollectionMetricServerResource(AnalyticsService service) {
		this.service = service;
	}

	protected void doInit() throws ResourceException {
		collection = getRequest().getAttributes().get("collection").toString();
	}

	@Post
	public MetricResponse getMetrics(MetricRequest req) {
		MetricResponse resp = new MetricResponse();
		resp.collection = collection;
		if (req.metrics != null) {
			resp.metrics = new HashMap<String, MetricResponseSpec>(req.metrics.size());
			for (Map.Entry<String, MetricRequestSpec> entry : req.metrics.entrySet()) {
				String metric = (String) entry.getKey();
				MetricRequest.MetricRequestSpec metricReqSpec = (MetricRequest.MetricRequestSpec) entry.getValue();
				System.err.println(metricReqSpec);

				MetricResponse.MetricResponseSpec metricRespSpec = new MetricResponse.MetricResponseSpec();
				metricRespSpec.startDate = metricReqSpec.startDate;
				metricRespSpec.endDate = metricReqSpec.endDate;

				MetricAggregator agg = null;
				if ((metricReqSpec.aggregates != null) && (metricReqSpec.aggregates.size() > 0)) {
					MetricAggregator[] aggs = new MetricAggregator[metricReqSpec.aggregates.size()];
					for (int i = 0; i < aggs.length; i++) {
						try {
							aggs[i] = MetricAggregators.get((String) metricReqSpec.aggregates.get(i));
						} catch (UnsupportedOperationException e) {
							throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, "Unknown aggregator "
									+ (String) metricReqSpec.aggregates.get(i));
						}
					}

					agg = new MetricAggregators.ChainedAggregator(aggs);
				}

				metricRespSpec.series = new HashMap<Long, Object>();
				for (Record<MetricKey> record : service.getMetricRange(collection, metric, metricReqSpec.startDate,
						metricReqSpec.endDate)) {
					Object metricValue = record.values.get(metric);

					if (metricReqSpec.includeSeries == true) {
						metricRespSpec.series.put(Long.valueOf(((MetricKey) record.key).timestamp), metricValue);
					}

					if (agg != null) {
						if (!(metricValue instanceof Number)) {
							throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, "Cannot aggregate on non-numeric data");
						}
						agg.feed((Number) record.values.get(metric));
					}

				}

				if (agg != null)
					metricRespSpec.aggregates = ((Map) agg.get());
				else {
					metricRespSpec.aggregates = Collections.emptyMap();
				}
				resp.metrics.put(metric, metricRespSpec);
			}
		} else {
			resp.metrics = new HashMap<String, MetricResponseSpec>();
		}
		return resp;
	}
}
