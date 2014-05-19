 package gaia.crawl.external;
 
 import gaia.crawl.datasource.DataSourceSpec;
 import gaia.crawl.datasource.DataSourceSpec.Category;
 import gaia.crawl.datasource.FieldMapping;
 import gaia.crawl.datasource.FieldMappingUtil;
 import gaia.spec.SpecProperty;
 import gaia.spec.SpecProperty.Separator;
 import gaia.spec.Validator;
 import java.util.Map;
 
 public class ExternalSpec extends DataSourceSpec
 {
   public static final String SOURCE_TYPE = "source_type";
   public static final String SOURCE = "source";
   public static final String CALLBACK = "callback";
 
   public ExternalSpec()
   {
     super(DataSourceSpec.Category.External.toString());
   }
 
   protected void addCrawlerSupportedProperties()
   {
     addSpecProperty(new SpecProperty("url", "datasource.url", String.class, null, Validator.NULL_VALIDATOR, false, true, SpecProperty.HINTS_DEFAULT));
 
     addSpecProperty(new SpecProperty.Separator("source"));
     addSpecProperty(new SpecProperty("source", "datasource.source", String.class, null, Validator.NOT_NULL_VALIDATOR, true));
 
     addSpecProperty(new SpecProperty("source_type", "datasource.source_type", String.class, null, Validator.NOT_NULL_VALIDATOR, true));
 
     addSpecProperty(new SpecProperty("callback", "datasource.callback", String.class, null, Validator.URL_VALIDATOR, false, SpecProperty.HINTS_ADVANCED));
 
     addFieldMappingProperties();
   }
 
   public Map<String, Object> cast(Map<String, Object> input)
   {
     super.cast(input);
     if (input.get("url") == null) {
       input.put("url", "external:" + input.get("source"));
     }
     return input;
   }
 
   public FieldMapping getDefaultFieldMapping()
   {
     FieldMapping fm = new FieldMapping();
 
     FieldMappingUtil.addTikaFieldMapping(fm, false);
     return fm;
   }
 }

