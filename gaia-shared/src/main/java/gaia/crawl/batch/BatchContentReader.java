package gaia.crawl.batch;

import gaia.crawl.io.Content;
import java.io.Closeable;
import java.io.IOException;

public abstract class BatchContentReader implements Closeable {
	public abstract boolean read(Content paramContent) throws IOException;
}
