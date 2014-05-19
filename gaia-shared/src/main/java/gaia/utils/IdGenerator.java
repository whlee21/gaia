package gaia.utils;

import java.net.URI;

public interface IdGenerator<T> {
	public T getId(URI paramURI);

	public void reset();
}
