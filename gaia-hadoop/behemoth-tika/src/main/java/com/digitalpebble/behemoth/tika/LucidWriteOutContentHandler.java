 package com.digitalpebble.behemoth.tika;
 
 import java.io.IOException;
 import java.io.OutputStream;
 import java.io.OutputStreamWriter;
 import java.io.StringWriter;
 import java.io.Writer;
 import java.nio.charset.Charset;
 import org.xml.sax.SAXException;
 import org.xml.sax.helpers.DefaultHandler;
 
 public class LucidWriteOutContentHandler extends DefaultHandler
 {
   private final Writer writer;
   private final int writeLimit;
   private int writeCount = 0;
 
   private LucidWriteOutContentHandler(Writer writer, int writeLimit) {
     this.writer = writer;
     this.writeLimit = writeLimit;
   }
 
   public LucidWriteOutContentHandler(Writer writer)
   {
     this(writer, -1);
   }
 
   public LucidWriteOutContentHandler(OutputStream stream, Charset charset)
   {
     this(new OutputStreamWriter(stream, charset));
   }
 
   public LucidWriteOutContentHandler(int writeLimit)
   {
     this(new StringWriter(), writeLimit);
   }
 
   public LucidWriteOutContentHandler()
   {
     this(100000);
   }
 
   public void characters(char[] ch, int start, int length)
     throws SAXException
   {
     try
     {
       if ((this.writeLimit == -1) || (this.writeCount + length <= this.writeLimit)) {
         this.writer.write(ch, start, length);
         this.writeCount += length;
       } else {
         this.writer.write(ch, start, this.writeLimit - this.writeCount);
         this.writeCount = this.writeLimit;
         throw new WriteLimitReachedException(null);
       }
     } catch (IOException e) {
       throw new SAXException("Error writing out character content", e);
     }
   }
 
   public void ignorableWhitespace(char[] ch, int start, int length)
     throws SAXException
   {
     characters(ch, start, length);
   }
 
   public void endDocument()
     throws SAXException
   {
     try
     {
       this.writer.flush();
     } catch (IOException e) {
       throw new SAXException("Error flushing character output", e);
     }
   }
 
   public String toString()
   {
     return this.writer.toString();
   }
 
   public boolean isWriteLimitReached(Throwable t)
   {
     if ((t instanceof WriteLimitReachedException)) {
       return this == ((WriteLimitReachedException)t).getSource();
     }
     return (t.getCause() != null) && (isWriteLimitReached(t.getCause()));
   }
 
   private class WriteLimitReachedException extends SAXException
   {
     private WriteLimitReachedException()
     {
     }
 
     public LucidWriteOutContentHandler getSource() {
       return LucidWriteOutContentHandler.this;
     }
   }
 }

