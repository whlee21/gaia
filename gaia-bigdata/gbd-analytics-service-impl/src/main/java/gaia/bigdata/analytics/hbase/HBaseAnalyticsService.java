package gaia.bigdata.analytics.hbase;

import gaia.bigdata.analytics.AnalyticsService;

import java.io.IOException;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import gaia.commons.api.Configuration;
import gaia.bigdata.hbase.Record;
import gaia.bigdata.hbase.metrics.MetricKey;
import gaia.bigdata.hbase.metrics.MetricTable;

public class HBaseAnalyticsService implements AnalyticsService {
	private static transient Logger log = LoggerFactory.getLogger(HBaseAnalyticsService.class);
	private final MetricTable table;

	@Inject
	public HBaseAnalyticsService(Configuration config) {
		String zkConnect = config.getProperties().getProperty("hbase.zk.connect");
		if (zkConnect == null) {
			throw new IllegalArgumentException("Missing required config value: hbase.zk.connect");
		}
		table = new MetricTable(zkConnect);
	}

	public Set<String> getSupportedMetrics() {
		return Collections.emptySet();
	}

	public Record<MetricKey> getMetric(String collection, String name, Date date) {
		try {
			Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
			cal.setTime(date);
			return table.getMetric(collection, cal.getTimeInMillis(), name);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public Iterable<Record<MetricKey>> getMetricRange(final String collection, final String name, Date startDate,
			Date endDate) {
		if ((startDate == null) || (endDate == null)) {
			return new Iterable<Record<MetricKey>>() {
				public Iterator<Record<MetricKey>> iterator() {
					return new Iterator<Record<MetricKey>>() {
						private final AtomicBoolean doNext = new AtomicBoolean(true);

						public boolean hasNext() {
							return doNext.compareAndSet(true, false);
						}

						public Record<MetricKey> next() {
							try {
								return table.getLatestMetric(collection, name);
							} catch (IOException e) {
								throw new RuntimeException(e);
							}
						}

						public void remove() {
							throw new UnsupportedOperationException("This iterator does not support removing elements");
						}
					};
				}
			};
		}
		Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
		cal.setTime(startDate);
		long startTime = cal.getTimeInMillis();
		cal.setTime(endDate);
		long endTime = cal.getTimeInMillis();
		try {
			log.info("Getting metric values for {} from {} to {}",
					new Object[] { name, Long.valueOf(startTime), Long.valueOf(endTime) });
			return table.getMetricRange(collection, startTime, endTime, name);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
