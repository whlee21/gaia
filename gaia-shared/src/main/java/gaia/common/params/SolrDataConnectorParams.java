package gaia.common.params;

public interface SolrDataConnectorParams {
	public static final String SOLR_DATA_CONNECTOR = "sdc";
	public static final String PREFIX = "sdc.";
	public static final String OVERWRITE = "sdc.overwrite";
	public static final String FIELD_MAPPING_PREFIX = "sdc.fm.";
	public static final String FIELD_VALUE_PREFIX = "sdc.f.";
	public static final String FIELDNAMES = "sdc.fieldNames";
	public static final String DEFAULT_FIELD = "sdc.df";
	public static final String BOOST = ".boost";
	public static final String MAX_BYTES = "mb";
	public static final String DEPTH = "dep";
	public static final String FOLLOW_SYM_LINKS = "fsm";
	public static final String ID_GENERATOR = "idg";
	public static final String INCLUDE = "incl";
	public static final String EXCLUDE = "ex";
	public static final String CRAWL_MODE = "crawl_mode";
	public static final String SPLIT_CRAWL = "splitCrawl";
	public static final String XML_DIR = "xmlDir";
	public static final String GET_STATUS = "status";
	public static final String WAIT = "wait";
	public static final String LOG_EXTRA_DETAIL = "log_extra_detail";
	public static final String FAIL_UNSUPPORTED_FILE_TYPES = "fail_unsupported_file_types";
	public static final String IMAP_HOST = "imapHost";
	public static final String IMAP_FOLDER = "fldr";
	public static final String IMAP_USER = "impUser";
	public static final String IMAP_SECURE_CONNECTION = "impSecure";
	public static final String IMAP_PASS = "impPass";
	public static final String DB_HANDLER_NAME = "dbHandlerName";
	public static final String CLEAR = "sdc.resetDataSource";
	public static final String BATCH_ID_NAME = "sdc.batchIdName";
	public static final String TYPE = "crawlType";
	public static final String WARN_UNKNOW_MIME_TYPES = "warn_unknown_mime_types";

	public static enum CrawlingType {
		URL, PATH, IMAP, CLEAR, CLEAR_ALL, SHAREPOINT;
	}
}
