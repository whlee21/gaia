package gaia.bigdata.documents;

import gaia.bigdata.hbase.HBaseTable;
import gaia.bigdata.hbase.SuffixKeyValueSerializer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.List;
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
import org.apache.hadoop.hbase.io.compress.Compression;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.hadoop.io.compress.BZip2Codec;
import org.apache.hadoop.io.compress.CompressionInputStream;
import org.apache.hadoop.io.compress.CompressionOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.digitalpebble.behemoth.BehemothDocument;
import com.google.common.base.Functions;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.Ordering;

public class DocumentTable extends HBaseTable {
	private static transient Logger log = LoggerFactory.getLogger(DocumentTable.class);

	public static final byte[] TABLE = Bytes.toBytes("documents");

	public static final byte[] INFO_CF = Bytes.toBytes("info");
	public static final byte[] RAW_CF = Bytes.toBytes("raw");
	public static final byte[] TEXT_CF = Bytes.toBytes("text");
	public static final byte[] FIELD_CF = Bytes.toBytes("field");
	public static final byte[] CALCULATED_CF = Bytes.toBytes("calc");
	public static final byte[] ANNOTATION_CF = Bytes.toBytes("annot");

	public static final byte[] INFO_ID_CQ = Bytes.toBytes("id");
	public static final byte[] INFO_COLLECTION_CQ = Bytes.toBytes("collection");
	public static final byte[] INFO_VERSION_CQ = Bytes.toBytes("version");
	public static final byte[] INFO_CONTENT_TYPE_CQ = Bytes.toBytes("contentType");
	public static final byte[] INFO_BOOST_CQ = Bytes.toBytes("boost");
	public static final byte[] RAW_CONTENT_CQ = Bytes.toBytes("content");
	public static final byte[] TEXT_CONTENT_CQ = Bytes.toBytes("content");
	public static final byte[] ANNOTATION_BLOB_CQ = Bytes.toBytes("blob");
	public static final String CALC_SIMDOC = "simdoc";
	public static final byte[] NB = { 0 };

	private static final SuffixKeyValueSerializer serializer = new SuffixKeyValueSerializer();
	private static final DocumentKeySerializer keySerializer = new DocumentKeySerializer();
	private static final BZip2Codec codec = new BZip2Codec();

	public DocumentTable(String zkConnect) {
		super(zkConnect);
	}

	public DocumentTable(HTablePool pool, Configuration conf) {
		super(pool, conf);
	}

	public HTableDescriptor getTableDescriptor() {
		HTableDescriptor tDesc = new HTableDescriptor(TABLE);
		tDesc.addFamily(new HColumnDescriptor(INFO_CF));
		HColumnDescriptor rawColumn = new HColumnDescriptor(RAW_CF);
		rawColumn.setCompressionType(Compression.Algorithm.GZ);
		tDesc.addFamily(rawColumn);
		HColumnDescriptor textColumn = new HColumnDescriptor(TEXT_CF);
		textColumn.setCompressionType(Compression.Algorithm.GZ);
		tDesc.addFamily(textColumn);
		tDesc.addFamily(new HColumnDescriptor(FIELD_CF));
		tDesc.addFamily(new HColumnDescriptor(CALCULATED_CF));
		tDesc.addFamily(new HColumnDescriptor(ANNOTATION_CF));
		return tDesc;
	}

	public void putDocument(Document doc) throws IOException {
		HTableInterface table = pool.getTable(TABLE);
		putDocument(doc, table, false);
		table.close();
	}

	public void putDocument(Document doc, boolean overwrite) throws IOException {
		HTableInterface table = pool.getTable(TABLE);
		putDocument(doc, table, overwrite);
		table.close();
	}

	public void putDocument(Document doc, HTableInterface table) throws IOException {
		putDocument(doc, table, false);
	}

	public void putDocument(Document doc, HTableInterface table, boolean overwrite) throws IOException {
		Put put = newPut(doc);

		if (!overwrite) {
			boolean ok;
			if (doc.version == 0L)
				ok = table.checkAndPut(put.getRow(), INFO_CF, INFO_VERSION_CQ, null, put);
			else {
				ok = table.checkAndPut(put.getRow(), INFO_CF, INFO_VERSION_CQ, Bytes.toBytes(doc.version), put);
			}
			if (!ok)
				throw new ConcurrentModificationException(
						"Version mismatch in Document record - HBase was not updated for doc: " + doc.id + " with version: "
								+ doc.version);
		} else {
			table.put(put);
		}
	}

	public Document getDocument(String id, String collection) throws IOException {
		return getDocument(id, collection, 0);
	}

	public Document getDocument(String id, String collection, HTableInterface table) throws IOException {
		return getDocument(id, collection, 0, table);
	}

	public Document getDocument(String id, String collection, int mask, HTableInterface table) throws IOException {
		Get get = newGet(id, collection, mask);
		Result result = table.get(get);
		return resultToDocument(result);
	}

