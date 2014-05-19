package gaia.crawl.fs;

import java.io.InputStream;
import java.util.List;
import org.apache.solr.common.SolrInputDocument;

public abstract class FSObject {
	protected static final FSObject[] EMPTY_KIDS = new FSObject[0];
	protected static final String[] EMPTY_ACLS = new String[0];
	protected long lastModified;
	protected long size;
	protected String name;
	protected String owner;
	protected String group;
	protected String uri;
	protected String[] acls;
	protected boolean directory;
	protected List<SolrInputDocument> docs;

	public abstract Iterable<FSObject> getChildren() throws Throwable;

	public abstract InputStream open() throws Throwable;

	public void dispose() {
	}

	public boolean isDirectory() {
		return directory;
	}

	public long getLastModified() {
		return lastModified;
	}

	public long getSize() {
		return size;
	}

	public String getUri() {
		return uri;
	}

	public String getName() {
		return name;
	}

	public String[] getAcls() {
		return acls;
	}

	public String getOwner() {
		return owner;
	}

	public String getGroup() {
		return group;
	}

	public List<SolrInputDocument> getDocuments() {
		return docs;
	}

	public void setDocuments(List<SolrInputDocument> docs) {
		this.docs = docs;
	}

	public String toString() {
		return uri;
	}
}
