package gaia.crawl.fs.hdfs;

import gaia.crawl.CrawlState;
import gaia.crawl.CrawlStatus;
import gaia.crawl.CrawlerUtils;
import gaia.crawl.TikaCrawlProcessor;
import gaia.crawl.fs.FSObject;
import gaia.crawl.impl.TikaParserController;
import gaia.crawl.io.Content;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import org.apache.hadoop.io.BytesWritable;
import org.apache.hadoop.io.Text;
import org.apache.solr.common.SolrInputDocument;

public class DefaultConverter extends Converter {
	TikaParserController tika;

	public void init(CrawlState state) throws Exception {
		super.init(state);
		tika = ((TikaCrawlProcessor) state.getProcessor()).getParserController();
	}

	public List<SolrInputDocument> convert(FSObject fso, Object key, Object value) throws IOException {
		byte[] data = null;
		int offset = 0;
		int length = 0;
		if ((value instanceof BytesWritable)) {
			data = ((BytesWritable) value).getBytes();
			offset = 0;
			length = ((BytesWritable) value).getLength();
		} else if ((value instanceof Text)) {
			data = ((Text) value).getBytes();
			offset = 0;
			length = ((Text) value).getLength();
		}
		if (data != null) {
			Content c = new Content();
			fillMetadata(fso, c);
			byte[] new_data = new byte[length - offset];
			System.arraycopy(data, offset, new_data, 0, length);
			c.setData(new_data);
			try {
				return tika.parse(c);
			} catch (Throwable t) {
				LOG.error(
						CrawlerUtils.msgDocFailed(state.getDataSource().getCollection(), state.getDataSource(), c.getKey(),
								t.toString()), t);

				state.getStatus().incrementCounter(CrawlStatus.Counter.Failed);
				return Collections.emptyList();
			}
		}
		SolrInputDocument doc = new SolrInputDocument();
		fillMetadata(fso, doc);
		doc.addField("body", value.toString());
		return Collections.singletonList(doc);
	}
}
