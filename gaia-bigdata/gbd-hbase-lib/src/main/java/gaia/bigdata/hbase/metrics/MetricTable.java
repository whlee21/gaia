package gaia.bigdata.hbase.metrics;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HColumnDescriptor;
import org.apache.hadoop.hbase.HTableDescriptor;
import org.apache.hadoop.hbase.client.Delete;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.HTableInterface;
import org.apache.hadoop.hbase.client.HTablePool;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.ResultScanner;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.util.Bytes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gaia.bigdata.hbase.BytesOrJSONSerializer;
import gaia.bigdata.hbase.HBaseTable;
import gaia.bigdata.hbase.Record;
import gaia.bigdata.hbase.ValueSerializer;

public class MetricTable extends HBaseTable {
	public static final byte[] TABLE = Bytes.toBytes("daily_collection_metrics");

	static final byte[] METRICS_CF = Bytes.toBytes("metrics");
	static final byte[] DATE_DIMS_CF = Bytes.toBytes("date_dims");

	static final byte[] DATE_DIMS_YEAR = Bytes.toBytes("year");
	static final byte[] DATE_DIMS_MONTH = Bytes.toBytes("month");
	static final byte[] DATE_DIMS_DATE = Bytes.toBytes("date");
	static final byte[] DATE_DIMS_WEEK_OF_YEAR = Bytes.toBytes("week_of_year");
	static final byte[] DATE_DIMS_DAY_OF_WEEK = Bytes.toBytes("day_of_week");

	private static transient Logger log = LoggerFactory.getLogger(MetricTable.class);
	private static final byte[] nb = { 0 };

	ValueSerializer<MetricKey> keySerializer = new MetricKeySerializer();
	ValueSerializer<Object> valueSerializer = new BytesOrJSONSerializer();

	public MetricTable(String zkConnect) {
		super(zkConnect);
	}

	public MetricTable(HTablePool pool, Configuration conf) {
		super(pool, conf);
	}

	protected HTableDescriptor getTableDescriptor() {
		HTableDescriptor tDesc = new HTableDescriptor(TABLE);
		tDesc.addFamily(new HColumnDescriptor(METRICS_CF));
		tDesc.addFamily(new HColumnDescriptor(DATE_DIMS_CF));
		return tDesc;
	}

	public Record<MetricKey> getMetric(String collection, long timestamp, String metric) throws IOException {
		MetricKey key = new MetricKey(collection, metric, timestamp);
		byte[] row = keySerializer.toBytes(key);
		Get get = new Get(row);
		get.addColumn(METRICS_CF, Bytes.toBytes(metric));
		HTableInterface table = pool.getTable(TABLE);
		Result result = table.get(get);
		table.close();
		Map<String, Object> metricValues = Collections.singletonMap(metric,
				valueSerializer.toObject(result.getValue(METRICS_CF, Bytes.toBytes(metric))));

		return new Record<MetricKey>(key, metricValues);
	}

	public Record<MetricKey> getLatestMetric(String collection, String metric) throws IOException {
		Iterator<Record<MetricKey>> it = getMetricRange(collection, 0L, 9223372036854775807L, metric).iterator();
		if (it.hasNext()) {
			return it.next();
		}
		return null;
	}

	public int deleteMetrics(String collection) throws IOException {
		MetricKey key = new MetricKey(collection, "", 0L);
		byte[] dummyRowKey = Bytes.head(keySerializer.toBytes(key), 2 + collection.getBytes("UTF-8").length);
		byte[] startRow = Bytes.add(dummyRowKey, new byte[] { 30 });
		byte[] stopRow = Bytes.add(dummyRowKey, new byte[] { 31 });

		Scan scan = new Scan(startRow, stopRow);
		List<Delete> deletes = new ArrayList<Delete>();
		int cnt = 0;
		HTableInterface table = pool.getTable(TABLE);
		for (Result result : table.getScanner(scan)) {
			deletes.add(new Delete(result.getRow()));
			cnt++;
		}
		table.delete(deletes);
		table.close();
		return cnt;
	}

	public Iterable<Record<MetricKey>> getMetricRange(String collection, long start, long stop, final String metric)
			throws IOException {
		final byte[] startRow = Bytes.add(keySerializer.toBytes(new MetricKey(collection, metric, stop)), nb);
		final byte[] stopRow = Bytes.add(keySerializer.toBytes(new MetricKey(collection, metric, start)), nb);

		log.info("Scanning daily_collection_metrics from [{},{})", Bytes.toStringBinary(startRow),
				Bytes.toStringBinary(stopRow));

		return new Iterable<Record<MetricKey>>() {
			public Iterator<Record<MetricKey>> iterator() {
				HTableInterface table = pool.getTable(MetricTable.TABLE);
				try {
					Scan scan = new Scan(startRow, stopRow);
					scan.addColumn(MetricTable.METRICS_CF, Bytes.toBytes(metric));
					ResultScanner scanner = table.getScanner(scan);
					final Iterator<Result> it = scanner.iterator();
					return new Iterator<Record<MetricKey>>() {
						public boolean hasNext() {
							return it.hasNext();
						}

						public Record<MetricKey> next() {
							try {
								Result result = (Result) it.next();
								MetricKey key = (MetricKey) keySerializer.toObject(result.getRow());
								Map<String, Object> metricValues = Collections.singletonMap(metric,
										valueSerializer.toObject(result.getValue(MetricTable.METRICS_CF, Bytes.toBytes(metric))));

								return new Record<MetricKey>(key, metricValues);
							} catch (IOException e) {
								throw new RuntimeException(e);
							}
						}

						public void remove() {
							throw new UnsupportedOperationException("This is a read-only iterator");
						}
					};
				} catch (IOException e) {
					throw new RuntimeException(e);
				} finally {
					try {
						table.close();
					} catch (IOException e) {
						throw new RuntimeException(e);
					}
				}
			}
		};
	}

	public void putMetric(String collection, long timestamp, String metric, Object value) throws IOException {
		MetricKey key = new MetricKey(collection, metric, timestamp);
		byte[] row = keySerializer.toBytes(key);
		Put put = new Put(row);

		attachDateDims(put, timestamp);

		put.add(METRICS_CF, Bytes.toBytes(metric), valueSerializer.toBytes(value));
		HTableInterface table = pool.getTable(TABLE);
		table.put(put);
		table.close();
	}

	private void attachDateDims(Put put, long timestamp) throws IOException {
		Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
		cal.setTimeInMillis(timestamp);
		put.add(DATE_DIMS_CF, DATE_DIMS_YEAR, valueSerializer.toBytes(Short.valueOf((short) cal.get(1))));
		put.add(DATE_DIMS_CF, DATE_DIMS_MONTH, valueSerializer.toBytes(Byte.valueOf((byte) cal.get(2))));
		put.add(DATE_DIMS_CF, DATE_DIMS_DATE, valueSerializer.toBytes(Byte.valueOf((byte) cal.get(5))));
		put.add(DATE_DIMS_CF, DATE_DIMS_WEEK_OF_YEAR, valueSerializer.toBytes(Byte.valueOf((byte) cal.get(3))));
		put.add(DATE_DIMS_CF, DATE_DIMS_DAY_OF_WEEK, valueSerializer.toBytes(Byte.valueOf((byte) cal.get(8))));
	}

	protected byte[][] getSplits() {
		return new byte[0][];
	}
}
