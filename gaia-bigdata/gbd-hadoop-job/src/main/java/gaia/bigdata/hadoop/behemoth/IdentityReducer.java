package gaia.bigdata.hadoop.behemoth;

import java.io.IOException;

import org.apache.hadoop.mapreduce.Reducer;

public class IdentityReducer<K, V> extends Reducer<K, V, K, V> {
	/**
	 * Writes all keys and values directly to output.
	 * 
	 * @throws InterruptedException
	 */
	@Override
	protected void reduce(K key, Iterable<V> values, Context context) throws IOException, InterruptedException {
		for (V value : values) {
			context.write(key, value);
		}
	}
}