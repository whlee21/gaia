package com.digitalpebble.behemoth.tika;

import java.io.OutputStream;
import java.io.Writer;
import java.nio.charset.Charset;
import org.apache.tika.sax.ContentHandlerDecorator;
import org.apache.tika.sax.xpath.Matcher;
import org.apache.tika.sax.xpath.MatchingContentHandler;
import org.apache.tika.sax.xpath.XPathParser;
import org.xml.sax.ContentHandler;

public class GaiaBodyContentHandler extends ContentHandlerDecorator {
	private static final XPathParser PARSER = new XPathParser("xhtml", "http://www.w3.org/1999/xhtml");

	private static final Matcher MATCHER = PARSER.parse("/xhtml:html/xhtml:body/descendant:node()");

	public GaiaBodyContentHandler(ContentHandler handler) {
		super(new MatchingContentHandler(handler, MATCHER));
	}

	public GaiaBodyContentHandler(Writer writer) {
		this(new GaiaWriteOutContentHandler(writer));
	}

	public GaiaBodyContentHandler(OutputStream stream, String encoding) {
		this(new GaiaWriteOutContentHandler(stream, Charset.forName(encoding)));
	}

	public GaiaBodyContentHandler(OutputStream stream, Charset charset) {
		this(new GaiaWriteOutContentHandler(stream, charset));
	}

	public GaiaBodyContentHandler(int writeLimit) {
		this(new GaiaWriteOutContentHandler(writeLimit));
	}

	public GaiaBodyContentHandler() {
		this(new GaiaWriteOutContentHandler());
	}
}
