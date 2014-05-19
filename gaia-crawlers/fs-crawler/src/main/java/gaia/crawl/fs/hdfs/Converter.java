package gaia.crawl.fs.hdfs;

import gaia.crawl.CrawlState;
import gaia.crawl.fs.FSObject;
import gaia.crawl.io.Content;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.FileStatus;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.util.DateUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class Converter extends Configured {
	protected static final Logger LOG = LoggerFactory.getLogger(Converter.class);
	protected CrawlState state;

	public void init(CrawlState state) throws Exception {
		this.state = state;
	}

	public FSObject convert(FileStatus p, Object key, Object value) throws IOException {
		SFObject sfo = new SFObject(p, key);
		List<SolrInputDocument> docs = convert(sfo, key, value);
		sfo.setDocuments(docs);
		return sfo;
	}

	protected void fillMetadata(FSObject fso, SolrInputDocument doc) {
		doc.addField("owner", fso.getOwner());
		doc.addField("group", fso.getGroup());
		doc.addField("Content-Length", String.valueOf(fso.getSize()));
		doc.addField("url", fso.getUri());
		doc.addField("id", fso.getUri());
		doc.addField("isDirectory", String.valueOf(fso.isDirectory()));
		Date date = new Date(fso.getLastModified());
		StringBuilder sb = new StringBuilder();
		try {
			DateUtil.formatDate(date, null, sb);
		} catch (IOException ioe) {
			SFOCrawler.LOG.warn("Cannot format date", ioe);
			sb.setLength(0);
			sb.append(date.toString());
		}
		doc.addField("Last-Modified", sb.toString());
		for (String acl : fso.getAcls())
			doc.addField("acl", acl);
	}

	protected void fillMetadata(FSObject fso, Content c) {
		c.addMetadata("owner", fso.getOwner());
		c.addMetadata("group", fso.getGroup());
		c.addMetadata("Content-Length", String.valueOf(fso.getSize()));
		c.addMetadata("url", fso.getUri());
		c.setKey(fso.getUri());
		c.addMetadata("isDirectory", String.valueOf(fso.isDirectory()));
		Date date = new Date(fso.getLastModified());
		StringBuilder sb = new StringBuilder();
		try {
			DateUtil.formatDate(date, null, sb);
		} catch (IOException ioe) {
			SFOCrawler.LOG.warn("Cannot format date", ioe);
			sb.setLength(0);
			sb.append(date.toString());
		}
		c.addMetadata("Last-Modified", sb.toString());
		for (String acl : fso.getAcls())
			c.addMetadata("acl", acl);
	}

	public abstract List<SolrInputDocument> convert(FSObject paramFSObject, Object paramObject1, Object paramObject2)
			throws IOException;
}
