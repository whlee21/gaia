 package gaia.api;
 
 import gaia.Constants;
import gaia.Defaults;
import gaia.crawl.api.BatchJobServerResource;
import gaia.crawl.api.BatchesServerResource;
import gaia.crawl.api.CrawlersServerResource;
import gaia.crawl.api.CrawlersStatusServerResource;
import gaia.crawl.api.DataSourceCrawlDataServerResource;
import gaia.crawl.api.DataSourceHistoryServerResource;
import gaia.crawl.api.DataSourceIndexResource;
import gaia.crawl.api.DataSourceJobServerResource;
import gaia.crawl.api.DataSourceJobStatusServerResource;
import gaia.crawl.api.DataSourceServerResource;
import gaia.crawl.api.DataSourceValidatorResource;
import gaia.crawl.api.DataSourcesHistoryServerResource;
import gaia.crawl.api.DataSourcesServerResource;
import gaia.crawl.api.FieldMappingServerResource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.restlet.Application;
import org.restlet.Restlet;
import org.restlet.data.Protocol;
import org.restlet.resource.Finder;
import org.restlet.routing.Route;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
 
 @Singleton
 public class RestAPI extends Application
 {
   private static transient Log log = LogFactory.getLog(RestAPI.class);
   public static final String COLLECTION_PARAM = "coll_name";
   private ResourceFinder factory;
   private LWERouter router;
 
   @Inject
   public RestAPI(Injector injector, Defaults defaults)
   {
     setStatusService(new LWEStatusService());
     this.factory = new ResourceFinder(injector);
   }
 
   public void stop() throws Exception
   {
     if (this.router != null)
       this.router.stop();
   }
 
   private Route attach(String path, Finder finder)
   {
     return this.router.attach(path, finder);
   }
 
   private Route attach(String path, Class clazz)
   {
     return attach(path, this.factory.finderOf(clazz));
   }
 
   private Route attachCollections(String path, Class clazz)
   {
     return attach("/collections/{coll_name}" + path, clazz);
   }
 
   private Route attachDatasources(String path, Class clazz)
   {
     return attachCollections("/datasources" + path, clazz);
   }
 
   public synchronized Restlet createInboundRoot()
   {
     AuditLogger.log("api started");
     log.info("lucidworksAppHome set to '" + Constants.GAIA_APP_HOME);
     log.info("lucidworksConfHome set to '" + Constants.GAIA_CONF_HOME);
     log.info("lucidworksDataHome set to '" + Constants.GAIA_DATA_HOME);
     log.info("lucidworksLogsHome set to '" + Constants.GAIA_LOGS_HOME);
     log.info("lucidworksHadoopHome set to '" + Constants.GAIA_HADOOP_JOB_HOME);
     log.info("lucidworksCrawlersHome set to '" + Constants.GAIA_CRAWLERS_HOME);
 
     getTunnelService().setEnabled(true);
     getTunnelService().setExtensionsTunnel(true);
 
     getConnectorService().getClientProtocols().add(Protocol.HTTP);
     getConnectorService().getClientProtocols().add(Protocol.HTTPS);
 
     LWERouter router = new LWERouter(getContext());
     this.router = router;
 
     attachCollectionRoutes();
     attachBatchJobRoutes();
     attachDatasourceRoutes();
     attachJdbcDriverRoutes();
     attachActivityRoutes();
     attachSchemaRoutes();
 
     attachGlobalRoutes();
 
     log.info("createInboundRoot complete");
 
     return router;
   }
 
   private void attachCollectionRoutes()
   {
     attach("/collections", CollectionsServerResource.class);
     attachCollections("", CollectionServerResource.class);
     attachCollections("/info", CollectionInfoServerResource.class);
     attachCollections("/info/{name}", CollectionInfoServerResource.class);
 
     attachCollections("/index", CollectionIndexResource.class);
 
     attachCollections("/security_trimming", SecurityTrimmingServerResource.class);
 
     attachCollections("/filtering", ACLFilteringServerResource.class);
     attachCollections("/filtering/{name}", ACLFilteringServerResource.class);
 
     attachCollections("/components/{name}", HandlerComponentsServerResource.class);
 
     attachCollections("/roles", RolesServerResource.class);
     attachCollections("/roles/{name}", RoleServerResource.class);
 
     attachCollections("/settings", SettingsServerResource.class);
     attachCollections("/settings/{name}", SettingsServerResource.class);
 
     attachCollections("/caches", CachesServerResource.class);
     attachCollections("/caches/{name}", CacheServerResource.class);
   }
 
   private void attachBatchJobRoutes()
   {
     attachCollections("/batches", BatchesServerResource.class);
     attachCollections("/batches/{crawler}", BatchesServerResource.class);
 
     attachCollections("/batches/{crawler}/job", BatchJobServerResource.class);
     attachCollections("/batches/{crawler}/job/{batch_id}", BatchJobServerResource.class);
   }
 
   private void attachDatasourceRoutes()
   {
     attachDatasources("/all/job", DataSourceJobServerResource.class);
     attachDatasources("/all/status", DataSourceJobStatusServerResource.class);
 
     attachDatasources("/{id}/status", DataSourceJobStatusServerResource.class);
 
     attachDatasources("/{id}/crawldata", DataSourceCrawlDataServerResource.class);
     attachDatasources("/{id}/index", DataSourceIndexResource.class);
 
     attachDatasources("/{id}/mapping", FieldMappingServerResource.class);
     attachDatasources("/{id}/mapping/{part}", FieldMappingServerResource.class);
 
     attachDatasources("/{id}/mapping/{part}/{key}", FieldMappingServerResource.class);
 
     attachDatasources("/all/history", DataSourcesHistoryServerResource.class);
     attachDatasources("/{id}/history", DataSourceHistoryServerResource.class);
 
     attachDatasources("", DataSourcesServerResource.class);
     attachDatasources("/validation", DataSourceValidatorResource.class);
     attachDatasources("/{id}", DataSourceServerResource.class);
     attachDatasources("/{id}/schedule", DataSourceScheduleServerResource.class);
     attachDatasources("/{id}/job", DataSourceJobServerResource.class);
   }
 
   private void attachJdbcDriverRoutes()
   {
     attachCollections("/jdbcdrivers/classes", JDBCDriversClassesServerResource.class);
     if (!Constants.IS_CLOUDY)
     {
       attachCollections("/jdbcdrivers", JDBCDriversServerResource.class);
       attachCollections("/jdbcdrivers/{filename}", JDBCDriverServerResource.class);
     }
   }
 
   private void attachActivityRoutes()
   {
     attachCollections("/activities/all/status", ActivitiesStatusServerResource.class);
     attachCollections("/activities/{id}/status", ActivityStatusServerResource.class);
 
     attachCollections("/activities/all/history", ActivitiesHistoryServerResource.class);
     attachCollections("/activities/{id}/history", ActivityHistoryServerResource.class);
     attachCollections("/activities", ActivitiesServerResource.class);
     attachCollections("/activities/{id}", ActivityServerResource.class);
   }
 
   private void attachSchemaRoutes()
   {
     attachCollections("/fields", FieldsServerResource.class);
     attachCollections("/fields/{name}", FieldResource.class);
 
     attachCollections("/dynamicfields", DynamicFieldsServerResource.class);
     attachCollections("/dynamicfields/{name}", DynamicFieldResource.class);
 
     attachCollections("/fieldtypes", FieldTypesServerResource.class);
     attachCollections("/fieldtypes/{name}", FieldTypeServerResource.class);
   }
 
   private void attachGlobalRoutes()
   {
     attach("/crawlers", CrawlersServerResource.class);
     attach("/crawlers/status", CrawlersStatusServerResource.class);
 
     attachCollections("/click", ClickEventServerResource.class);
     attachCollections("/click/analysis", ClickAnalysisServerResource.class);
 
     attach("/collectiontemplates", CollectionTemplatesServerResource.class);
 
     attach("/version", VersionServerResource.class);
 
     attach("/config/ssl", SSLConfigServerResource.class);
 
     attach("/config/master.conf", MasterConfServerResource.class);
 
     attach("/status/lock", LockStatusServerResource.class);
   }
 }