	public Document getDocument(String id, String collection, int mask) throws IOException {
		Get get = newGet(id, collection, mask);
		HTableInterface table = pool.getTable(TABLE);
		Result result = table.get(get);
		table.close();
		return resultToDocument(result);
	}

	public void deleteDocument(String id, String collection) throws IOException {
		Delete delete = newDelete(id, collection);
		HTableInterface table = pool.getTable(TABLE);
		table.delete(delete);
		table.close();
	}

	public static Put newPut(Document doc) throws IOException {
		Put put = new Put(keySerializer.toBytes(new DocumentKey(doc.id, doc.collection)));

		put.add(INFO_CF, INFO_ID_CQ, Bytes.toBytes(doc.id));
		put.add(INFO_CF, INFO_COLLECTION_CQ, Bytes.toBytes(doc.collection));

		put.add(INFO_CF, INFO_VERSION_CQ, Bytes.toBytes(doc.version + 1L));

		if (doc.contentType != null) {
			put.add(INFO_CF, INFO_CONTENT_TYPE_CQ, Bytes.toBytes(doc.contentType));
		}

		if (doc.boost != null) {
			put.add(INFO_CF, INFO_BOOST_CQ, Bytes.toBytes(doc.boost.doubleValue()));
		}

		if (doc.text != null) {
			put.add(TEXT_CF, TEXT_CONTENT_CQ, Bytes.toBytes(doc.text));
		}

		if (doc.content != null) {
			put.add(RAW_CF, RAW_CONTENT_CQ, doc.content);
		}

		if (doc.fields != null) {
			SuffixKeyValueSerializer.KeyValue kv = serializer.toBytes("fields", doc.fields);
			put.add(FIELD_CF, (byte[]) kv.key, (byte[]) kv.value);
			if ((doc.boosts != null) && (doc.boosts.size() > 0)) {
				kv = serializer.toBytes("boosts", doc.boosts);
				put.add(FIELD_CF, (byte[]) kv.key, (byte[]) kv.value);
			}

		}

		if (doc.calculated != null) {
			for (Map.Entry<String, Object> calc : doc.calculated.entrySet()) {
				SuffixKeyValueSerializer.KeyValue kv = serializer.toBytes((String) calc.getKey(), calc.getValue());
				put.add(CALCULATED_CF, (byte[]) kv.key, (byte[]) kv.value);
			}

		}

		if (doc.annotations != null) {
			BehemothDocument _doc = new BehemothDocument();
			_doc.setUrl(doc.id);
			_doc.setContent(NB);
			_doc.setAnnotations(doc.annotations);

			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			CompressionOutputStream cos = codec.createOutputStream(baos);
			DataOutputStream dos = new DataOutputStream(cos);
			_doc.write(dos);
			dos.flush();
			cos.finish();
			baos.flush();
			put.add(ANNOTATION_CF, ANNOTATION_BLOB_CQ, baos.toByteArray());
			dos.close();
			cos.close();
			baos.close();
		}
		log.debug("Put: {}", put);

		return put;
	}

	public static Get newGet(String id, String collection, int mask) throws IOException {
		Get get = new Get(keySerializer.toBytes(new DocumentKey(id, collection)));

		get.addColumn(INFO_CF, INFO_ID_CQ);
		get.addColumn(INFO_CF, INFO_COLLECTION_CQ);
		get.addColumn(INFO_CF, INFO_VERSION_CQ);

		get.addColumn(INFO_CF, INFO_CONTENT_TYPE_CQ);
		get.addColumn(INFO_CF, INFO_BOOST_CQ);

		if ((mask & 0x1) == 1) {
			get.addColumn(TEXT_CF, TEXT_CONTENT_CQ);
		}

		if ((mask & 0x2) == 2) {
			get.addColumn(RAW_CF, RAW_CONTENT_CQ);
		}

		if ((mask & 0x4) == 4) {
			get.addFamily(FIELD_CF);
		}

		if ((mask & 0x8) == 8) {
			get.addFamily(CALCULATED_CF);
		}

		if ((mask & 0x10) == 16) {
			get.addColumn(ANNOTATION_CF, ANNOTATION_BLOB_CQ);
		}
		log.debug("Get: {}", get);
		return get;
	}

	public static Delete newDelete(String id, String collection) throws IOException {
		Delete delete = new Delete(keySerializer.toBytes(new DocumentKey(id, collection)));
		return delete;
	}

