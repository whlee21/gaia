package gaia;

import java.io.File;

public interface Constants {
	public static final String OS_NAME = System.getProperty("os.name");
	public static final boolean IS_WINDOWS = OS_NAME.startsWith("Windows");
	public static final boolean IS_OS2 = OS_NAME.startsWith("OS/2");
	public static final boolean IS_UNIX = (!IS_OS2) && (!IS_WINDOWS);
	public static final String USER_ATTR = "user";
	public static final String COLLECTION_ATTR = "collection";
	public static final String DATA_SOURCE_CLASSES_ATTR = "dataSourceClasses";
	public static final String DATA_SOURCE_DESCRIPTOR_ATTR = "dataSourceDescriptor";
	public static final String DATA_SOURCE_DESCRIPTORS_ATTR = "dataSourceDescriptors";
	public static final String DATA_SOURCE_ATTR = "theDataSource";
	public static final String ADD_ATTR = "add";
	public static final String COLLECTION_ID_ATTR = "collectionId";
	public static final String DATA_SOURCE_ID_ATTR = "dataSourceId";
	public static final String SORT_TYPES = "sortTypes";
	public static final String SCHEDULE_ATTR = "schedule";
	public static final String CRAWL_SCHEDULE_ATTR = "crawlSchedule";
	public static final String SPLIT_CRAWL_ATTR = "splitCrawl";
	public static final String IN_WIZARD_ATTR = "inWizard";
	public static final String SUCCESS = "success";
	public static final String DISPLAY_STATUS_ATTR = "displayStatus";
	public static final String CLEAN_SUCCESS = "clean.success";
	public static final String AVAILABLE_DRIVERS_ATTR = "availableDrivers";
	public static final String UPLOAD_FAILED = "upload.failed";
	public static final String DRIVER_LOAD_FAILED = "driver.load.failed";
	public static final String FILE_NOT_SAVED = "file.not.saved";
	public static final String DATA_SOURCES_ATTR = "dataSources";
	public static final String SCHEDULED_TIME_PREFIX = "scheduledTime_";
	public static final String SCHEDULED_END_TIME_PREFIX = "scheduledEndTime_";
	public static final String SCHEDULED_REPEAT_PREFIX = "scheduledRepeatUnit_";
	public static final String SCHEDULED_INTERVAL_PREFIX = "interval_";
	public static final String CRAWL_SCHEDULED_PREFIX = "crawl_";
	public static final String DEDUPE_ATTR = "dedupe";
	public static final String DEDUPE_FIELDS_ATTR = "dedupe_fields";
	public static final String APPLY_TO_ALL_ATTR = "applyToAll";
	public static final String INDEX_DISK_SIZE = "indexDiskSize";
	public static final String TOTAL_DISK_SIZE = "totalDiskSize";
	public static final String NO_RESPONSE_EXISTS = "no.response.exists";
	public static final String SKIPPED = "skipped";
	public static final String SCHEDULED = "scheduled";
	public static final String TRIGGER_NAME_APPEND = "_Trigger";
	public static final String ERRORS = "errors";
	public static final String JOBS = "jobStates";
	public static final int ONE_MEGABYTE = 1048576;
	public static final int DEFAULT_MAX_BYTES = 10485760;
	public static final int DEFAULT_MAX_FAILED_DB_COLUMNS = 5;
	public static final String SEARCH_COMPONENTS_ATTR = "searchComponents";
	public static final String FIELD_TYPES_ATTR = "fieldTypes";
	public static final String REQUEST_HANDLERS_ATTR = "requestHandlers";
	public static final String DOC_FAILED_PREFIX = "Doc failed: ";
	public static final String DOC_SUCCEEDED_PREFIX = "Doc succeeded: ";
	public static final String DOC_SKIPPED_PREFIX = "Doc skipped: ";
	public static final String QUERY_REQUEST_LOG_SUBSTRING = "req_type=main";
	public static final String LOG_FILE_NAME_PREFIX = "core.";
	public static final String INTERRUPTABLE_KEY = "interruptable";
	public static final String JOB_REPEAT_UNITS = "repeatUnits";
	public static final String DEFAULT_SOLR_XML_DIR = "gaia_solrxml";
	public static final String UNKNOWN_KEY_ERROR = "Unknown or dissallowed key found:";
	public static final String GAIA_APP_HOME = System.getProperty("app.dir");
	public static final String GAIA_CRAWLERS_HOME = System.getProperty("crawler.dir", GAIA_APP_HOME + File.separator
			+ "crawlers");

	public static final String GAIA_CRAWLERS_SHARED_HOME = GAIA_CRAWLERS_HOME + File.separator + "shared";
	public static final String GAIA_HADOOP_JOB_HOME = System.getProperty("GAIA_HADOOP_JOB_HOME", GAIA_APP_HOME
			+ File.separator + "hadoop");

	public static final String GAIA_CONF_HOME = System.getProperty("conf.dir");
	public static final String GAIA_DATA_HOME = System.getProperty("data.dir");
	public static final String GAIA_LOGS_HOME = System.getProperty("logs.dir");
	public static final String GAIA_CRAWLER_RESOURCES_HOME = System.getProperty("crawler.resource.dir", GAIA_DATA_HOME
			+ File.separator + "crawler-resources");

	public static final String STORAGE_PATH = GAIA_DATA_HOME + File.separator + "search";
	public static final String DEFAULT_COLLECTION = "collection1";
	public static final String LOGS_COLLECTION = "GaiaSearchLogs";
	public static final String BLOCK_UPDATES_ATTR = "blockUpdates";
	public static final String BLOCK_UPDATES_REASON_ATTR = "blockUpdatesReason";
	public static final boolean IS_CLOUDY = Boolean.parseBoolean(System.getProperty("cloud.mode"));
	public static final int GCM_PORT = Integer.parseInt(System.getProperty("gcm.port", "-1"));
	public static final int DEFAULT_COMMIT_WITHIN = 900000;
	public static final String SCHEDULE_TYPE_CRAWL = "crawl";
	public static final String ACTIVITY_STARTED = "activity_started";
	public static final String ACTIVITY_FINISHED = "activity_finished";
}
