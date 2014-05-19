package gaia.crawl.fs;

import gaia.crawl.datasource.FieldMapping;
import gaia.crawl.io.Content;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import org.apache.solr.common.SolrInputDocument;
import org.apache.tika.exception.TikaException;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.parser.ParserDecorator;
import org.apache.tika.sax.BodyContentHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.textmining.extraction.TextExtractor;
import org.textmining.extraction.word.Word2TextExtractor;
import org.textmining.extraction.word.WordTextExtractorFactory;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

public class FsParserController {
	private static final Logger LOG = LoggerFactory.getLogger(FsParserController.class);

	public static String getBodyUsingTM(InputStream st) {
		try {
			WordTextExtractorFactory fac = new WordTextExtractorFactory();
			TextExtractor ex = fac.textExtractor(st);
			if ((ex instanceof Word2TextExtractor)) {
				st.reset();
			}
			return ex.getText();
		} catch (Exception e) {
			LOG.error("Error: ", e);
		}

		return null;
	}

	public static SolrInputDocument parse(InputStream stream, Content content, String batchId, FieldMapping fieldMap) {
		try {
			FsDecorator parser = new FsDecorator(new AutoDetectParser());
			ParseContext context = new ParseContext();
			ContentHandler contentHandler = new BodyContentHandler(-1);
			context.set(Parser.class, parser);
			org.apache.tika.metadata.Metadata metadata = new org.apache.tika.metadata.Metadata();

			long len = content.getData() != null ? content.getData().length : 0L;
			metadata.set("Content-Length", Long.toString(len));

			for (Map.Entry<String, String[]> e : content.getMetadata().entrySet()) {
				for (String s : (String[]) e.getValue()) {
					metadata.add((String) e.getKey(), s);
				}
			}

			parser.parse(stream, contentHandler, metadata, context);

			String appName = metadata.get("Application-Name") != null ? metadata.get("Application-Name").trim() : "Other";
			String contentType = metadata.get("Content-Type") != null ? metadata.get("Content-Type").trim() : "None";

			String body = null;

			if (((appName.equals("Microsoft Word for Windows 95")) && (contentType.equals("application/msword")))
					|| ((appName.equals("Microsoft Word for Windows 97")) && (contentType.equals("application/msword")))) {
				body = getBodyUsingTM(stream);

				SolrInputDocument doc = new SolrInputDocument();

				doc.addField("body", body);
				doc.addField("crawl_uri", content.getKey());
				doc.addField("batch_id", batchId);
				doc.addField("parsing", "ok");
				doc.addField("fetching", "ok");

				for (String name : metadata.names()) {
					if (metadata.get(name) != null) {
						doc.addField(name, metadata.get(name));
					}
				}
				Content.fill(doc, fieldMap, content);

				return doc;
			}
		} catch (Throwable t) {
			LOG.debug("Error FsParserController : " + t.getMessage());
		} finally {
			try {
				stream.close();
			} catch (IOException e) {
				LOG.debug("Error FsParserController : " + e.getMessage());
			}
		}

		return null;
	}

	public static class FsDecorator extends ParserDecorator {
		private static final long serialVersionUID = 1L;
		private ContentHandler content;

		public FsDecorator(Parser parser) {
			super(parser);
		}

		public void parse(InputStream stream, ContentHandler content, org.apache.tika.metadata.Metadata metadata,
				ParseContext context) throws IOException, SAXException, TikaException {
			this.content = content;

			super.parse(stream, content, metadata, context);
		}

		public String getBody() {
			return this.content.toString();
		}
	}
}
