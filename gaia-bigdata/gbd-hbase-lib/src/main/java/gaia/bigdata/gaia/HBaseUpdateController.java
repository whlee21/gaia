package gaia.bigdata.gaia;

import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.lang.NotImplementedException;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.SolrInputField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gaia.crawl.UpdateController;
import gaia.crawl.datasource.DataSource;
import gaia.bigdata.hbase.documents.Document;
import gaia.bigdata.hbase.documents.DocumentTable;

public class HBaseUpdateController extends UpdateController {
	private static final Logger LOG = LoggerFactory.getLogger(HBaseUpdateController.class);
	protected DocumentTable table;
	protected String collection;
	protected String idField;
	protected boolean overwrite = true;

	public void init(DataSource ds) throws Exception {
		super.init(ds);

		String output = ds.getString("output_args");
		if (output == null) {
			throw new Exception("Configuration error, output_args is null");
		}
		table = new DocumentTable(output);

		String collection = ds.getString("collection");
		if (collection == null) {
			throw new Exception("Configuration error, collection is null");
		}
		this.collection = collection;

		String uniqueKey = ds.getFieldMapping().getUniqueKey();
		if (uniqueKey == null) {
			uniqueKey = "id";
		}
		idField = uniqueKey;
		overwrite = ds.getBoolean("overwrite", true);
	}

	public void add(SolrInputDocument siDoc) throws IOException {
		String hbaseDocId = (String) siDoc.getFieldValue(idField);
		Document hbaseDoc = new Document(hbaseDocId, collection);
		Set<Entry<String, SolrInputField>> entries = siDoc.entrySet();

		for (Map.Entry<String, SolrInputField> entry : entries) {
			String fieldName = (String) entry.getKey();
			if (!fieldName.equals(idField)) {
				Object fieldVal = ((SolrInputField) entry.getValue()).getValue();
				if (fieldName.equals("original_content")) {
					byte[] bytes = (byte[]) fieldVal;
					hbaseDoc.content = bytes;
				} else {
					hbaseDoc.fields.put(fieldName, fieldVal);
				}
			}
		}
		table.putDocument(hbaseDoc, overwrite);
	}

	public void delete(String id) throws IOException {
		table.deleteDocument(id, collection);
	}

	public void deleteByQuery(String query) throws IOException {
		throw new NotImplementedException();
	}

	public void commit() throws IOException {
	}

	public DocumentTable getTable() {
		return table;
	}

	public String getCollection() {
		return collection;
	}

	public String getIdField() {
		return idField;
	}

	public void finish(boolean commit) {
		try {
			table.close();
		} catch (IOException e) {
			LOG.error("Could not close connection to HBase", e);
		}
	}
}
