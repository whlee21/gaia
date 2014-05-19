package gaia.crawl.mongodb;

import gaia.mongodb.converters.DotRepresentationTwoConverter;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import org.apache.solr.common.SolrInputDocument;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LucidworksDocumentConverter extends DotRepresentationTwoConverter {
	private final Logger logger = LoggerFactory.getLogger(LucidworksDocumentConverter.class);

	protected void convertSimpleField(String fieldName, Object fieldValue)
   {
     Class[] supportedClasses = { Integer.class, String.class, Boolean.class, Double.class, Date.class, [B.class, ObjectId.class };
 
     if (fieldValue != null) {
       Class valueClass = fieldValue.getClass();
       if (Arrays.asList(supportedClasses).contains(valueClass)) {
         if (fieldName.equals("id"))
           fieldName = "id_";
         solrDocument.addField(fieldName, fieldValue);
       } else {
         logger.warn("This class " + valueClass + " is not supported");
       }
     }
   }
}
