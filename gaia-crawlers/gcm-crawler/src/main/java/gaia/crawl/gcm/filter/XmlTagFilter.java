package gaia.crawl.gcm.filter;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.HashSet;
import java.util.Set;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.apache.xerces.jaxp.SAXParserFactoryImpl;
import org.apache.xml.serialize.OutputFormat;
import org.apache.xml.serialize.XMLSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;
import org.xml.sax.helpers.DefaultHandler;

public class XmlTagFilter {
	private static final Logger LOG = LoggerFactory.getLogger(XmlTagFilter.class);

	public static byte[] filterXmlStream(byte[] data, Set<String> inclusions, Set<String> exclusions) throws Exception {
		if ((data == null) || (data.length == 0))
			return data;

		ByteArrayOutputStream byteOutput = new ByteArrayOutputStream();
		OutputFormat format = new OutputFormat("XML", "UTF-8", false);
		XMLSerializer serializer = new XMLSerializer(byteOutput, format);

		SAXParserFactory factoryInstance = SAXParserFactoryImpl.newInstance();
		SAXParser parser = factoryInstance.newSAXParser();

		InputSource inputSource = new InputSource(new ByteArrayInputStream(data));
		inputSource.setEncoding("UTF-8");
		parser.parse(inputSource, new XmlFilterHandler(serializer.asContentHandler(), inclusions, exclusions));

		return byteOutput.toByteArray();
	}

	private static class XmlFilterHandler extends DefaultHandler {
		ContentHandler outputHandler;
		AttributesImpl atts = new AttributesImpl();
		Set<String> inclusions;
		Set<String> exclusions;
		String currentTagRemoved = null;
		String parentTagIncluded = null;

		boolean rootTag = true;

		XmlFilterHandler(ContentHandler outputHandler, Set<String> inclusions, Set<String> exclusions) {
			this.outputHandler = outputHandler;
			this.inclusions = inclusions;
			if (this.inclusions == null)
				this.inclusions = new HashSet();

			this.exclusions = exclusions;
			if (this.exclusions == null)
				this.exclusions = new HashSet();

			XmlTagFilter.LOG.info("Inclusions to be applied: " + this.inclusions);
			XmlTagFilter.LOG.info("Exclusions to be applied: " + this.exclusions);
		}

		public void startDocument() throws SAXException {
			this.outputHandler.startDocument();
		}

		public void endDocument() throws SAXException {
			this.outputHandler.endDocument();
		}

		public void endPrefixMapping(String prefix) throws SAXException {
			this.outputHandler.endPrefixMapping(prefix);
		}

		public void startPrefixMapping(String prefix, String uri) throws SAXException {
			this.outputHandler.startPrefixMapping(prefix, uri);
		}

		public void processingInstruction(String target, String data) throws SAXException {
			this.outputHandler.processingInstruction(target, data);
		}

		public void setDocumentLocator(Locator locator) {
		}

		public void characters(char[] ch, int start, int length) throws SAXException {
			if (this.currentTagRemoved == null)
				this.outputHandler.characters(ch, start, length);
		}

		public void startElement(String uri, String localname, String qname, Attributes atts) throws SAXException {
			if (this.rootTag) {
				this.rootTag = false;
				this.outputHandler.startElement(uri, localname, qname, atts);
				return;
			}

			if (this.currentTagRemoved != null)
				return;
			if ((this.exclusions.contains(qname))
					|| ((this.parentTagIncluded == null) && (!this.inclusions.isEmpty()) && (!this.inclusions.contains(qname)))) {
				if (this.currentTagRemoved == null)
					this.currentTagRemoved = qname;
				return;
			}

			if (this.parentTagIncluded == null)
				this.parentTagIncluded = qname;

			this.outputHandler.startElement(uri, localname, qname, atts);
		}

		public void endElement(String uri, String localname, String qname) throws SAXException {
			if (this.currentTagRemoved == null) {
				this.outputHandler.endElement(uri, localname, qname);
			}
			if (qname.equals(this.currentTagRemoved)) {
				this.currentTagRemoved = null;
			}
			if (qname.equals(this.parentTagIncluded))
				this.parentTagIncluded = null;
		}

		public void skippedEntity(String name) {
		}

		public void ignorableWhitespace(char[] ch, int start, int length) {
		}
	}
}
