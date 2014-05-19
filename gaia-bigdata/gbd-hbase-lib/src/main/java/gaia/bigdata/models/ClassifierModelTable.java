package gaia.bigdata.models;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.Map;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HColumnDescriptor;
import org.apache.hadoop.hbase.HTableDescriptor;
import org.apache.hadoop.hbase.client.Delete;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.HTableInterface;
import org.apache.hadoop.hbase.client.HTablePool;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.filter.CompareFilter;
import org.apache.hadoop.hbase.filter.Filter;
import org.apache.hadoop.hbase.filter.RegexStringComparator;
import org.apache.hadoop.hbase.filter.RowFilter;
import org.apache.hadoop.hbase.filter.SingleColumnValueFilter;
import org.apache.hadoop.hbase.util.Bytes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gaia.bigdata.api.classification.ClassifierModel;
import gaia.bigdata.api.classification.ModelProvider;
import gaia.bigdata.hbase.BytesOrJSONSerializer;
import gaia.bigdata.hbase.HBaseTable;
import gaia.bigdata.hbase.ValueSerializer;

public class ClassifierModelTable extends HBaseTable {
	private static transient Logger log = LoggerFactory.getLogger(ClassifierModelTable.class);
	public static final byte[] TABLE = Bytes.toBytes("classifier_models");
	public static final byte[] MODEL_CF = Bytes.toBytes("model");
	public static final byte[] NAME_Q = Bytes.toBytes("name");
	public static final byte[] LOCATION_Q = Bytes.toBytes("location");
	public static final byte[] TYPE_Q = Bytes.toBytes("type");
	public static final byte[] PROVIDER_Q = Bytes.toBytes("provider");
	public static final byte[] METADATA_Q = Bytes.toBytes("metadata");
	public static final byte[] VERSION_Q = Bytes.toBytes("version");
	public static final byte[] NUMCATS_Q = Bytes.toBytes("numCategories");

	static final ValueSerializer<ClassifierModelKey> keySerializer = new ClassifierModelKeySerializer();
	static final ValueSerializer<Object> valueSerializer = new BytesOrJSONSerializer();

	public ClassifierModelTable(String zkConnect) {
		super(zkConnect);
	}

	public ClassifierModelTable(HTablePool pool, Configuration conf) {
		super(pool, conf);
	}

	public HTableDescriptor getTableDescriptor() {
		HTableDescriptor result = new HTableDescriptor(TABLE);
		result.addFamily(new HColumnDescriptor(MODEL_CF));
		return result;
	}

	public Iterable<ClassifierModel> listModels() {
		Scan scan = new Scan();
		scan.addFamily(MODEL_CF);
		return wrapResultIterable(scan);
	}

	public Iterable<ClassifierModel> grepModelByName(String pattern) throws IOException {
		return grepModel(new RowFilter(CompareFilter.CompareOp.EQUAL, new RegexStringComparator(pattern)));
	}

	public Iterable<ClassifierModel> grepModel(Filter filter) throws IOException {
		Scan scan = new Scan();
		scan.addFamily(MODEL_CF);
		scan.setFilter(filter);
		return wrapResultIterable(scan);
	}

	public Iterable<ClassifierModel> grepModel(String column, String pattern) throws IOException {
		SingleColumnValueFilter filter = new SingleColumnValueFilter(MODEL_CF, Bytes.toBytes(column),
				CompareFilter.CompareOp.EQUAL, new RegexStringComparator(pattern));

		filter.setFilterIfMissing(true);
		filter.setLatestVersionOnly(true);
		return grepModel(filter);
	}

	public Iterable<ClassifierModel> grepModel(String pattern) throws IOException {
		return grepModel("location", pattern);
	}

	public void putModel(ClassifierModel model) throws IOException {
		HTableInterface table = pool.getTable(TABLE);
		Put put = newPut(model);
		boolean ok;
		if (model.getVersion() == 0L)
			ok = table.checkAndPut(put.getRow(), MODEL_CF, VERSION_Q, null, put);
		else {
			ok = table.checkAndPut(put.getRow(), MODEL_CF, VERSION_Q,
					valueSerializer.toBytes(Long.valueOf(model.getVersion())), put);
		}
		if (!ok) {
			throw new ConcurrentModificationException("Version mismatch in Document record - HBase was not updated");
		}
		table.close();
	}

