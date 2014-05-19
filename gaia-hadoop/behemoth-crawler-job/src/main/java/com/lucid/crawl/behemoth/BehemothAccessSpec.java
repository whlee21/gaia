 package com.lucid.crawl.behemoth;
 
 import com.digitalpebble.behemoth.tika.TikaProcessor;
 import com.lucid.api.Error;
 import com.lucid.crawl.datasource.DataSourceSpec;
 import com.lucid.crawl.datasource.FieldMapping;
 import com.lucid.crawl.datasource.FieldMappingUtil;
 import com.lucid.spec.SpecProperty;
 import com.lucid.spec.Validator;
 import com.lucid.utils.MasterConfUtil;
 import java.net.URL;
 import java.util.ArrayList;
 import java.util.Collections;
 import java.util.List;
 import java.util.Map;
 import java.util.regex.Matcher;
 import java.util.regex.Pattern;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
 
 public class BehemothAccessSpec extends DataSourceSpec
 {
   private static transient Logger log = LoggerFactory.getLogger(BehemothAccessSpec.class);
   public static final String PATH = "path";
   public static final String WORK_PATH = "work_path";
   public static final String TIKA_PROCESSOR = "tika_content_handler";
   public static final String MIME_TYPE = "mime_type";
   public static final String ADD_METADATA = "add_metadata";
   public static final String ADD_ANNOTATIONS = "add_annotations";
   public static final String ANNOTATIONS = "annotations";
   public static final String RECURSE = "recurse";
   public static final String ZK_HOST = "zookeeper_host";
   public static final String HADOOP_CONF = "hadoop_conf";
   public static final String DIRECT_ACCESS = "direct_access";
 
   public BehemothAccessSpec()
   {
     super("high_volume_hdfs");
   }
 
   protected BehemothAccessSpec(String name) {
     super(name);
   }
 
   protected void addCrawlerSupportedProperties()
   {
     addCommonBehemothProperties();
     addSpecProperty(new SpecProperty("hadoop_conf", "The location of Hadoop configuration files", String.class, null, Validator.NOT_NULL_VALIDATOR, true));
 
     addCommitProperties();
 
     addFieldMappingProperties();
   }
 
   protected void addCommonBehemothProperties()
   {
     addSpecProperty(new SpecProperty("recurse", "Boolean indicating whether we should descend into sub directories.", Boolean.class, Boolean.TRUE, Validator.BOOLEAN_VALIDATOR, false));
 
     addSpecProperty(new SpecProperty("tika_content_handler", "The name of the Behemoth TikaProcessor class to use.  The default is " + TikaProcessor.class.getName() + ". Must be available from the Hadoop classpath.", String.class, TikaProcessor.class.getName(), Validator.NOOP_VALIDATOR, false));
 
     addSpecProperty(new SpecProperty("mime_type", "If the MIME type is known for all the content to be processed, provide it here to save time during content extraction", String.class, null, Validator.NOOP_VALIDATOR, false));
 
     addSpecProperty(new SpecProperty("path", "The fully-qualified Hadoop path of the input to be processed", String.class, null, Validator.URI_VALIDATOR, true));
 
     addSpecProperty(new SpecProperty("work_path", "The Hadoop path where the connector can store intermediate results. ", String.class, "/tmp", Validator.NOT_BLANK_VALIDATOR, true));
 
     addSpecProperty(new SpecProperty("add_metadata", "Include metadata obtained during Tika content extraction", Boolean.class, Boolean.TRUE, Validator.BOOLEAN_VALIDATOR, false, SpecProperty.HINTS_ADVANCED));
 
     addSpecProperty(new SpecProperty("add_annotations", "Include Behemoth annotations obtained during content processing", Boolean.class, Boolean.FALSE, Validator.BOOLEAN_VALIDATOR, false, SpecProperty.HINTS_ADVANCED));
 
     addSpecProperty(new SpecProperty("annotations", "Space-separated list of annotations to include (e.g. 'Person.string Token')", String.class, null, Validator.TREAT_BLANK_AS_NULL_VALIDATOR, false, SpecProperty.HINTS_ADVANCED));
 
     addSpecProperty(new SpecProperty("direct_access", "Directly access source files from a shared file system (avoids copying)", Boolean.class, Boolean.TRUE, Validator.BOOLEAN_VALIDATOR, false, SpecProperty.HINTS_ADVANCED));
 
     addSpecProperty(new SpecProperty("zookeeper_host", "ZooKeeper connection string for LucidWorks", String.class, null, new ZKValidator(null), false, SpecProperty.HINTS_ADVANCED));
   }
 
   public FieldMapping getDefaultFieldMapping()
   {
     FieldMapping mapping = new FieldMapping();
     FieldMappingUtil.addTikaFieldMapping(mapping, true);
 
     mapping.defineMapping("text", "body");
     return mapping;
   }
 
   public Map<String, Object> cast(Map<String, Object> input)
   {
     Map map = super.cast(input);
     String zkHost = (String)map.get("zookeeper_host");
     if ((zkHost != null) && (zkHost.trim().length() > 0)) {
       return map;
     }
     String outputType = (String)map.get("output_type");
     String outputArgs = (String)map.get("output_args");
     String collection = (String)map.get("collection");
     if (outputArgs == null) {
       outputArgs = "";
     }
     if ((outputType != null) && ((outputType.trim().length() == 0) || (outputType.equalsIgnoreCase("solr"))) && 
       (!outputArgs.matches(".*http[s]?://.*")))
     {
       String url = null;
       try {
         URL u = MasterConfUtil.getSolrAddress(false, (String)map.get("collection"));
         url = u.toString();
       } catch (Exception e) {
         log.warn("Could not obtain Solr URL, using http://localhost:8888/solr/" + collection + " - indexing may fail!", e);
         url = "http://localhost:8888/solr/" + collection;
       }
       if (outputArgs.length() == 0)
         outputArgs = url;
       else {
         outputArgs = "," + url;
       }
       map.put("output_args", outputArgs);
     }
 
     return map;
   }
 
   private static final class ZKValidator extends Validator
   {
     Pattern pat = Pattern.compile("([\\w-_\\.]+:\\d+(/[\\w-_\\.]*)*[,\\s]*)+");
 
     public List<Error> validate(SpecProperty specProp, Object value)
     {
       if ((value == null) || (value.toString().trim().length() == 0)) {
         if (!specProp.required) {
           return Collections.emptyList();
         }
         List res = new ArrayList(1);
         res.add(new Error(specProp.name, "error.empty.value"));
         return res;
       }
       String str = value.toString();
       if (this.pat.matcher(str).matches()) {
         return Collections.emptyList();
       }
       List res = new ArrayList(1);
       res.add(new Error(specProp.name, Error.E_INVALID_VALUE, "does not match required pattern '" + this.pat.pattern() + "'"));
       return res;
     }
 
     public Object cast(SpecProperty specProp, Object value)
     {
       if (value == null) {
         return null;
       }
       return value.toString().trim();
     }
   }
 }

