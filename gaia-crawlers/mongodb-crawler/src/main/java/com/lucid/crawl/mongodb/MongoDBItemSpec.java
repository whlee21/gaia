 package gaia.crawl.mongodb;
 
 import gaia.api.Error;
 import gaia.crawl.datasource.DataSourceSpec;
 import gaia.crawl.datasource.DataSourceSpec.Category;
 import gaia.crawl.datasource.FieldMapping;
 import gaia.spec.SpecProperty;
 import gaia.spec.SpecProperty.Separator;
 import gaia.spec.Validator;
 import gaia.utils.StringUtils;
 import com.mongodb.DB;
 import com.mongodb.Mongo;
 import com.mongodb.MongoException;
 import java.net.UnknownHostException;
 import java.util.List;
 import java.util.Map;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
 
 public class MongoDBItemSpec extends DataSourceSpec
 {
   private static final Logger logger = LoggerFactory.getLogger(MongoDBItemSpec.class);
   public static final String PROPERTY_HOST = "host";
   public static final String PROPERTY_PORT = "port";
   public static final String PROPERTY_USERNAME = "username";
   public static final String PROPERTY_PASSWORD = "password";
   public static final String COLLECTIONS = "collections";
   public static final String CONVERTER_CLASS = "converter_class";
   public static final String PERFORM_INITIAL_SYNC = "perform_initial_sync";
   public static final String PROCESS_OPLOG = "process_oplog";
   public static final String LAST_TIMESTAMP = "last_timestamp";
 
   protected MongoDBItemSpec()
   {
     super(DataSourceSpec.Category.Other.toString());
   }
 
   protected void addCrawlerSupportedProperties()
   {
     addSpecProperty(new SpecProperty.Separator("Connection parameters"));
     addSpecProperty(new SpecProperty("host", "datasource.host", String.class, "localhost", Validator.URI_VALIDATOR, true));
 
     addSpecProperty(new SpecProperty("port", "datasource.port", Integer.class, Integer.valueOf(27017), Validator.INT_STRING_VALIDATOR, true));
 
     addSpecProperty(new SpecProperty("username", "datasource.username", String.class, null, Validator.NOT_NULL_VALIDATOR, false));
 
     addSpecProperty(new SpecProperty("password", "datasource.password", String.class, null, Validator.NOT_NULL_VALIDATOR, false, SpecProperty.HINTS_PASSWORD));
 
     addSpecProperty(new SpecProperty.Separator("Other parameters"));
     addSpecProperty(new SpecProperty("collections", "datasource.collections", String.class, null, Validator.NOT_NULL_VALIDATOR, false));
 
     addSpecProperty(new SpecProperty("perform_initial_sync", "datasource.perform_initial_sync", Boolean.class, Boolean.valueOf(true), Validator.BOOLEAN_VALIDATOR, true));
 
     addSpecProperty(new SpecProperty("process_oplog", "datasource.process_oplog", Boolean.class, Boolean.valueOf(true), Validator.BOOLEAN_VALIDATOR, true));
 
     addFieldMappingProperties();
     addCommitProperties();
     addVerifyAccessProperties();
   }
 
   public List<Error> validate(Map<String, Object> map)
   {
     List errors = super.validate(map);
     Mongo mongoServer = null;
     try {
       int port = Integer.parseInt(map.get("port").toString());
       mongoServer = new Mongo((String)map.get("host"), port);
     } catch (UnknownHostException ex) {
       errors.add(new Error("host", Error.E_INVALID_VALUE, ex.toString()));
       return errors;
     }
 
     try
     {
       mongoServer.getDatabaseNames();
     } catch (MongoException ex) {
       if (ex.toString().indexOf("Network") != -1) {
         errors.add(new Error("host", Error.E_INVALID_VALUE, ex.toString()));
         return errors;
       }
     }
 
     if ((map.get("verify_access") != null) && (StringUtils.getBoolean(map.get("verify_access")).booleanValue()))
     {
       String collections = (String)map.get("collections");
       String user = (String)map.get("username");
       char[] pass = ((String)map.get("password")).toCharArray();
 
       if (collections.indexOf("*.*") != -1) {
         DB admin = mongoServer.getDB("admin");
         checkAccess(admin, user, pass, errors);
       } else {
         for (String ns : collections.split(",")) {
           String dbName = ns.substring(0, ns.indexOf("."));
           DB db = mongoServer.getDB(dbName);
           checkAccess(db, user, pass, errors);
         }
       }
     }
 
     if (mongoServer != null) {
       mongoServer.close();
     }
     return errors;
   }
 
   private void checkAccess(DB db, String user, char[] pass, List<Error> errors) {
     try {
       db.getCollectionNames();
     } catch (MongoException ex) {
       if (!db.authenticate(user, pass))
         errors.add(new Error("username", Error.E_INVALID_VALUE, "You don't have access to db: " + db.getName()));
     }
   }
 
   public FieldMapping getDefaultFieldMapping()
   {
     return new FieldMapping();
   }
 }