	protected Put newPut(ClassifierModel model) throws IOException {
		ClassifierModelKey key = new ClassifierModelKey(model.getName());
		byte[] row = keySerializer.toBytes(key);
		Put put = new Put(row);

		if (model.getLocation() != null) {
			put.add(MODEL_CF, LOCATION_Q, valueSerializer.toBytes(model.getLocation()));
		}
		if (model.getType() != null) {
			put.add(MODEL_CF, TYPE_Q, valueSerializer.toBytes(model.getType()));
		}
		if (model.getMetadata() != null) {
			put.add(MODEL_CF, METADATA_Q, valueSerializer.toBytes(model.getMetadata()));
		}
		if (model.getNumCategories() != 0) {
			put.add(MODEL_CF, NUMCATS_Q, valueSerializer.toBytes(Integer.valueOf(model.getNumCategories())));
		}
		if (model.getProvider() != null) {
			put.add(MODEL_CF, PROVIDER_Q, valueSerializer.toBytes(model.getProvider()));
		}

		put.add(MODEL_CF, VERSION_Q, valueSerializer.toBytes(Long.valueOf(model.getVersion() + 1L)));
		return put;
	}

	public ClassifierModel getModel(String modelName) throws IOException {
		return getModel(modelName, 0);
	}

	public ClassifierModel getModel(String modelName, int mask) throws IOException {
		Get get = newGet(modelName, mask);
		HTableInterface table = pool.getTable(TABLE);
		Result result = table.get(get);
		table.close();

		return resultToModel(result);
	}

	public void deleteModel(String modelName) throws IOException {
		Delete delete = newDelete(modelName);
		HTableInterface table = pool.getTable(TABLE);
		table.delete(delete);
		table.close();
	}

	protected ClassifierModel resultToModel(Result result) throws IOException {
		if ((result == null) || (result.isEmpty())) {
			return null;
		}
		ClassifierModel model = new ClassifierModel();

		ClassifierModelKey key = (ClassifierModelKey) keySerializer.toObject(result.getRow());
		model.setName(key.modelName);

		if (result.containsColumn(MODEL_CF, LOCATION_Q)) {
			try {
				model.setLocation(new URI((String) valueSerializer.toObject(result.getValue(MODEL_CF, LOCATION_Q))));
			} catch (URISyntaxException e) {
				log.error("Exception", e);
			}
		}

		if (result.containsColumn(MODEL_CF, TYPE_Q)) {
			model.setType((String) valueSerializer.toObject(result.getValue(MODEL_CF, TYPE_Q)));
		}
		if (result.containsColumn(MODEL_CF, PROVIDER_Q)) {
			model
					.setProvider(ModelProvider.valueOf((String) valueSerializer.toObject(result.getValue(MODEL_CF, PROVIDER_Q))));
		}
		if (result.containsColumn(MODEL_CF, METADATA_Q)) {
			model.setMetadata((Map) valueSerializer.toObject(result.getValue(MODEL_CF, METADATA_Q)));
		}
		if (result.containsColumn(MODEL_CF, VERSION_Q)) {
			model.setVersion(((Long) valueSerializer.toObject(result.getValue(MODEL_CF, VERSION_Q))).longValue());
		}
		if (result.containsColumn(MODEL_CF, NUMCATS_Q)) {
			model.setNumCategories(((Integer) valueSerializer.toObject(result.getValue(MODEL_CF, NUMCATS_Q))).intValue());
		}
		return model;
	}

	public static Delete newDelete(String modelName) throws IOException {
		return new Delete(keySerializer.toBytes(new ClassifierModelKey(modelName)));
	}

	public static Get newGet(String modelName, int mask) throws IOException {
		Get get = new Get(keySerializer.toBytes(new ClassifierModelKey(modelName)));

		get.addColumn(MODEL_CF, NAME_Q);
		get.addColumn(MODEL_CF, LOCATION_Q);
		get.addColumn(MODEL_CF, TYPE_Q);
		get.addColumn(MODEL_CF, PROVIDER_Q);
		get.addColumn(MODEL_CF, METADATA_Q);
		get.addColumn(MODEL_CF, VERSION_Q);
		get.addColumn(MODEL_CF, NUMCATS_Q);

		return get;
	}

	private Iterable<ClassifierModel> wrapResultIterable(final Scan scan) {
		return new Iterable<ClassifierModel>() {
			public Iterator<ClassifierModel> iterator() {
				HTableInterface table = pool.getTable(ClassifierModelTable.TABLE);
				try {
					final Iterator<Result> results = table.getScanner(scan).iterator();
					return new Iterator<ClassifierModel>() {
						public boolean hasNext() {
							return results.hasNext();
						}

						public ClassifierModel next() {
							Result result = (Result) results.next();
							try {
								return ClassifierModelTable.this.resultToModel(result);
							} catch (IOException e) {
								throw new RuntimeException(e);
							}
						}

						public void remove() {
							throw new UnsupportedOperationException();
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

	protected byte[][] getSplits() {
		return new byte[0][];
	}
}
