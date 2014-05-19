package gaia.crawl.fs;

import gaia.crawl.CrawlState;
import gaia.crawl.datasource.DataSource;
import java.io.IOException;

public abstract class FS {
	protected DataSource fsds;
	protected CrawlState state;

	protected FS(DataSource fsds) {
		this.fsds = fsds;
	}

	public void init(CrawlState state) {
		this.state = state;
	}

	public abstract FSObject get(String paramString) throws IOException;

	public abstract void close();
}
