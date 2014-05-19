 package gaia.api;
 
 import java.util.HashMap;
 import java.util.Map;
 import org.apache.solr.core.SolrCore;
 import org.apache.solr.schema.IndexSchema;
 import org.apache.solr.schema.SchemaField;
 
 public class DynamicFieldAttributeReader extends AbstractFieldAttributeReader
 {
   private static final String INTERNAL_CURRENCY_FIELD_PREFIX = "*___";
   private final Map<String, SchemaField> dynamicFieldPrototypes;
 
   public DynamicFieldAttributeReader(SolrCore solrCore)
   {
     super(solrCore);
     this.dynamicFieldPrototypes = getDynamicFieldPrototypes(solrCore.getLatestSchema());
   }
 
   public static Map<String, SchemaField> getDynamicFieldPrototypes(IndexSchema schema)
   {
     SchemaField[] dynfields = schema.getDynamicFieldPrototypes();
     Map<String, SchemaField> results = new HashMap<String, SchemaField>();
     for (SchemaField proto : dynfields)
     {
       if (!proto.getName().startsWith(INTERNAL_CURRENCY_FIELD_PREFIX)) {
         results.put(proto.getName(), proto);
       }
     }
     return results;
   }
 
   public Map<String, Object> getAttributes(String protoName)
   {
     return getAttributes((SchemaField)dynamicFieldPrototypes.get(protoName));
   }
 }

