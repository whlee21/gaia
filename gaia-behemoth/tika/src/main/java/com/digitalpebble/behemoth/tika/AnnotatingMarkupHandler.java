package com.digitalpebble.behemoth.tika;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

import com.digitalpebble.behemoth.Annotation;

public class AnnotatingMarkupHandler extends TikaMarkupHandler implements ContentHandler {
	private List<Annotation> annotationBuffer;
	private LinkedList<Annotation> startedAnnotations;

	public AnnotatingMarkupHandler() {
		textBuffer = new StringBuilder();
		annotationBuffer = new LinkedList<Annotation>();
		startedAnnotations = new LinkedList<Annotation>();
	}

	public void startDocument() throws SAXException {
		textBuffer.setLength(0);
		annotationBuffer.clear();
		startedAnnotations.clear();
	}

	public void endDocument() throws SAXException {
		if (startedAnnotations.size() != 0)
			;
	}

	public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
		int startOffset = textBuffer.length();

		Annotation annot = new Annotation();
		annot.setStart(startOffset);

		annot.setType(localName);

		for (int i = 0; i < atts.getLength(); i++) {
			String key = atts.getLocalName(i);
			String value = atts.getValue(i);
			annot.getFeatures().put(key, value);
		}
		startedAnnotations.addLast(annot);
	}

	public void endElement(String uri, String localName, String qName) throws SAXException {
		int endOffset = textBuffer.length();

		if ((localName.equals("head")) && (endOffset > 0)) {
			textBuffer.append("\n");
		}

		Iterator iter = startedAnnotations.iterator();
		Annotation startedAnnot = null;
		while (iter.hasNext()) {
			Annotation temp = (Annotation) iter.next();
			if (temp.getType().equals(localName)) {
				startedAnnot = temp;
				break;
			}
		}

		if (startedAnnot == null) {
			return;
		}

		startedAnnot.setEnd(endOffset);
		startedAnnotations.remove(startedAnnot);
		annotationBuffer.add(startedAnnot);
	}

	public List<Annotation> getAnnotations() {
		return annotationBuffer;
	}
}
