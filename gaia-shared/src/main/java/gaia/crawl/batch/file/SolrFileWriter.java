package gaia.crawl.batch.file;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.SolrInputField;
import org.apache.solr.common.util.DateUtil;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.ObjectMapper;

import gaia.crawl.batch.BatchSolrWriter;

public class SolrFileWriter extends BatchSolrWriter {
	Writer out;
	JsonGenerator jg;
	boolean open = false;
	boolean dirty = false;
	ObjectMapper mapper = new ObjectMapper();
	HashMap<String, Object> map = new HashMap<String, Object>();
	HashMap<String, Object> subMap = new HashMap<String, Object>();

	public SolrFileWriter(File output) throws IOException {
		if (!output.getParentFile().exists()) {
			output.getParentFile().mkdirs();
		}
		mapper.configure(DeserializationConfig.Feature.FAIL_ON_UNKNOWN_PROPERTIES, true);
		out = new OutputStreamWriter(new FileOutputStream(output), "UTF-8");

		out.write("[\n");
		jg = mapper.getJsonFactory().createJsonGenerator(out);
		open = true;
	}

	public void writeAdd(SolrInputDocument doc) throws IOException {
		if (doc == null) {
			return;
		}
		if (dirty)
			out.write(",\n");
		else {
			dirty = true;
		}
		map.clear();
		subMap.clear();
		Map<String, Object> fields = new HashMap<String, Object>();
		StringBuilder sb = new StringBuilder();
		for (Map.Entry<String, SolrInputField> e : doc.entrySet())
			if ((e.getKey() != null) && (e.getValue() != null)) {
				Map<String, Object> val = new HashMap<String, Object>();
				Collection<Object> vals = e.getValue().getValues();
				if ((vals != null) && (vals.size() > 0)) {
					Object v = vals.iterator().next();
					if ((v instanceof Date)) {
						Collection<Object> newVals = new ArrayList<Object>(vals.size());
						for (Object o : vals) {
							sb.setLength(0);
							DateUtil.formatDate((Date) o, null, sb);
							newVals.add(sb.toString());
						}
						vals = newVals;
					}

					val.put("val", vals);
					if (((SolrInputField) e.getValue()).getBoost() != 1.0F) {
						val.put("boost", Float.valueOf(((SolrInputField) e.getValue()).getBoost()));
					}
					fields.put(e.getKey(), val);
				}
			}
		subMap.put("doc", fields);
		if (doc.getDocumentBoost() != 1.0F) {
			subMap.put("boost", Float.valueOf(doc.getDocumentBoost()));
		}
		map.put("add", subMap);
		mapper.writeValue(jg, map);
	}

	public void writeDelete(String id) throws IOException {
		if (id == null) {
			return;
		}
		if (dirty)
			out.write(",\n");
		else {
			dirty = true;
		}
		map.clear();
		subMap.clear();
		subMap.put("id", id);
		map.put("delete", subMap);
		mapper.writeValue(jg, map);
	}

	public void writeDeleteByQuery(String query) throws IOException {
		if (query == null) {
			return;
		}
		if (dirty)
			out.write(",\n");
		else {
			dirty = true;
		}
		map.clear();
		subMap.clear();
		subMap.put("query", query);
		map.put("delete", subMap);
		mapper.writeValue(jg, map);
	}

	public void writeCommit() throws IOException {
		if (dirty)
			out.write(",\n");
		else {
			dirty = true;
		}
		map.clear();
		subMap.clear();
		map.put("commit", subMap);
		mapper.writeValue(jg, map);
	}

	public boolean isOpen() throws IOException {
		return open;
	}

	public void close() throws IOException {
		out.write("\n]");
		out.flush();
		jg.close();
		open = false;
	}
}
