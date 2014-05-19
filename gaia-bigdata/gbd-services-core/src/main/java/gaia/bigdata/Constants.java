package gaia.bigdata;

import java.util.regex.Pattern;

public interface Constants {
	public static final String CLASSIFIER_PREFIX = "classifier.";
	public static final String SDA_ZK_CONNECT = "zkhost";
	public static final String HBASE_ZK_CONNECT = "hbase.zk.connect";
	public static final String SOLR_ZK_CONNECT = "solr.zk.connect";
	public static final String HADOOP_BASE_PATH = "hadoop.base.path";
	public static final String HADOOP_CONF_DIR = "hadoop.conf.dir";
	public static final String HADOOP_JOBTRACKER_HOST_PORT = "job.tracker";
	public static final String HADOOP_FS_DEFAULT_NAME = "fs.name";
	public static final String ID_FIELD = "solr.id.field";
	public static final String COLLECTION = "collection";
	public static final String ID = "id";
	public static final String DOC_SERVICE_NAME_POSTFIX = "_SDA_DS";
	public static final String TYPE = "type";
	public static final String DOCUMENT = "document";
	public static final String METRIC = "metric";
	public static final String USER_NAME = "username";
	public static final String ROLE_NAME = "rolename";
	public static final String REGEX = "regex";
	public static final String CREATE_IF_NON_EXISTENT = "createIfNonExistent";
	public static final String COLLOCATIONS_POSTFIX = "_collocations";
	public static final String SOLR_ZK_HOST_PROPERTY = "solr_zkHost";
	public static final String ZK_HOST_PROPERTY = "zkhost";
	public static final String SERVICE_IMPL = "service-impl";
	public static final Pattern MATCH_ALL = Pattern.compile(".*");
	public static final String GAIA_SEARCH_SERVER_ERROR = "GaiaSearch server error.";
	public static final String MODEL_NAME = "model";
	public static final String VEC_TYPE = "vecType";
	public static final String SEQUENTIAL = "sequential";
	public static final String RANDOM = "random";
	public static final String SGD = "SGD";
	public static final String SOURCE_TYPE = "sda_document_service";
}
