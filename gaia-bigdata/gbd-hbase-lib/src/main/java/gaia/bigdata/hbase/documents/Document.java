package gaia.bigdata.hbase.documents;

import com.digitalpebble.behemoth.Annotation;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Document {
	public String id;
	public final String collection;
	public final long version;
	public String contentType;
	public String text;
	public byte[] content;
	public Double boost;
	public List<Annotation> annotations;
	public final Map<String, Object> fields = new HashMap<String, Object>();
	public final Map<String, Double> boosts = new HashMap<String, Double>();
	public final Map<String, Object> calculated = new HashMap<String, Object>();

	public Document(String collection) {
		this("", collection, 0L);
	}

	public Document(String id, String collection) {
		this(id, collection, 0L);
	}

	public Document(String id, String collection, long version) {
		this.id = id;
		this.collection = collection;
		this.version = version;
	}

	public String toString() {
		return String.format("[Document id=%s collection=%s version=%d]",
				new Object[] { id, collection, Long.valueOf(version) });
	}
}
