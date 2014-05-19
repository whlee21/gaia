 package com.digitalpebble.behemoth.tika;
 
 import com.digitalpebble.behemoth.Annotation;
 import java.util.Iterator;
 import java.util.LinkedList;
 import java.util.List;
 import java.util.Map;
 import org.xml.sax.Attributes;
 import org.xml.sax.ContentHandler;
 import org.xml.sax.SAXException;
 
 public class AnnotatingMarkupHandler extends TikaMarkupHandler
   implements ContentHandler
 {
   private List<Annotation> annotationBuffer;
   private LinkedList<Annotation> startedAnnotations;
 
   public AnnotatingMarkupHandler()
   {
     this.textBuffer = new StringBuilder();
     this.annotationBuffer = new LinkedList();
     this.startedAnnotations = new LinkedList();
   }
 
   public void startDocument() throws SAXException {
     this.textBuffer.setLength(0);
     this.annotationBuffer.clear();
     this.startedAnnotations.clear();
   }
 
   public void endDocument() throws SAXException
   {
     if (this.startedAnnotations.size() != 0);
   }
 
   public void startElement(String uri, String localName, String qName, Attributes atts)
     throws SAXException
   {
     int startOffset = this.textBuffer.length();
 
     Annotation annot = new Annotation();
     annot.setStart(startOffset);
 
     annot.setType(localName);
 
     for (int i = 0; i < atts.getLength(); i++) {
       String key = atts.getLocalName(i);
       String value = atts.getValue(i);
       annot.getFeatures().put(key, value);
     }
     this.startedAnnotations.addLast(annot);
   }
 
   public void endElement(String uri, String localName, String qName)
     throws SAXException
   {
     int endOffset = this.textBuffer.length();
 
     if ((localName.equals("head")) && (endOffset > 0)) {
       this.textBuffer.append("\n");
     }
 
     Iterator iter = this.startedAnnotations.iterator();
     Annotation startedAnnot = null;
     while (iter.hasNext()) {
       Annotation temp = (Annotation)iter.next();
       if (temp.getType().equals(localName)) {
         startedAnnot = temp;
         break;
       }
     }
 
     if (startedAnnot == null)
     {
       return;
     }
 
     startedAnnot.setEnd(endOffset);
     this.startedAnnotations.remove(startedAnnot);
     this.annotationBuffer.add(startedAnnot);
   }
 
   public List<Annotation> getAnnotations()
   {
     return this.annotationBuffer;
   }
 }

