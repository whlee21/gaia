 package com.digitalpebble.behemoth.tika;
 
 import org.xml.sax.ContentHandler;
 import org.xml.sax.SAXException;
 
 public class NoAnnotationsMarkupHandler extends TikaMarkupHandler
   implements ContentHandler
 {
   public void startDocument()
     throws SAXException
   {
     this.textBuffer.setLength(0);
   }
 
   public void endDocument()
     throws SAXException
   {
   }
 }

