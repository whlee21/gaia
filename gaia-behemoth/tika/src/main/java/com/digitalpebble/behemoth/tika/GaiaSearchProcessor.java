package com.digitalpebble.behemoth.tika;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import org.apache.commons.io.input.TeeInputStream;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.MapWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.Writable;
import org.apache.hadoop.mapreduce.Mapper.Context;
import org.apache.tika.config.TikaConfig;
import org.apache.tika.detect.Detector;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
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

import com.digitalpebble.behemoth.BehemothDocument;
import com.digitalpebble.behemoth.DocumentProcessor;

public class GaiaSearchProcessor implements DocumentProcessor, TikaConstants {
	private static final Logger LOG = LoggerFactory.getLogger(GaiaSearchProcessor.class);
	private Configuration config;
	private boolean includeMetadata;
	private String mimeType;
	public static final String defaultInputEncoding = "UTF-8";
	public static final String defaultOutputEncoding = "UTF-8";
	public static final String BODY_FIELD = "body";
	public static final String CONTAINER_FIELD = "belongsToContainer";
	public static final String RESOURCE_FIELD = "resource_name";
	public static final String CRAWL_URI_FIELD = "crawl_uri";
	public static final String BATCH_ID_FIELD = "batch_id";
	protected String defaultFieldName;
	protected String batchId;
	protected ContentHandler dummy;
	protected boolean includeImages;
	protected boolean flattenCompound;
	protected boolean addFailedDocs;
	protected boolean addOriginalContent;
	protected Parser defaultParser;
	protected MimeTypes mimetypes;
	protected Detector detector;

	public GaiaSearchProcessor() {
		includeMetadata = false;
		mimeType = "text/plain";

		defaultFieldName = null;
		batchId = null;
		dummy = new DefaultHandler();
		includeImages = false;
		flattenCompound = false;
		addFailedDocs = true;
		addOriginalContent = false;
		defaultParser = TikaConfig.getDefaultConfig().getParser();
		mimetypes = TikaConfig.getDefaultConfig().getMimeRepository();
		detector = TikaConfig.getDefaultConfig().getDetector();
	}

	public Configuration getConf() {
		return config;
	}

	public void setConf(Configuration conf) {
		config = conf;
		mimeType = config.get("tika.mime.type");
		batchId = conf.get("gaia.batch.id");
		includeMetadata = conf.getBoolean("tika.metadata", true);
		flattenCompound = conf.getBoolean("tika.flatten", false);
		addFailedDocs = conf.getBoolean("tika.add.failed", true);
		includeImages = conf.getBoolean("tika.images", false);
		addOriginalContent = conf.getBoolean("tika.add.original", false);
		if ((defaultParser instanceof DefaultParser))
			((DefaultParser) defaultParser).setFallback(ErrorParser.INSTANCE);
	}

	public void cleanup() {
	}

