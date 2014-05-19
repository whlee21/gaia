 package com.digitalpebble.behemoth.tika;
 
 import com.digitalpebble.behemoth.BehemothDocument;
 import com.digitalpebble.behemoth.DocumentProcessor;
 import java.io.IOException;
 import org.apache.hadoop.io.Text;
 import org.apache.hadoop.mapred.JobConf;
 import org.apache.hadoop.mapred.MapReduceBase;
 import org.apache.hadoop.mapred.Mapper;
 import org.apache.hadoop.mapred.OutputCollector;
 import org.apache.hadoop.mapred.Reporter;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
 
 public class TikaMapper extends MapReduceBase
   implements Mapper<Text, BehemothDocument, Text, BehemothDocument>
 {
   private static final Logger LOG = LoggerFactory.getLogger(TikaMapper.class);
   protected DocumentProcessor processor;
 
   public void map(Text text, BehemothDocument inputDoc, OutputCollector<Text, BehemothDocument> outputCollector, Reporter reporter)
     throws IOException
   {
     BehemothDocument[] documents = this.processor.process(inputDoc, reporter);
     if (documents != null)
       for (int i = 0; i < documents.length; i++)
         outputCollector.collect(text, documents[i]);
   }
 
   public void configure(JobConf job)
   {
     String handlerName = job.get("tika.processor");
     LOG.info("Configured DocumentProcessor class: " + handlerName);
     if (handlerName != null) {
       Class handlerClass = job.getClass("tika.processor", TikaProcessor.class);
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

