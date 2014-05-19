package gaia.utils;

import java.net.URI;

public class FilenameIdGenerator implements IdGenerator<String> {
	public String getId(URI uri) {
		return uri.toString();
	}

	public void reset() {
	}
}
