 package gaia.crawl.mongodb;
 
 import gaia.Constants;
 import gaia.crawl.CrawlProcessor;
 import gaia.crawl.CrawlState;
 import gaia.crawl.CrawlStatus;
 import gaia.crawl.CrawlStatus.Counter;
 import gaia.crawl.CrawlStatus.JobState;
 import gaia.crawl.UpdateController;
 import gaia.crawl.datasource.DataSource;
 import gaia.crawl.datasource.DataSourceId;
 import gaia.mongodb.BasicMongoDBSolrConnector;
 import gaia.mongodb.BasicMongoDBSolrConnector.ConnectorStatusListener;
 import gaia.mongodb.BasicMongoDBSolrConnector.State;
 import gaia.mongodb.ConnectorProperties;
 import gaia.mongodb.FileTimestampStore;
 import gaia.mongodb.TimestampStore;
 import gaia.mongodb.outputs.ConnectorOutput;
 import java.io.File;
 import org.apache.solr.common.SolrException;
 import org.apache.solr.common.SolrInputDocument;
 import org.apache.solr.common.SolrInputField;
 import org.bson.types.BSONTimestamp;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
 
 public class MongoDBCrawler
   implements Runnable, BasicMongoDBSolrConnector.ConnectorStatusListener, ConnectorOutput
 {
   private static final Logger logger = LoggerFactory.getLogger(MongoDBCrawler.class);
 
   public static String DATA_DIR = Constants.LWE_DATA_HOME + File.separator + "gaia.mongodb";
   private final TimestampStore timestampStore;
   private CrawlState state;
   private DataSource dataSource;
   private BasicMongoDBSolrConnector connector;
   private Throwable throwable = null;
 
   public MongoDBCrawler(CrawlState state) {
     this.state = state;
     this.dataSource = state.getDataSource();
     this.connector = new BasicMongoDBSolrConnector();
 
     new File(DATA_DIR).mkdirs();
     String filename = DATA_DIR + File.separator + this.dataSource.getDataSourceId().getId() + ".timestamp";
     this.timestampStore = new FileTimestampStore(filename);
   }
 
   public void run()
   {
     logger.info("Starting MongoDB crawler");
     this.state.getStatus().starting();
     try {
       this.state.getProcessor().start();
       ConnectorProperties properties = createConnectorProperties();
       this.connector.setProperties(properties);
       this.connector.setConnectorOutput(this);
       this.connector.setTimestampStore(this.timestampStore);
       this.connector.addStatusListener(this);
       this.connector.run();
     } catch (Throwable t) {
       this.state.getStatus().failed(t);
     }
   }
 
   public void stop() {
     this.state.getStatus().setState(CrawlStatus.JobState.STOPPING);
     this.connector.stop();
   }
 
   public void abort() {
     this.state.getStatus().setState(CrawlStatus.JobState.ABORTING);
     this.connector.abort();
   }
 
   private ConnectorProperties createConnectorProperties() throws Exception {
     String mongoDbHost = this.dataSource.getString("host");
     int port = this.dataSource.getInt("port");
     String username = this.dataSource.getString("username");
     String password = this.dataSource.getString("password");
     String collectionNames = this.dataSource.getString("collections");
 
     String converterClassName = LucidworksDocumentConverter.class.getName();
     boolean initialSync = this.dataSource.getBoolean("perform_initial_sync");
     boolean watchOplog = this.dataSource.getBoolean("process_oplog");
 
     logger.info("Creating ConnectorProperties object with the following arguments  host : " + mongoDbHost + " port : " + port + " username : " + username + " password : " + password + " collectionNames : " + collectionNames + " converterClassName : " + converterClassName + " initialSync : " + initialSync + " watchOplog : " + watchOplog);
 
     ConnectorProperties properties = new ConnectorProperties();
     properties.setMongoDbHost(mongoDbHost);
     properties.setMongoDbPort(port);
     properties.setMongoDbUsername(username);
     properties.setMongoDbPassword(password);
     properties.setDocumentConverterClass(converterClassName);
     properties.setNamespaceInclusions(collectionNames);
     properties.setPerformInitialSync(initialSync);
     properties.setProcessOplog(watchOplog);
     return properties;
   }
 
   public void started()
   {
     logger.info("Starting crawl");
     this.state.getStatus().running();
   }
 
   public void connected()
   {
     logger.info("Established connection to MongoDB server");
   }
 
   public void initialSyncFinished()
   {
     logger.info("Initial sync finished");
     if (this.dataSource.getBoolean("process_oplog")) {
       this.state.getStatus().setMessage("Initial sync finished");
       this.state.getStatus().end(CrawlStatus.JobState.FINISHED);
       this.state.getStatus().reset();
       this.state.getStatus().running();
     }
   }
 
   public void exceptionThrown(Throwable t)
   {
     this.throwable = t;
     this.state.getStatus().setState(CrawlStatus.JobState.STOPPING);
   }
 
   public void finished(BasicMongoDBSolrConnector.State endState)
   {
     logger.info("Finishing with connector state: " + endState.toString());
     try {
       this.state.getProcessor().finish();
     } catch (Exception e) {
       logger.info("Could not commit changes", e);
     }
 
     switch (1.$SwitchMap$com$gaia$mongodb$BasicMongoDBSolrConnector$State[endState.ordinal()]) {
     case 1:
       this.state.getStatus().end(CrawlStatus.JobState.FINISHED);
       break;
     case 2:
       this.state.getStatus().end(CrawlStatus.JobState.STOPPED);
       break;
     case 3:
       this.state.getStatus().end(CrawlStatus.JobState.ABORTED);
       break;
     case 4:
       if (this.throwable != null)
         this.state.getStatus().failed(this.throwable);
       else {
         this.state.getStatus().failed(new Exception("Unable to retrieve the exception thrown"));
       }
       break;
     default:
       logger.warn("Invalid ending state: " + endState);
       this.state.getStatus().end(CrawlStatus.JobState.FINISHED);
     }
   }
 
   public void addDocument(SolrInputDocument document)
     throws Exception
   {
     if (document == null)
     {
       this.state.getStatus().incrementCounter(CrawlStatus.Counter.Failed);
     }
     else try {
         this.state.getProcessor().processParsedDocument(document);
         SolrInputField timestampField = document.getField("_timestamp");
         if (timestampField != null) {
           this.timestampStore.saveTimestamp((BSONTimestamp)timestampField.getValue());
         }
         document.removeField("_timestamp");
       }
       catch (SolrException e) {
         logger.error("Error while adding document", e);
         this.state.getStatus().incrementCounter(CrawlStatus.Counter.Failed);
       }
   }
 
   public void updateDocument(SolrInputDocument document)
     throws Exception
   {
     if (document == null)
     {
       this.state.getStatus().incrementCounter(CrawlStatus.Counter.Failed);
     } else {
       this.state.getProcessor().getUpdateController().add(document);
       this.state.getStatus().incrementCounter(CrawlStatus.Counter.Updated);
 
       SolrInputField timestampField = document.getField("_timestamp");
       if (timestampField != null) {
         this.timestampStore.saveTimestamp((BSONTimestamp)timestampField.getValue());
       }
       document.removeField("_timestamp");
     }
   }
 
   public void deleteById(String id)
     throws Exception
   {
     logger.info("Deleting document with id " + id);
     this.state.getProcessor().delete(id);
   }
 
   public void deleteByNamespace(String dbName, String collectionName) throws Exception
   {
     String namespace = dbName + "." + collectionName;
     String query = "attr__namespace : " + namespace;
     logger.info("Dropping collection: [" + query + "]");
     try {
       this.state.getProcessor().getUpdateController().deleteByQuery(query);
       this.state.getStatus().setMessage("Drop collection operation was requested");
       this.state.getStatus().end(CrawlStatus.JobState.FINISHED);
       this.state.getStatus().reset();
       this.state.getStatus().running();
     } catch (Exception e) {
       logger.error("Cannot delete by query", e);
       throw e;
     }
   }
 }

