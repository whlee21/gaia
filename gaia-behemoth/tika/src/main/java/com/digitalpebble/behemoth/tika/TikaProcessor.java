package com.digitalpebble.behemoth.tika;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.MapWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper.Context;
//import org.apache.hadoop.mapred.Reporter;
import org.apache.tika.config.TikaConfig;
import org.apache.tika.detect.Detector;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.mime.MediaType;
import org.apache.tika.mime.MimeType;
import org.apache.tika.mime.MimeTypeException;
import org.apache.tika.mime.MimeTypes;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.parser.html.HtmlMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.digitalpebble.behemoth.Annotation;
import com.digitalpebble.behemoth.BehemothDocument;
import com.digitalpebble.behemoth.DocumentProcessor;

public class TikaProcessor implements DocumentProcessor, TikaConstants {
	private static final Logger LOG = LoggerFactory.getLogger(TikaProcessor.class);
	private Configuration config;
	private boolean includeMetadata = false;
	private boolean includeAnnotations = false;
	private String mimeType = "text/plain";

	private MimeTypes mimetypes = TikaConfig.getDefaultConfig().getMimeRepository();

	private Detector detector = TikaConfig.getDefaultConfig().getDetector();

	public Configuration getConf() {
		return config;
	}

	public void setConf(Configuration conf) {
		config = conf;
		mimeType = config.get("tika.mime.type");
		includeMetadata = conf.getBoolean("tika.metadata", false);
		includeAnnotations = conf.getBoolean("tika.annotations", false);
	}

	@Override
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

		if ((inputDoc.getContentType() == null) || (inputDoc.getContentType().equals("") == true)) {
			String mt = null;

			if (mimeType == null) {
				if (inputDoc.getContent() != null) {
					Metadata meta = new Metadata();
					meta.set("resourceName", inputDoc.getUrl());
					MimeType mimetype = null;
					try {
						MediaType mediaType = detector.detect(new ByteArrayInputStream(inputDoc.getContent()), meta);
						mimetype = mimetypes.forName(mediaType.getType() + "/" + mediaType.getSubtype());
					} catch (IOException e) {
						LOG.error("Exception", e);
					} catch (MimeTypeException e) {
						LOG.error("Exception", e);
					}
					mt = mimetype.getName();
				} else if (inputDoc.getText() != null) {
					mt = "text/plain";
				}
			} else
				mt = mimeType;

			if (mt != null) {
				inputDoc.setContentType(mt);
			}

		}

		Parser parser = TikaConfig.getDefaultConfig().getParser();

		if (inputDoc.getText() != null) {
			setMetadata(inputDoc, "parsing", "skipped, already processed?");
			if (context != null)
				context.getCounter("TIKA", "TEXT ALREADY AVAILABLE").increment(1L);
			return new BehemothDocument[] { inputDoc };
		}

		InputStream is = new ByteArrayInputStream(inputDoc.getContent());

		Metadata metadata = new Metadata();

		metadata.set("Content-Type", inputDoc.getContentType());

		if (context != null)
			context.getCounter("MIME-TYPE", inputDoc.getContentType()).increment(1L);
		TikaMarkupHandler handler;
		if (includeAnnotations == true)
			handler = new AnnotatingMarkupHandler();
		else {
			handler = new NoAnnotationsMarkupHandler();
		}
		ParseContext parseContext = new ParseContext();

		parseContext.set(HtmlMapper.class, new IdentityHtmlMapper());
		try {
			parser.parse(is, handler, metadata, parseContext);
			processText(inputDoc, handler.getText());
			if (includeMetadata == true) {
				processMetadata(inputDoc, metadata);
			}
			if (includeAnnotations == true)
				processMarkupAnnotations(inputDoc, ((AnnotatingMarkupHandler) handler).getAnnotations());
		} catch (Exception e) {
			LOG.error(inputDoc.getUrl().toString(), e);
			if (context != null)
				context.getCounter("TIKA", "PARSING_ERROR").increment(1L);
			setMetadata(inputDoc, "parsing", "failed: " + e.toString());
			return new BehemothDocument[] { inputDoc };
		} finally {
			try {
				is.close();
			} catch (IOException e) {
			}
		}

		if (context != null) {
			context.getCounter("TIKA", "DOC-PARSED").increment(1L);
		}
		setMetadata(inputDoc, "parsing", "ok");
		return new BehemothDocument[] { inputDoc };
	}

	private void setMetadata(BehemothDocument doc, String name, String value) {
		if (doc.getMetadata() == null) {
			doc.setMetadata(new MapWritable());
		}
		doc.getMetadata().put(new Text(name), new Text(value));
	}

	protected void processText(BehemothDocument inputDoc, String textContent) {
		if (textContent != null)
			inputDoc.setText(textContent);
	}

	protected void processMarkupAnnotations(BehemothDocument inputDoc, List<Annotation> annotations) {
		inputDoc.getAnnotations().addAll(annotations);
	}

	protected void processMetadata(BehemothDocument inputDoc, Metadata metadata) {
		MapWritable mapW = new MapWritable();
		for (String name : metadata.names()) {
			String[] values = metadata.getValues(name);

			StringBuffer buff = new StringBuffer();
			for (int i = 0; i < values.length; i++) {
				if (i > 0)
					buff.append(",");
				buff.append(values[i]);
			}
			mapW.put(new Text(name), new Text(buff.toString()));
		}

		inputDoc.setMetadata(mapW);
	}
}
