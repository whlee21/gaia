 package com.digitalpebble.behemoth.tika;
 
 import com.digitalpebble.behemoth.BehemothDocument;
 import com.digitalpebble.behemoth.DocumentProcessor;
 import java.io.BufferedInputStream;
 import java.io.FileNotFoundException;
 import java.io.IOException;
 import java.io.InputStream;
 import java.net.URI;
 import java.net.URLDecoder;
 import java.util.Locale;
 import java.util.zip.GZIPInputStream;
 import org.apache.commons.compress.archivers.ArchiveEntry;
 import org.apache.commons.compress.archivers.ArchiveInputStream;
 import org.apache.commons.compress.archivers.ArchiveStreamFactory;
 import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
 import org.apache.hadoop.fs.FSDataInputStream;
 import org.apache.hadoop.fs.FileStatus;
 import org.apache.hadoop.fs.FileSystem;
 import org.apache.hadoop.fs.Path;
 import org.apache.hadoop.io.MapWritable;
 import org.apache.hadoop.io.Text;
 import org.apache.hadoop.mapred.JobConf;
 import org.apache.hadoop.mapred.MapReduceBase;
 import org.apache.hadoop.mapred.Mapper;
 import org.apache.hadoop.mapred.OutputCollector;
 import org.apache.hadoop.mapred.Reporter;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
 
 public class DirectAccessTikaMapper extends MapReduceBase
   implements Mapper<Text, Text, Text, BehemothDocument>
 {
   private static final Logger LOG = LoggerFactory.getLogger(DirectAccessTikaMapper.class);
   protected DocumentProcessor processor;
   int MAX_SIZE = 20971520;
   protected JobConf conf;
   private Text key = new Text();
 
   public void map(Text path, Text type, OutputCollector<Text, BehemothDocument> outputCollector, Reporter reporter)
     throws IOException
   {
     String pathString = path.toString();
     try {
       pathString = URLDecoder.decode(path.toString(), "UTF-8");
     } catch (Exception e) {
       LOG.warn("Invalid URLEncoded string, file might be inaccessible: " + e.toString());
       pathString = path.toString();
     }
 
     Path p = new Path(pathString);
     FileSystem fs = p.getFileSystem(this.conf);
     if (!fs.exists(p)) {
       LOG.warn("File could not be found! " + p.toUri());
       if (reporter != null)
         reporter.getCounter("TIKA", "NOT_FOUND");
       return;
     }
     String uri = p.toUri().toString();
     int processed = 0;
     String fn = p.getName().toLowerCase(Locale.ENGLISH);
     if (((type.toString().equals("seq")) || 
       (!type.toString().equals("map"))) || (
       (processed == 0) && (isArchive(fn)))) {
       InputStream fis = null;
       try {
         fis = fs.open(p);
         if ((fn.endsWith(".gz")) || (fn.endsWith(".tgz")))
           fis = new GZIPInputStream(fis);
         else if ((fn.endsWith(".tbz")) || (fn.endsWith(".tbz2")) || (fn.endsWith(".bzip2"))) {
           fis = new BZip2CompressorInputStream(fis);
         }
         ArchiveInputStream input = new ArchiveStreamFactory().createArchiveInputStream(new BufferedInputStream(fis));
 
         ArchiveEntry entry = null;
         while ((entry = input.getNextEntry()) != null) {
           String name = entry.getName();
           long size = entry.getSize();
           byte[] content = new byte[(int)size];
           input.read(content);
           this.key.set(uri + "!" + name);
 
           BehemothDocument value = new BehemothDocument();
           value.setUrl(uri + ":" + name);
           value.setContent(content);
           processed++;
           BehemothDocument[] documents = this.processor.process(value, reporter);
           if (documents != null) {
             for (int i = 0; i < documents.length; i++)
               outputCollector.collect(this.key, documents[i]);
           }
           else
             LOG.info("Empty parsing result for " + value.getUrl());
         }
       }
       catch (Throwable t)
       {
         if (processed == 0)
           LOG.warn("Error unpacking archive: " + p + ", adding as a regular file: " + t.toString());
         else
           LOG.warn("Error unpacking archive: " + p + ", processed " + processed + " entries, skipping remaining entries: " + t.toString());
       }
       finally {
         if (fis != null) {
           fis.close();
         }
       }
     }
     if (processed == 0) {
       int realSize = (int)fs.getFileStatus(p).getLen();
       int maxLen = Math.min(this.MAX_SIZE, realSize);
       byte[] fileBArray = new byte[maxLen];
       FSDataInputStream fis = null;
       try {
         fis = fs.open(p);
         fis.readFully(0L, fileBArray);
         fis.close();
         this.key.set(uri);
 
         BehemothDocument value = new BehemothDocument();
         value.setUrl(uri);
         value.setContent(fileBArray);
         if (realSize > maxLen) {
           value.getMetadata(true).put(new Text("fetch"), new Text("truncated " + realSize + " to " + maxLen + " bytes."));
         }
         BehemothDocument[] documents = this.processor.process(value, reporter);
         if (documents != null) {
           for (int i = 0; i < documents.length; i++)
             outputCollector.collect(this.key, documents[i]);
         }
         else
           LOG.info("Empty parsing result for " + value.getUrl());
       }
       catch (FileNotFoundException e)
       {
         LOG.warn("File not found " + p + ", skipping: " + e);
         collectErrorDoc(outputCollector, p, e);
       } catch (IOException e) {
         LOG.warn("IO error reading file " + p + ", skipping: " + e);
         collectErrorDoc(outputCollector, p, e);
       } finally {
         if (fis != null)
           fis.close();
       }
     }
   }
 
   private static void collectErrorDoc(OutputCollector<Text, BehemothDocument> collector, Path p, Throwable t) throws IOException
   {
     BehemothDocument doc = new BehemothDocument();
     Text key = new Text(p.toUri().toString());
     doc.setUrl(key.toString());
     doc.getMetadata(true).put(new Text("fetch"), new Text("error: " + t.toString()));
     collector.collect(key, doc);
   }
 
   private static boolean isArchive(String n) {
     if ((n.endsWith(".cpio")) || (n.endsWith(".jar")) || (n.endsWith(".dump")) || (n.endsWith(".ar")) || (n.endsWith("tar")) || (n.endsWith(".zip")) || (n.endsWith("tar.gz")) || (n.endsWith(".tgz")) || (n.endsWith(".tbz2")) || (n.endsWith(".tbz")) || (n.endsWith("tar.bzip2")))
     {
       return true;
     }
     return false;
   }
 
   public void configure(JobConf job)
   {
     this.conf = job;
     String handlerName = job.get("tika.processor");
     LOG.info("Configured DocumentProcessor class: " + handlerName);
     if (handlerName != null) {
       Class handlerClass = job.getClass("tika.processor", DocumentProcessor.class);
       try {
         this.processor = ((DocumentProcessor)handlerClass.newInstance());
       }
       catch (InstantiationException e) {
         LOG.error("Exception", e);
 
         throw new RuntimeException(e);
       } catch (IllegalAccessException e) {
         LOG.error("Exception", e);
         throw new RuntimeException(e);
       }
     } else {
       this.processor = new TikaProcessor();
     }
     LOG.info("Using DocumentProcessor class: " + this.processor.getClass().getName());
     this.processor.setConf(job);
   }
 }

