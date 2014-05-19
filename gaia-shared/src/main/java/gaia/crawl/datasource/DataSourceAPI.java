package gaia.crawl.datasource;

import java.util.Date;
import java.util.Map;

public interface DataSourceAPI {
	public static final String URL = "url";
	public static final String EXCLUDE_PATHS = "exclude_paths";
	public static final String INCLUDE_PATHS = "include_paths";
	public static final String CRAWL_BOUNDS = "bounds";
	public static final String MAX_BYTES = "max_bytes";
	public static final String MAX_THREADS = "max_threads";
	public static final String INDEX_DIRECTORIES = "index_directories";
	public static final String REMOVE_OLD_DOCS = "remove_old_docs";
	public static final String MAX_DOCS = "max_docs";
	public static final String CRAWL_DEPTH = "crawl_depth";
	public static final String DISPLAY_NAME = "name";
	public static final String DS_TYPE = "type";
	public static final String CRAWLER_TYPE = "crawler";
	public static final String COLLECTION = "collection";
	public static final String CATEGORY = "category";
	public static final String FIELD_MAPPING = "mapping";
	public static final String ID = "id";
	public static final String PATH = "path";
	public static final String USERNAME = "username";
	public static final String PASSWORD = "password";
	public static final String WINDOWSDOMAIN = "windows_domain";
	public static final String AUTHENTICATION = "auth";
	public static final String PROXY_HOST = "proxy_host";
	public static final String PROXY_PORT = "proxy_port";
	public static final String PROXY_USERNAME = "proxy_username";
	public static final String PROXY_PASSWORD = "proxy_password";
	public static final String PARSING = "parsing";
	public static final String INDEXING = "indexing";
	public static final String CACHING = "caching";
	public static final String COMMIT_WITHIN = "commit_within";
	public static final String COMMIT_ON_FINISH = "commit_on_finish";
	public static final String VERIFY_ACCESS = "verify_access";
	public static final String FS_CRAWL_HOME = "filesystem.crawl.home";
	public static final String IGNORE_ROBOTS = "ignore_robots";
	public static final String FOLLOW_LINKS = "follow_links";
	public static final String ADD_FAILED_DOCS = "add_failed_docs";
	public static final String USE_DIRECT_SOLR = "use_direct_solr";
	public static final String OUTPUT_ARGS = "output_args";
	public static final String OUTPUT_TYPE = "output_type";
	public static final String ENABLE_SECURITY_TRIMMING = "enable_security_trimming";
	public static final String MAX_RETRIES = "max_retries";

	public Date getCreateDate();

	public void setCreateDate(Date paramDate);

	public Date getLastModified();

	public void setLastModified(Date paramDate);

	public String getCategory();

	public void setCategory(String paramString);

	public DataSourceId getDataSourceId();

	public void setDataSourceId(DataSourceId paramDataSourceId);

	public String getDisplayName();

	public void setDisplayName(String paramString);

	public void setCrawlerType(String paramString);

	public String getCrawlerType();

	public String getType();

	public String getCollection();

	public void setCollection(String paramString);

	public FieldMapping getFieldMapping();

	public void setFieldMapping(FieldMapping paramFieldMapping);

	public Map<String, Object> getProperties();

	public Object getProperty(String paramString);

	public Object getProperty(String paramString, Object paramObject);

	public Object setProperty(String paramString, Object paramObject);

	public void setProperties(Map<String, Object> paramMap);

	public int getInt(String paramString);

	public int getInt(String paramString, int paramInt);

	public long getLong(String paramString);

	public long getLong(String paramString, long paramLong);

	public String getString(String paramString);

	public String getString(String paramString1, String paramString2);

	public boolean getBoolean(String paramString);

	public boolean getBoolean(String paramString, boolean paramBoolean);

	public static enum Bounds {
		tree,

		host,

		domain,

		none;
	}
}
