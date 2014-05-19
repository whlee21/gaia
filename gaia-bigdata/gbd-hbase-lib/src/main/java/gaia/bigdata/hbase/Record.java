package gaia.bigdata.hbase;

import java.util.Map;

public class Record<T extends Key> {
	public final T key;
	public final Map<String, Object> values;

	public Record(T key, Map<String, Object> values) {
		this.key = key;
		this.values = values;
	}

	public String toString() {
		return "Record [key=" + key + ", values=" + values + "]";
	}
}
