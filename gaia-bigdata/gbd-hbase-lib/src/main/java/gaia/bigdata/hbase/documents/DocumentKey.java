package gaia.bigdata.hbase.documents;

import gaia.bigdata.hbase.Key;

public class DocumentKey implements Key {
	public final String id;
	public final String collection;

	public DocumentKey(String id, String collection) {
		this.id = id;
		this.collection = collection;
	}

	public String toString() {
		return String.format("[DocumentKey id=%s collection=%s]", new Object[] { id, collection });
	}
}
