package gaia.crawl;

import gaia.api.Error;
import gaia.crawl.batch.BatchStatus;
import gaia.crawl.datasource.DataSource;
import gaia.crawl.datasource.DataSourceId;
import gaia.crawl.resource.Resource;
import gaia.crawl.security.SecurityFilter;

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.solr.common.util.DateUtil;

public interface ConnectorManager {
	public static final String CC_APERTURE = "gaia.aperture";
	public static final String CC_GCM = "gaia.gcm";
	public static final String CC_DIH = "gaia.jdbc";
	public static final String CC_SOLRXML = "gaia.solrxml";
	public static final String CC_GAIALOGS = "gaia.logs";
	public static final String CC_EXTERNAL = "gaia.external";
	public static final String CC_FS = "gaia.fs";
	public static final String CC_TCS = "gaia.twitter.stream";
	public static final String CC_HV_HDFS = "gaia.map.reduce.hdfs";
	public static final String CC_MONGO_DB = "gaia.mongodb";
	public static final String UC_FILE = "gaia.file";
	public static final String UC_NULL = "gaia.null";
	public static final String UC_SOLRJ = "gaia.solrj";
	public static final String UC_DIRECTSOLR = "gaia.directsolr";
	public static final String UC_BUFFERING = "gaia.buffering";
	public static final String UC_BATCHTEE = "gaia.batchtee";
	public static final String UC_HBASE = "gaia.hbase";
	public static final String CONNECTORS_URL = "connector.url";
	// FIXME: by whlee21
	public static final String[][] DEFAULT_BUILTIN_CRAWLERS = {
			{ "gaia.crawl.aperture.ApertureCrawlerController", CC_APERTURE },
			// { "gaia.crawl.gcm.GCMController", CC_GCM },
			{ "gaia.crawl.dih.DIHCrawlerController", CC_DIH },
			{ "gaia.crawl.solrxml.SolrXmlCrawlerController", CC_SOLRXML },
			{ "gaia.crawl.gaialogs.LogCrawlerController", CC_GAIALOGS },
			// { "gaia.crawl.external.ExternalCrawlerController", CC_EXTERNAL },
			{ "gaia.crawl.fs.FSCrawlerController", CC_FS },
			{ "gaia.crawl.twitter.stream.TwitterStreamController", CC_TCS },
			{ "gaia.crawl.behemoth.BehemothController", CC_HV_HDFS },
	// { "gaia.crawl.mongodb.MongoDBCrawlerController", CC_MONGO_DB }
	};

	public static final String[][] DEFAULT_BUILTIN_UPDATERS = {
		  { "gaia.crawl.impl.FileUpdateController", UC_FILE },
			{ "gaia.crawl.UpdateController.NullUpdateController", UC_NULL },
			{ "gaia.crawl.impl.SolrJUpdateController", UC_SOLRJ },
			{ "gaia.crawl.impl.DirectSolrUpdateController", UC_DIRECTSOLR },
			{ "gaia.crawl.batch.file.BufferingUpdateController", UC_BUFFERING },
			{ "gaia.crawl.batch.BatchTeeUpdateController", UC_BATCHTEE },
			{ "gaia.crawl.impl.HBaseUpdateController", UC_HBASE }
	};

	public String getVersion() throws Exception;

	public ConnectorStatus getConnectorStatus();

	public Set<String> getCrawlerTypes() throws Exception;

	public List<String> initCrawlersFromJar(String paramString) throws Exception;

	public boolean initCrawler(String paramString1, String paramString2) throws Exception;

	public boolean isAvailable(String paramString) throws Exception;

	public Map<String, Object> getCrawlerSpecs(String paramString) throws Exception;

	public Map<String, Object> getCrawlerSpec(String paramString1, String paramString2) throws Exception;

	public DataSource createDataSource(Map<String, Object> paramMap) throws DataSourceFactoryException, Exception;

	public DataSource validateDataSource(DataSource paramDataSource, boolean paramBoolean1, boolean paramBoolean2)
			throws DataSourceFactoryException, Exception;

	public void setClosing(String paramString1, String paramString2, boolean paramBoolean) throws Exception;

	public Map<String, Object> reset(String paramString1, String paramString2, DataSourceId paramDataSourceId)
			throws Exception;

	public Map<String, Object> resetAll(String paramString1, String paramString2) throws Exception;

