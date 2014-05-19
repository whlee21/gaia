package gaia.crawl.impl;

import gaia.Defaults;
import gaia.crawl.CrawlState;
import gaia.crawl.CrawlStatus;
import gaia.crawl.ParserController;
import gaia.crawl.UpdateController;
import gaia.crawl.datasource.DataSourceAPI;
import gaia.crawl.datasource.FieldMapping;
import gaia.crawl.io.Content;
import gaia.crawl.parser.GaiaBodyContentHandler;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.UUID;

import org.apache.commons.io.input.TeeInputStream;
import org.apache.solr.common.SolrInputDocument;
import org.apache.tika.config.TikaConfig;
import org.apache.tika.detect.Detector;
import org.apache.tika.exception.TikaException;
import org.apache.tika.mime.MediaType;
import org.apache.tika.mime.MimeType;
import org.apache.tika.mime.MimeTypes;
import org.apache.tika.parser.DefaultParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.parser.ParserDecorator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class TikaParserController extends ParserController {
	public static transient Logger LOG = LoggerFactory.getLogger(TikaParserController.class);
	public static final String defaultInputEncoding = "UTF-8";
	public static final String defaultOutputEncoding = "UTF-8";
	public static final String BODY_FIELD = "body";
	public static final String CONTAINER_FIELD = "belongsToContainer";
	public static final String RESOURCE_FIELD = "resource_name";
	public static final String CRAWL_URI_FIELD = "crawl_uri";
	public static final String BATCH_ID_FIELD = "batch_id";
	public static final String INCLUDE_IMAGES_KEY = "tika.parsers.include.images";
	public static final String FLATTEN_COMPOUND_KEY = "tika.parsers.flatten.compound";
	protected String defaultFieldName = null;
	protected String batchId = null;
	protected ContentHandler dummy = new DefaultHandler();
	protected boolean includeImages = false;
	protected boolean flattenCompound = false;
	protected boolean addFailedDocs = false;
	protected boolean addOriginalContent = false;
	protected Parser defaultParser = TikaConfig.getDefaultConfig().getParser();
	protected MimeTypes mimetypes = TikaConfig.getDefaultConfig().getMimeRepository();
	protected Detector detector = TikaConfig.getDefaultConfig().getDetector();

	public TikaParserController(UpdateController updateController) {
		super(updateController);
		if ((defaultParser instanceof DefaultParser)) {
			((DefaultParser) defaultParser).setFallback(ErrorParser.INSTANCE);
		}

		includeImages = Defaults.INSTANCE.getBoolean(Defaults.Group.datasource, INCLUDE_IMAGES_KEY, Boolean.valueOf(false));
		flattenCompound = Defaults.INSTANCE.getBoolean(Defaults.Group.datasource, FLATTEN_COMPOUND_KEY,
				Boolean.valueOf(true));
		addFailedDocs = Defaults.INSTANCE.getBoolean(Defaults.Group.datasource, "add_failed_docs", Boolean.valueOf(false));
	}

	public void init(CrawlState state) {
		super.init(state);
		batchId = UUID.randomUUID().toString();
		if (state.getDataSource() != null) {
			addFailedDocs = state.getDataSource().getBoolean("add_failed_docs", addFailedDocs);
			FieldMapping mapping = state.getDataSource().getFieldMapping();
			if (mapping != null)
				addOriginalContent = mapping.isAddOriginalContent();
		}
	}

	public List<SolrInputDocument> parse(Content content) throws Exception {
		List<SolrInputDocument> docs = new ArrayList<SolrInputDocument>();

		if (content == null) {
			return docs;
		}
		org.apache.tika.metadata.Metadata metadata = new org.apache.tika.metadata.Metadata();

		long len = content.getData() != null ? content.getData().length : 0L;
		metadata.set("Content-Length", Long.toString(len));

		for (Map.Entry<String, String[]> e : content.getMetadata().entrySet()) {
			for (String s : (String[]) e.getValue()) {
				metadata.add((String) e.getKey(), s);
			}
		}
		ParseContext context = new ParseContext();
		String docUrl = content.getKey();
		String uniqueKey = fieldMapping != null ? fieldMapping.getUniqueKey() : "id";
		CollectingParser parser = new CollectingParser(defaultParser, detector, mimetypes, state.getStatus(), docs,
				metadata, batchId, uniqueKey, docUrl, includeImages, flattenCompound, addOriginalContent, addFailedDocs);

		context.set(Parser.class, parser);

		if (content.getData() != null) {
			InputStream input = new ByteArrayInputStream(content.getData());
			try {
				parser.parse(input, dummy, metadata, context);
			} catch (Exception e) {
				LOG.debug("Error tika:", e);
			} finally {
				input.close();
			}
		} else {
			state.getStatus().incrementCounter(CrawlStatus.Counter.Failed);
			if (addFailedDocs) {
				SolrInputDocument doc = new SolrInputDocument();
				doc.addField(uniqueKey, docUrl);
				for (String m : metadata.names()) {
					String[] vals = metadata.getValues(m);
					for (String v : vals) {
						doc.addField(m, v);
					}
				}
				doc.addField("parsing", "no_data");
				doc.addField("batch_id", getBatchId());
				docs.add(doc);
			} else {
				LOG.warn("Parsing " + docUrl + " failed: no data");
			}
		}
		return docs;
	}

	public boolean isIncludeImages() {
		return includeImages;
	}

	public void setIncludeImages(boolean includeImages) {
		includeImages = includeImages;
	}

	public boolean isFlattenCompound() {
		return flattenCompound;
	}

	public void setFlattenCompound(boolean flattenCompound) {
		flattenCompound = flattenCompound;
	}

	public boolean isAddFailedDocs() {
		return addFailedDocs;
	}

	public void setAddFailedDocs(boolean addFailedDocs) {
		addFailedDocs = addFailedDocs;
	}

	public String getBatchId() {
		return batchId;
	}

	private static class CollectingParser extends ParserDecorator {
		private static final long serialVersionUID = 1508171654130339149L;
		private static final int maxStackDepth = 20;
		private static final boolean alwaysDetectMimeType = true; // FIXME: by
																															// whlee21
		List<SolrInputDocument> docs;
		Stack<String> nested = new Stack<String>();
		org.apache.tika.metadata.Metadata parentMeta;
		MimeTypes mimeTypes;
		Detector detector;
		String uniqueKey;
		String parentUrl;
		String batchId;
		int count = 0;
		boolean images;
		boolean flatten;
		boolean addFailedDocs;
		boolean original;
		ParseContext plainContext = new ParseContext();
		CrawlStatus status;

		public CollectingParser(Parser parser, Detector detector, MimeTypes mimeTypes, CrawlStatus status,
				List<SolrInputDocument> docs, org.apache.tika.metadata.Metadata parentMeta, String batchId, String uniqueKey,
				String parentUrl, boolean images, boolean flatten, boolean original, boolean addFailedDocs) {
			super(parser);
			this.mimeTypes = mimeTypes;
			this.detector = detector;
			this.status = status;
			this.docs = docs;
			this.uniqueKey = uniqueKey;
			this.parentUrl = parentUrl;
			this.batchId = batchId;
			this.parentMeta = parentMeta;
			this.images = images;
			this.flatten = flatten;
			this.original = original;
			this.addFailedDocs = addFailedDocs;
			plainContext.set(Parser.class, parser);
		}

		public void parse(InputStream stream, ContentHandler dummy, org.apache.tika.metadata.Metadata metadata,
				ParseContext context) throws IOException, SAXException, TikaException {
			String resourceName = metadata.get("resourceName");

			StringBuilder thisUrl = new StringBuilder();
			thisUrl.append(parentUrl);
			StringBuilder localName = new StringBuilder();
			StringBuilder containerName = null;
			if (!nested.isEmpty()) {
				if (resourceName == null) {
					resourceName = new StringBuilder().append("item").append(count++).toString();
				}
				for (int i = 1; i < nested.size(); i++) {
					if (i > 1) {
						localName.append('!');
					}
					localName.append((String) nested.get(i));
				}
				containerName = new StringBuilder(parentUrl);
				if (localName.length() > 0) {
					containerName.append('!');
					containerName.append(localName);
					localName.append('!');
				}
				localName.append(resourceName);
				thisUrl.append('!');
				thisUrl.append(localName);
			} else {
				localName.append(resourceName);
			}
			if (nested.size() > maxStackDepth) {
				LOG.warn(new StringBuilder().append("Avoiding parsing loop in ").append(thisUrl)
						.append(", nesting level: ").append(nested.size()).toString());
				return;
			}
			String contentType = metadata.get("Content-Type");
			if ((contentType != null) && (contentType.startsWith("image")) && (!images)) {
				return;
			}
			nested.push(resourceName);
			try {
				ByteArrayOutputStream out = new ByteArrayOutputStream();
				org.apache.tika.metadata.Metadata m = new org.apache.tika.metadata.Metadata();

				for (String name : metadata.names()) {
					String[] vals = metadata.getValues(name);

					for (String v : vals) {
						m.add(name, v);
					}

				}

				Parser p = getWrappedParser();
				boolean archive = false;

				MediaType type = detector.detect(stream, m);

				if (type != null) {
					contentType = new StringBuilder().append(type.getType()).append("/").append(type.getSubtype()).toString();
					MimeType mimetype = mimeTypes.forName(contentType);
					if (mimetype != null) {
						contentType = mimetype.getName();
					}
					m.set("Content-Type", contentType);
					if ((p instanceof DefaultParser)) {
						Parser subParser = (Parser) ((DefaultParser) p).getParsers(context).get(type);
						if (subParser != null) {
							if ((subParser instanceof DefaultParser)) {
								Parser subSub = (Parser) ((DefaultParser) subParser).getParsers().get(type);

								if ((subSub != null)
										&& (subSub.getClass().getName().equals("org.apache.tika.parser.pkg.PackageParser")))
									archive = true;
							} else if (subParser.getClass().getName().equals("org.apache.tika.parser.pkg.PackageParser")) {
								archive = true;
							}
						}
					}
				}

				if (contentType == null) {
					contentType = "application/octet-stream";
				}

				if ((contentType != null) && (contentType.startsWith("image")) && (!images)) {
					return;
				}

				if (contentType.equals("application/java-archive")) {
					archive = true;
				}
				ContentHandler handler = new GaiaBodyContentHandler(out, "UTF-8");

				ByteArrayOutputStream cache = null;
				if (original) {
					cache = new ByteArrayOutputStream();
					TeeInputStream tee = new TeeInputStream(stream, cache);
					stream = tee;
				}
				if (archive) {
					super.parse(stream, handler, m, context);
				} else if (flatten)
					p.parse(stream, handler, m, plainContext);
				else {
					super.parse(stream, handler, m, context);
				}

				SolrInputDocument doc = new SolrInputDocument();
				String mime = null;
				String[] mimes = m.getValues("Content-Type");
				m.remove("Content-Type");
				if ((mimes != null) && (mimes.length > 0)) {
					for (String mn : mimes) {
						if (!mn.equals(contentType)) {
							mime = mn;
							break;
						}
					}
				}
				if (mime == null) {
					mime = contentType;
				}
				if ((mime.startsWith("image")) && (!images)) {
					return;
				}
				m.set("Content-Type", mime);
				byte[] charData = out.toByteArray();

				String body = new String(charData, "UTF-8");
				LOG.trace(new StringBuilder().append("---\nExtracted content body length: ")
						.append(body.length()).append(" extracted content body: <<").append(body).append(">>").toString());

				doc.addField("body", body);

				if (containerName != null) {
					doc.addField("belongsToContainer", containerName.toString());
				}
				if (resourceName != null) {
					doc.addField("resource_name", resourceName);
				}

				if (original) {
					doc.addField("original_content", cache.toByteArray());
				}

				doc.addField("crawl_uri", thisUrl.toString());

				doc.addField("batch_id", batchId);

				for (String name : m.names()) {
					String[] fieldValues = m.getValues(name);
					for (String value : fieldValues) {
						doc.addField(name, value);
					}
				}

				for (String name : parentMeta.names()) {
					if (!doc.containsKey(name)) {
						for (String v : parentMeta.getValues(name))
							doc.addField(name, v);
					}
				}
				doc.addField("parsing", "ok");

				if (doc.getField(uniqueKey) == null) {
					doc.setField(uniqueKey, thisUrl.toString());
				} else {
					String key = String.valueOf(doc.getFieldValue(uniqueKey));

					doc.setField(uniqueKey, key);
				}
				docs.add(doc);
			} catch (Exception e) {
				status.incrementCounter(CrawlStatus.Counter.Failed);
				LOG.debug("Error: ", e);

				if (addFailedDocs) {
					SolrInputDocument doc = new SolrInputDocument();
					for (String m : metadata.names()) {
						String[] vals = metadata.getValues(m);
						for (String v : vals) {
							doc.addField(m, v);
						}
					}
					doc.setField(uniqueKey, thisUrl.toString());
					doc.addField("parsing",
							new StringBuilder().append("failed: (invalid/unrecognized format?) ").append(e.getMessage()).toString());
					docs.add(doc);
				} else {
					LOG.warn(new StringBuilder().append("Parsing ").append(thisUrl).append(" failed: ")
							.append(e.getMessage()).toString());
					return;
				}
			} finally {
				nested.pop();
			}
		}
	}

	private static class ErrorParser implements Parser {
		static final ErrorParser INSTANCE = new ErrorParser();

		public Set<MediaType> getSupportedTypes(ParseContext context) {
			return Collections.singleton(new MediaType("application", "octet-stream"));
		}

		public void parse(InputStream stream, ContentHandler handler, org.apache.tika.metadata.Metadata metadata,
				ParseContext context) throws IOException, SAXException, TikaException {
			throw new TikaException("unsupported mimeType " + metadata.get("Content-Type"));
		}
	}
}
