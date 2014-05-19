 package gaia.api;
 
import gaia.admin.editor.EditableSchemaConfig;

import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;

import org.apache.solr.core.CoreContainer;
import org.apache.solr.core.SolrCore;
import org.apache.solr.schema.SchemaField;
 
 public class DynamicFieldAttribs
 {
   public final Map<String, SchemaField> updatedFields = new TreeMap<String, SchemaField>();
   public EditableSchemaConfig esc;
 
   public DynamicFieldAttribs(CoreContainer cores, SolrCore core)
   {
     this.esc = new EditableSchemaConfig(core, cores.getZkController());
 
     Map<String, SchemaField> currentFields = DynamicFieldAttributeReader.getDynamicFieldPrototypes(core.getLatestSchema());
 
     this.updatedFields.putAll(currentFields);
   }
 
   public void save()
   {
     this.esc.replaceDynamicFields(this.updatedFields.values());
     try
     {
       this.esc.save();
     } catch (IOException e) {
       throw ErrorUtils.statusExp(e);
     }
   }
 }

