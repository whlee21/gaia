package gaia.crawl.batch;

import gaia.crawl.io.Content;
import java.io.Closeable;
import java.io.IOException;

public abstract class BatchContentWriter implements Closeable {
	public abstract void write(Content paramContent) throws IOException;
}