	public List<CrawlId> close(String paramString1, String paramString2, boolean paramBoolean) throws Exception;

	public Map<String, Object> crawl(DataSourceId paramDataSourceId) throws Exception;

	public List<Map<String, Object>> listJobs(String paramString, boolean paramBoolean) throws Exception;

	public Map<String, Object> getStatus(String paramString, CrawlId paramCrawlId) throws Exception;

	public boolean stopJob(String paramString, CrawlId paramCrawlId, boolean paramBoolean, long paramLong)
			throws Exception;

	public List<Error> finishAllJobs(String paramString1, String paramString2, boolean paramBoolean, long paramLong)
			throws Exception;

	public boolean addDataSource(DataSource paramDataSource) throws Exception;

	public boolean exists(DataSourceId paramDataSourceId) throws Exception;

	public DataSource getDataSource(DataSourceId paramDataSourceId) throws Exception;

	public List<DataSource> getDataSources(String paramString) throws Exception;

	public List<DataSourceId> listDataSources(String paramString) throws Exception;

	public boolean removeDataSource(DataSourceId paramDataSourceId, boolean paramBoolean) throws Exception;

	public List<DataSourceId> removeDataSources(String paramString, boolean paramBoolean) throws Exception;

	public boolean updateDataSource(DataSource paramDataSource) throws DataSourceFactoryException, Exception;

	public void reset(boolean paramBoolean) throws Exception;

	public void shutdown() throws Exception;

	public List<BatchStatus> listBatches(String paramString1, String paramString2, String paramString3) throws Exception;

	public BatchStatus getBatchStatus(String paramString1, String paramString2, String paramString3) throws Exception;

	public boolean deleteBatches(String paramString1, String paramString2, String paramString3) throws Exception;

	public CrawlId startBatchJob(String paramString1, String paramString2, String paramString3,
			DataSource paramDataSource, boolean paramBoolean1, boolean paramBoolean2) throws Exception;

	public List<Map<String, Object>> getBatchJobStatuses(String paramString1, String paramString2, String paramString3)
			throws Exception;

	public List<Map<String, Object>> getHistory(String paramString) throws Exception;

	public Map<String, Object> getCumulativeHistory(String paramString) throws Exception;

	public void removeHistory(String paramString) throws Exception;

	public List<Resource> listResources(String paramString1, String paramString2, DataSourceId paramDataSourceId)
			throws Exception;

	public void uploadResource(String paramString1, Resource paramResource, InputStream paramInputStream,
			String paramString2, DataSourceId paramDataSourceId) throws Exception;

	public void deleteResource(String paramString1, String paramString2, String paramString3,
			DataSourceId paramDataSourceId) throws Exception;

	public InputStream openResource(String paramString1, String paramString2, String paramString3,
			DataSourceId paramDataSourceId) throws Exception;

	public SecurityFilter buildSecurityFilter(DataSourceId paramDataSourceId, String paramString) throws Exception;

	public void deleteOutputData(DataSourceId paramDataSourceId) throws UnsupportedOperationException, Exception;

	public static enum CStatus {
		OK, WARNING, ERROR;
	}

	public static final class ConnectorStatus {
		public CStatus status = CStatus.OK;
		public List<Map<String, Object>> messages = new LinkedList<Map<String, Object>>();
		public String type;
		public Map<String, Object> clients = new HashMap<String, Object>();

		public ConnectorStatus() {
		}

		public ConnectorStatus(String type, CStatus s, String message) {
			this.type = type;
			this.status = s;
			addMessage(message);
		}

		public Map<String, Object> toMap() {
			HashMap<String, Object> m = new HashMap<String, Object>();
			m.put("type", type);
			m.put("status", status.toString());
			synchronized (messages) {
				m.put("messages", messages);
			}
			m.put("clients", clients);
			return m;
		}

		public void addMessage(String message) {
			HashMap<String, Object> m = new HashMap<String, Object>();
			StringBuilder sb = new StringBuilder();
			try {
				DateUtil.formatDate(new Date(), null, sb);
			} catch (IOException e) {
				e.printStackTrace();
			}
			m.put("time", sb.toString());
			m.put("message", message);
			synchronized (messages) {
				messages.add(m);
				if (messages.size() > 10)
					messages.remove(0);
			}
		}

		public synchronized void clearMessages() {
			messages.clear();
		}
	}
}
