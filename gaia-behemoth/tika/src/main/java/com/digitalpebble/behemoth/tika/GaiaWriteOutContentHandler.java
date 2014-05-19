package com.digitalpebble.behemoth.tika;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.Charset;

import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class GaiaWriteOutContentHandler extends DefaultHandler {
	private final Writer writer;
	private final int writeLimit;
	private int writeCount = 0;

	private GaiaWriteOutContentHandler(Writer writer, int writeLimit) {
		this.writer = writer;
		this.writeLimit = writeLimit;
	}

	public GaiaWriteOutContentHandler(Writer writer) {
		this(writer, -1);
	}

	public GaiaWriteOutContentHandler(OutputStream stream, Charset charset) {
		this(new OutputStreamWriter(stream, charset));
	}

	public GaiaWriteOutContentHandler(int writeLimit) {
		this(new StringWriter(), writeLimit);
	}

	public GaiaWriteOutContentHandler() {
		this(100000);
	}

	public void characters(char[] ch, int start, int length) throws SAXException {
		try {
			if ((writeLimit == -1) || (writeCount + length <= writeLimit)) {
				writer.write(ch, start, length);
				writeCount += length;
			} else {
				writer.write(ch, start, writeLimit - writeCount);
				writeCount = writeLimit;
				throw new WriteLimitReachedException();
			}
		} catch (IOException e) {
			throw new SAXException("Error writing out character content", e);
		}
	}

	public void ignorableWhitespace(char[] ch, int start, int length) throws SAXException {
		characters(ch, start, length);
	}

	public void endDocument() throws SAXException {
		try {
			writer.flush();
		} catch (IOException e) {
			throw new SAXException("Error flushing character output", e);
		}
	}

	public String toString() {
		return writer.toString();
	}

	public boolean isWriteLimitReached(Throwable t) {
		if ((t instanceof WriteLimitReachedException)) {
			return this == ((WriteLimitReachedException) t).getSource();
		}
		return (t.getCause() != null) && (isWriteLimitReached(t.getCause()));
	}

	@SuppressWarnings("serial")
	private class WriteLimitReachedException extends SAXException {
		private WriteLimitReachedException() {
		}

		public GaiaWriteOutContentHandler getSource() {
			return GaiaWriteOutContentHandler.this;
		}
	}
}
