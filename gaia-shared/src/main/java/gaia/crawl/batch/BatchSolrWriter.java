package gaia.crawl.batch;

import java.io.Closeable;
import java.io.IOException;
import org.apache.solr.common.SolrInputDocument;

public abstract class BatchSolrWriter implements Closeable {
	public abstract void writeAdd(SolrInputDocument paramSolrInputDocument) throws IOException;

	public abstract void writeCommit() throws IOException;

	public abstract void writeDelete(String paramString) throws IOException;

	public abstract void writeDeleteByQuery(String paramString) throws IOException;
}
