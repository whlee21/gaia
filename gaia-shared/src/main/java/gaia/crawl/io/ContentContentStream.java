package gaia.crawl.io;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import org.apache.solr.common.util.ContentStreamBase;

public class ContentContentStream extends ContentStreamBase {
	Content c;

	public ContentContentStream(Content c) {
		this.c = c;
		contentType = c.getFirstMetaValue("Content-Type");
		if (contentType == null) {
			contentType = "application/octet-stream";
		}
		name = c.getKey();
		size = Long.valueOf(c.getData() != null ? c.getData().length : 0);
	}

	public InputStream getStream() throws IOException {
		return new ByteArrayInputStream(c.getData() != null ? c.getData() : new byte[0]);
	}
}
