package gaia.bigdata.analytics;

import gaia.bigdata.hbase.Record;
import gaia.bigdata.hbase.metrics.MetricKey;
import java.util.Date;
import java.util.Set;

public interface AnalyticsService {
	public Set<String> getSupportedMetrics();

	public Record<MetricKey> getMetric(String paramString1, String paramString2, Date paramDate);

	public Iterable<Record<MetricKey>> getMetricRange(String paramString1, String paramString2, Date paramDate1,
			Date paramDate2);
}