	public BehemothDocument[] process(BehemothDocument inputDoc, Context context) {
		if (((inputDoc.getContent() == null) || (inputDoc.getContent().length == 0))
				&& ((inputDoc.getText() == null) || (inputDoc.getText().length() == 0))) {
			LOG.info("No content or text for " + inputDoc.getUrl() + " skipping");

			setMetadata(inputDoc, "parsing", "skipped, no content");
			if (context != null)
				context.getCounter("TIKA", "DOC-NO_DATA").increment(1L);
			return new BehemothDocument[] { inputDoc };
		}
		if (inputDoc.getText() != null) {
			setMetadata(inputDoc, "parsing", "skipped, already parsed?");
			if (context != null)
				context.getCounter("TIKA", "TEXT ALREADY AVAILABLE").increment(1L);
			return new BehemothDocument[] { inputDoc };
		}

		Metadata metadata = new Metadata();

		long len = inputDoc.getContent() != null ? inputDoc.getContent().length : 0L;
		metadata.set("Content-Length", Long.toString(len));

		MapWritable docMeta = inputDoc.getMetadata();
		if (docMeta != null) {
			for (Map.Entry<Writable, Writable> e : docMeta.entrySet()) {
				if (e.getValue() != null) {
					metadata.add(((Writable) e.getKey()).toString(), ((Writable) e.getValue()).toString());
				}
			}
		}
		ParseContext parseContext = new ParseContext();

		metadata.set("Content-Type", inputDoc.getContentType());

		if ((context != null) && (inputDoc.getContentType() != null)) {
			context.getCounter("MIME-TYPE", inputDoc.getContentType()).increment(1L);
		}
		String docUrl = inputDoc.getUrl();
		String uniqueKey = "id";
		List<BehemothDocument> docs = new LinkedList<BehemothDocument>();
		CollectingParser parser = new CollectingParser(defaultParser, detector, mimetypes, context, docs, metadata,
				batchId, uniqueKey, docUrl, includeImages, flattenCompound, addOriginalContent, addFailedDocs);

		parseContext.set(Parser.class, parser);

		if (inputDoc.getContent() != null) {
			InputStream input = new ByteArrayInputStream(inputDoc.getContent());
			try {
				parser.parse(input, dummy, metadata, parseContext);
			} catch (Throwable t) {
				LOG.info("Parsing failed for " + inputDoc.getUrl() + ", skipping: " + t.toString());
				if (context != null)
					context.getCounter("TIKA", "PARSING_ERROR").increment(1L);
				if (addFailedDocs) {
					StringWriter sw = new StringWriter();
					PrintWriter pw = new PrintWriter(sw);
					t.printStackTrace(pw);
					pw.flush();
					setMetadata(inputDoc, "parsing", "failed: " + sw.toString());
					docs.add(inputDoc);
				}
			} finally {
				try {
					if (input != null)
						input.close();
				} catch (IOException ioe) {
				}
			}
		} else {
			if (addFailedDocs) {
				setMetadata(inputDoc, "parsing", "skipped, no content");
				docs.add(inputDoc);
			}
			if (context != null) {
				context.getCounter("TIKA", "DOC-NO_DATA").increment(1L);
			}
		}
		return (BehemothDocument[]) docs.toArray(new BehemothDocument[0]);
	}

	private static void setMetadata(BehemothDocument doc, String name, String value) {
		if (doc.getMetadata() == null) {
			doc.setMetadata(new MapWritable());
		}
		if (value == null) {
			value = "";
		}
		doc.getMetadata().put(new Text(name), new Text(value));
	}

	private static void addMetadata(BehemothDocument doc, String name, String value) {
		if (doc.getMetadata() == null) {
			doc.setMetadata(new MapWritable());
		}
		Text key = new Text(name);
		Text oldValue = (Text) doc.getMetadata().get(key);
		if (oldValue != null) {
			if (value == null) {
				return;
			}
			Text newValue = new Text(oldValue.toString() + "," + value);
			doc.getMetadata().put(key, newValue);
		} else {
			if (value == null) {
				value = "";
			}
			doc.getMetadata().put(key, new Text(value));
		}
	}

	private static class CollectingParser extends ParserDecorator {
		private static final long serialVersionUID = 1508171654130339149L;
		private static final int maxStackDepth = 20;
		List<BehemothDocument> docs;
		Stack<String> nested = new Stack<String>();
		Metadata parentMeta;
		MimeTypes mimeTypes;
		Context context;
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
		private static final byte[] EMPTY_CONTENT = new byte[0];

