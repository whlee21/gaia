package gaia.utils;

import java.net.URI;

public class IntegerIdGenerator implements IdGenerator<Integer> {
	private int id = 0;

	public Integer getId(URI uri) {
		Integer result = null;
		synchronized (this) {
			result = Integer.valueOf(id++);
		}
		return result;
	}

	public void reset() {
		synchronized (this) {
			id = 0;
		}
	}
}
