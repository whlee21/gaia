package gaia.utils;

import java.net.URI;
import java.util.Date;

public class DateIdGenerator implements IdGenerator<Date> {
	public Date getId(URI uri) {
		return new Date();
	}

	public void reset() {
	}
}
