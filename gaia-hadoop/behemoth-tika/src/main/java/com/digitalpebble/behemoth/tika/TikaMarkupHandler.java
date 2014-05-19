 package com.digitalpebble.behemoth.tika;
 
 import org.xml.sax.Attributes;
 import org.xml.sax.ContentHandler;
 import org.xml.sax.Locator;
 import org.xml.sax.SAXException;
 
 public abstract class TikaMarkupHandler
   implements ContentHandler
 {
   protected StringBuilder textBuffer;
 
   public TikaMarkupHandler()
   {
     this.textBuffer = new StringBuilder();
   }
 
   public void startElement(String s, String s1, String s2, Attributes attributes)
     throws SAXException
   {
   }
 
   public void endElement(String uri, String localName, String qName)
     throws SAXException
   {
     int endOffset = this.textBuffer.length();
 
     if ((localName.equals("head")) && (endOffset > 0))
       this.textBuffer.append("\n");
   }
 
   public String getText() {
     return this.textBuffer.toString();
   }
 
   public void startPrefixMapping(String prefix, String uri) throws SAXException
   {
   }
 
   public void endPrefixMapping(String prefix) throws SAXException
   {
   }
 
   public void setDocumentLocator(Locator locator)
   {
   }
 
   public void skippedEntity(String name) throws SAXException {
   }
 
   public void processingInstruction(String target, String data) throws SAXException {
   }
 
   public void ignorableWhitespace(char[] ch, int start, int length) throws SAXException {
     characters(ch, start, length);
   }
 
   public void characters(char[] ch, int start, int length) throws SAXException
   {
     this.textBuffer.append(ch, start, length);
   }
 }

