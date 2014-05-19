package gaia.crawl.io;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Map;

import org.apache.solr.common.SolrInputDocument;

import gaia.crawl.datasource.FieldMapping;
import gaia.crawl.metadata.Metadata;
import gaia.crawl.metadata.SpellCheckedMetadata;

public class Content {
	public static final String INVALID_KEY = "INVALID";
	private String key = INVALID_KEY;
	private Metadata metadata = new SpellCheckedMetadata();
	private byte[] data = null;
	private static final byte[] EMPTY_DATA = new byte[0];
	private static final String MARKER = "0";

	public Content() {
	}

	public Content(Metadata metadata, byte[] data) {
		this.metadata = metadata;
		this.data = data;
	}

	public void reset() {
		metadata.clear();
		key = INVALID_KEY;
		data = null;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public String getKey() {
		return key;
	}

	public Metadata getMetadata() {
		return metadata;
	}

	public void addMetadata(String key, String value) {
		metadata.add(key, value);
	}

	public byte[] getData() {
		return data;
	}

	public void setData(byte[] data) {
		this.data = data;
	}

	public void readFields(DataInput in) throws IOException {
		reset();
		key = in.readUTF();
		while (true) {
			String k = in.readUTF();
			String v = in.readUTF();
			if ((k.equals(MARKER)) && (v.equals(MARKER))) {
				break;
			}
			metadata.add(k, v);
		}
		int len = in.readInt();
		if (len > 0) {
			data = new byte[len];
			in.readFully(data);
		} else if (len == 0) {
			data = EMPTY_DATA;
		} else {
			data = null;
		}
	}

	public void write(DataOutput out) throws IOException {
		out.writeUTF(key);
		for (Map.Entry<String, String[]> e : metadata.entrySet()) {
			for (String s : (String[]) e.getValue()) {
				out.writeUTF((String) e.getKey());
				out.writeUTF(s);
			}
		}
		out.writeUTF(MARKER);
		out.writeUTF(MARKER);
		if (data == null) {
			out.writeInt(-1);
		} else {
			out.writeInt(data.length);
			if (data.length > 0)
				out.write(data);
		}
	}

	public String getLastMetaValue(String key) {
		String[] res = metadata.getValues(key);
		if ((res == null) || (res.length == 0)) {
			return null;
		}
		return res[(res.length - 1)];
	}

	public String getFirstMetaValue(String key) {
		String[] res = metadata.getValues(key);
		if ((res == null) || (res.length == 0)) {
			return null;
		}
		return res[0];
	}

	public static void fill(SolrInputDocument template, FieldMapping mapping, Content c) {
		template.setField(mapping.getUniqueKey(), c.getKey());
		for (Map.Entry<String, String[]> e : c.getMetadata().entrySet())
			for (String s : (String[]) e.getValue())
				template.addField((String) e.getKey(), s);
	}
}
