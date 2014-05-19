 package com.digitalpebble.behemoth.tika;
 
 import java.io.OutputStream;
 import java.io.Writer;
 import java.nio.charset.Charset;
 import org.apache.tika.sax.ContentHandlerDecorator;
 import org.apache.tika.sax.xpath.Matcher;
 import org.apache.tika.sax.xpath.MatchingContentHandler;
 import org.apache.tika.sax.xpath.XPathParser;
 import org.xml.sax.ContentHandler;
 
 public class LucidBodyContentHandler extends ContentHandlerDecorator
 {
   private static final XPathParser PARSER = new XPathParser("xhtml", "http://www.w3.org/1999/xhtml");
 
   private static final Matcher MATCHER = PARSER.parse("/xhtml:html/xhtml:body/descendant:node()");
 
   public LucidBodyContentHandler(ContentHandler handler)
   {
     super(new MatchingContentHandler(handler, MATCHER));
   }
 
   public LucidBodyContentHandler(Writer writer)
   {
     this(new LucidWriteOutContentHandler(writer));
   }
 
   public LucidBodyContentHandler(OutputStream stream, String encoding)
   {
     this(new LucidWriteOutContentHandler(stream, Charset.forName(encoding)));
   }
 
   public LucidBodyContentHandler(OutputStream stream, Charset charset)
   {
     this(new LucidWriteOutContentHandler(stream, charset));
   }
 
   public LucidBodyContentHandler(int writeLimit)
   {
     this(new LucidWriteOutContentHandler(writeLimit));
   }
 
   public LucidBodyContentHandler()
   {
     this(new LucidWriteOutContentHandler());
   }
 }

