package gaia.crawl.batch.file;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Map;

import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.update.AddUpdateCommand;
import org.apache.solr.update.CommitUpdateCommand;
import org.apache.solr.update.DeleteUpdateCommand;
import org.apache.solr.update.UpdateCommand;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonToken;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gaia.crawl.batch.BatchSolrReader;

class SolrFileReader extends BatchSolrReader {
	public static final Logger LOG = LoggerFactory.getLogger(SolrFileReader.class);
	ObjectMapper mapper = new ObjectMapper();
	JsonParser p;
	Reader reader;

	public SolrFileReader(File input) throws IOException {
		reader = new BufferedReader(new InputStreamReader(new FileInputStream(input), "UTF-8"));

		p = mapper.getJsonFactory().createJsonParser(reader);
		JsonToken t = p.nextToken();
		if (!t.asString().equals("[")) {
			throw new IOException("Invalid SolrFile preamble '" + t.asString() + "', should be '['");
		}
		p.nextToken();
	}

	public UpdateCommand next() throws IOException {
		Map map = null;
		boolean got = false;
		try {
			while ((map = (Map) mapper.readValue(p, Map.class)) != null)
				if (map.size() != 1) {
					LOG.warn("invalid object in stream, size=" + map.size() + ": " + map);
				} else
					got = true;
		} catch (EOFException e) {
			got = false;
		}
		if ((!got) || (map == null)) {
			return null;
		}
		String key = (String) map.keySet().iterator().next();
		Object o = map.get(key);
		if (!(o instanceof Map)) {
			LOG.warn("invalid command: " + key + ", value=" + o);
			return InvalidUpdateCommand.INSTANCE;
		}
		Map subMap = (Map) o;
		if (key.equals("commit"))
			return new CommitUpdateCommand(null, false);
		if (key.equals("delete")) {
			String id = (String) subMap.get("id");
			DeleteUpdateCommand del = new DeleteUpdateCommand(null);
			if (id != null) {
				del.id = id;
			} else {
				String q = (String) subMap.get("query");
				del.query = q;
			}
			return del;
		}
		if (key.equals("add")) {
			Map<Object, Object> doc = (Map) subMap.get("doc");
			SolrInputDocument d = new SolrInputDocument();
			if (subMap.containsKey("boost")) {
				d.setDocumentBoost(((Number) subMap.get("boost")).floatValue());
			}
			for (Map.Entry<Object, Object> e : doc.entrySet()) {
				String fldName = (String) e.getKey();
				float boost = 1.0F;
				Map fv = (Map) e.getValue();
				if (fv.containsKey("boost")) {
					boost = ((Number) fv.get("boost")).floatValue();
				}
				d.addField(fldName, fv.get("val"), boost);
			}
			AddUpdateCommand add = new AddUpdateCommand(null);
			add.solrDoc = d;
			return add;
		}
		LOG.warn("invalid command: " + key + ", map=" + map);
		return InvalidUpdateCommand.INSTANCE;
	}

	public void close() throws IOException {
		p.close();
	}

	public static final class InvalidUpdateCommand extends UpdateCommand {
		static final InvalidUpdateCommand INSTANCE = new InvalidUpdateCommand(null);

		public InvalidUpdateCommand(SolrQueryRequest req) {
			super(req);
		}

		public String name() {
			return "invalid";
		}
	}
}