		public CollectingParser(Parser parser, Detector detector, MimeTypes mimeTypes, Context context,
				List<BehemothDocument> docs, Metadata parentMeta, String batchId, String uniqueKey, String parentUrl,
				boolean images, boolean flatten, boolean original, boolean addFailedDocs) {
			super(parser);
			this.mimeTypes = mimeTypes;
			this.detector = detector;
			this.context = context;
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

		public void parse(InputStream stream, ContentHandler dummy, Metadata metadata, ParseContext parseContext)
				throws IOException, SAXException, TikaException {
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
						localName.append(':');
					}
					localName.append((String) nested.get(i));
				}
				containerName = new StringBuilder(parentUrl);
				if (localName.length() > 0) {
					containerName.append(':');
					containerName.append(localName);
					localName.append(':');
				}
				localName.append(resourceName);
				thisUrl.append(':');
				thisUrl.append(localName);
			} else {
				localName.append(resourceName);
			}
			if (nested.size() > 20) {
				GaiaSearchProcessor.LOG.warn(new StringBuilder().append("Avoiding parsing loop in ").append(thisUrl)
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
				Metadata m = new Metadata();

				for (String name : metadata.names()) {
					String[] vals = metadata.getValues(name);

					for (String v : vals) {
						m.add(name, v);
					}

				}

				Parser p = getWrappedParser();
				boolean archive = false;
				if ((contentType == null) || (contentType.trim().length() == 0)) {
					MediaType type = detector.detect(stream, m);

					if (type != null) {
						contentType = new StringBuilder().append(type.getType()).append("/").append(type.getSubtype()).toString();
						MimeType mimetype = mimeTypes.forName(contentType);
						if (mimetype != null) {
							contentType = mimetype.getName();
						}
						m.set("Content-Type", contentType);
						if ((p instanceof DefaultParser)) {
							Parser subParser = (Parser) ((DefaultParser) p).getParsers(parseContext).get(type);
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
					super.parse(stream, handler, m, parseContext);
				} else if (flatten)
					p.parse(stream, handler, m, plainContext);
				else {
					super.parse(stream, handler, m, parseContext);
				}

				BehemothDocument doc = new BehemothDocument();
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
				GaiaSearchProcessor.LOG.trace(new StringBuilder().append("---\nExtracted content body length: ")
						.append(body.length()).append(" extracted content body: <<").append(body).append(">>").toString());

				doc.setText(body);
				if (containerName != null) {
					GaiaSearchProcessor.addMetadata(doc, "belongsToContainer", containerName.toString());
				}
				if (resourceName != null) {
					GaiaSearchProcessor.addMetadata(doc, "resource_name", resourceName);
				}

				if (original)
					doc.setContent(cache.toByteArray());
				else {
					doc.setContent(EMPTY_CONTENT);
				}

				doc.setUrl(thisUrl.toString());

				if ((doc.getMetadata() == null) || (!doc.getMetadata().containsKey(new Text(uniqueKey)))) {
					GaiaSearchProcessor.setMetadata(doc, uniqueKey, thisUrl.toString());
				}

				GaiaSearchProcessor.addMetadata(doc, "crawl_uri", thisUrl.toString());

				GaiaSearchProcessor.addMetadata(doc, "batch_id", batchId);

				for (String name : m.names()) {
					String[] fieldValues = m.getValues(name);
					for (String value : fieldValues) {
						GaiaSearchProcessor.addMetadata(doc, name, value);
					}
				}

				for (String name : parentMeta.names()) {
					if ((doc.getMetadata() == null) || (!doc.getMetadata().containsKey(new Text(name)))) {
						for (String v : parentMeta.getValues(name))
							GaiaSearchProcessor.addMetadata(doc, name, v);
					}
				}
				GaiaSearchProcessor.setMetadata(doc, "parsing", "ok");
				if (context != null)
					context.getCounter("TIKA", "DOC-PARSED").increment(1L);
				docs.add(doc);
			} catch (Throwable e) {
				if (addFailedDocs) {
					BehemothDocument doc = new BehemothDocument();
					doc.setUrl(thisUrl.toString());
					doc.setContent(EMPTY_CONTENT);
					GaiaSearchProcessor.setMetadata(doc, uniqueKey, thisUrl.toString());
					for (String m : metadata.names()) {
						String[] vals = metadata.getValues(m);
						for (String v : vals) {
							GaiaSearchProcessor.addMetadata(doc, m, v);
						}
					}
					StringWriter sw = new StringWriter();
					PrintWriter pw = new PrintWriter(sw);
					e.printStackTrace(pw);
					pw.flush();
					GaiaSearchProcessor.setMetadata(doc, "parsing",
							new StringBuilder().append("failed: (invalid/unsupported format?) ").append(sw.toString()).toString());
					docs.add(doc);
				} else {
					GaiaSearchProcessor.LOG.warn(new StringBuilder().append("Parsing ").append(thisUrl).append(" failed: ")
							.append(e.getMessage()).toString());
					return;
				}
				if (context != null)
					context.getCounter("TIKA", "PARSING_ERROR").increment(1L);
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

		public void parse(InputStream stream, ContentHandler handler, Metadata metadata, ParseContext context)
				throws IOException, SAXException, TikaException {
			throw new TikaException("unsupported mimeType " + metadata.get("Content-Type"));
		}
	}
}