	public static Document resultToDocument(Result result) throws IOException {
		if ((result == null) || (result.isEmpty())) {
			return null;
		}

		Document doc = new Document(Bytes.toString(result.getValue(INFO_CF, INFO_ID_CQ)), Bytes.toString(result.getValue(
				INFO_CF, INFO_COLLECTION_CQ)), Bytes.toLong(result.getValue(INFO_CF, INFO_VERSION_CQ)));

		DocumentKey key = keySerializer.toObject(result.getRow());
		assert (doc.id.equals(key.id));
		assert (doc.collection.equals(key.collection));

		if (result.containsColumn(INFO_CF, INFO_CONTENT_TYPE_CQ)) {
			doc.contentType = Bytes.toString(result.getValue(INFO_CF, INFO_CONTENT_TYPE_CQ));
		}

		if (result.containsColumn(INFO_CF, INFO_BOOST_CQ)) {
			doc.boost = Double.valueOf(Bytes.toDouble(result.getValue(INFO_CF, INFO_BOOST_CQ)));
		}

		if (result.containsColumn(TEXT_CF, TEXT_CONTENT_CQ)) {
			doc.text = Bytes.toString(result.getValue(TEXT_CF, TEXT_CONTENT_CQ));
		}

		if (result.containsColumn(RAW_CF, RAW_CONTENT_CQ)) {
			doc.content = result.getValue(RAW_CF, RAW_CONTENT_CQ);
		}

		Map<byte[], byte[]> fieldMap = result.getFamilyMap(FIELD_CF);
		if (fieldMap != null) {
			for (Map.Entry<byte[], byte[]> column : fieldMap.entrySet()) {
				SuffixKeyValueSerializer.KeyValue<String, Object> kv = serializer.toObject((byte[]) column.getKey(),
						(byte[]) column.getValue());
				if (((String) kv.key).equals("fields"))
					doc.fields.putAll((Map) kv.value);
				else if (((String) kv.key).equals("boosts"))
					doc.boosts.putAll((Map) kv.value);
				else {
					log.warn("Unexpected column qualifier: {}", kv.key);
				}
			}
		}

		Map<byte[], byte[]> calcMap = result.getFamilyMap(CALCULATED_CF);
		if (calcMap != null) {
			for (Map.Entry<byte[], byte[]> column : calcMap.entrySet()) {
				SuffixKeyValueSerializer.KeyValue<String, Object> kv = serializer.toObject((byte[]) column.getKey(),
						(byte[]) column.getValue());
				doc.calculated.put(kv.key, kv.value);
			}

		}

		if (result.containsColumn(ANNOTATION_CF, ANNOTATION_BLOB_CQ)) {
			byte[] ba = result.getValue(ANNOTATION_CF, ANNOTATION_BLOB_CQ);
			if (ba != null) {
				ByteArrayInputStream bais = new ByteArrayInputStream(ba);
				CompressionInputStream cis = codec.createInputStream(bais);
				DataInputStream dis = new DataInputStream(cis);
				BehemothDocument _doc = new BehemothDocument();
				_doc.readFields(dis);
				doc.annotations = _doc.getAnnotations();
				dis.close();
				cis.close();
				bais.close();
			}
		}

		return doc;
	}

	public Map<String, Double> listSimilarDocuments(String id, String collection) throws IOException {
		Document doc = getDocument(id, collection, 8);
		Map<String, Double> similarDocIdsWithScore = null;
		if (doc != null) {
			Map unsortedMap = (Map) doc.calculated.get("simdoc");
			if (unsortedMap != null) {
				Comparator valueComparator = Ordering.natural().reverse().onResultOf(Functions.forMap(unsortedMap))
						.compound(Ordering.natural());

				similarDocIdsWithScore = ImmutableSortedMap.copyOf(unsortedMap, valueComparator);
			}
		}
		return similarDocIdsWithScore;
	}

	public void setSimilarDocuments(Document doc, Map<String, Double> similarDocIdsWithScore) {
		doc.calculated.put("simdoc", similarDocIdsWithScore);
	}

	public void deleteAllDocuments(String collection) throws IOException {
		byte[][] rowRange = keySerializer.toByteRange(collection);
		Scan scan = new Scan(rowRange[0], rowRange[1]);
		scan.setBatch(100);

		HTableInterface table = pool.getTable(TABLE);
		DistributedScanner scanner = new DistributedScanner(table, scan);
		log.info("Delete: " + scan);
		int batchSize = 100;
		List batch = new ArrayList(batchSize);
		try {
			for (Result result : scanner) {
				if (batch.size() >= batchSize) {
					table.delete(batch);
					batch.clear();
				}
				byte[] row = result.getRow();
				log.debug("Deleting: " + keySerializer.toObject(row));
				batch.add(new Delete(row));
			}

			if (batch.size() > 0)
				table.delete(batch);
		} finally {
			scanner.close();
		}
		table.close();
	}

	protected byte[][] getSplits() {
		log.info("DocumentTable has " + DocumentKeySerializer.getNumberOfRegions() + " splits.");
		return DocumentKeySerializer.getSplits();
	}

	public static class DocumentFields {
		public static final int TEXT = 1;
		public static final int RAW = 2;
		public static final int FIELD = 4;
		public static final int CALCULATED = 8;
		public static final int ANNOTATION = 16;
		public static final int ALL = 31;
	}
}
