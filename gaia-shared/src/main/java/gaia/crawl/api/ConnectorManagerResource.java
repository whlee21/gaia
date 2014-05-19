package gaia.crawl.api;

import gaia.api.Error;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.restlet.representation.Representation;
import org.restlet.resource.Delete;
import org.restlet.resource.Get;
import org.restlet.resource.Post;
import org.restlet.resource.Put;
import org.restlet.resource.ResourceException;

public interface ConnectorManagerResource {
	public static final String CRAWLER_PARAM = "crawler";
	public static final String COLLECTION_PARAM = "collection";
	public static final String ID_PARAM = "id";
	public static final String NAME_PARAM = "name";
	public static final String DS_TYPE_PARAM = "type";

	@Get("json?m=getVersion")
	public String getVersion() throws ResourceException;

	@Get("json?m=getCrawlerTypes")
	public Set<String> getCrawlerTypes() throws ResourceException;

	@Post("json?m=initCrawlersFromJar")
	public List<String> initCrawlersFromJar(String paramString) throws ResourceException;

	@Post("json?m=initCrawler")
	public boolean initCrawler(Map<String, Object> paramMap) throws ResourceException;

	@Get("json?m=isAvailable")
	public boolean isAvailable() throws ResourceException;

	@Get("json?m=getCrawlerSpecs")
	public Map<String, Object> getCrawlerSpecs() throws ResourceException;

	@Get("json?m=getCrawlerSpec")
	public Map<String, Object> getCrawlerSpec() throws ResourceException;

	@Post("json?m=createDataSource")
	public Map<String, Object> createDataSource(Map<String, Object> paramMap) throws ResourceException;

	@Put("json?m=validateDataSource")
	public Map<String, Object> validateDataSource(Map<String, Object> paramMap) throws ResourceException;

	@Put("json?m=setClosing")
	public void setClosing(Map<String, Object> paramMap) throws ResourceException;

	@Put("json?m=crawlerReset")
	public Map<String, Object> crawlerReset(Map<String, Object> paramMap) throws ResourceException;

	@Put("json?m=crawlerResetAll")
	public Map<String, Object> crawlerResetAll(Map<String, Object> paramMap) throws ResourceException;

	@Put("json?m=close")
	public List<String> close(Map<String, Object> paramMap) throws ResourceException;

	@Put("json?m=crawl")
	public Map<String, Object> crawl(Map<String, Object> paramMap) throws ResourceException;

	@Get("json?m=listJobs")
	public List<Map<String, Object>> listJobs() throws ResourceException;

	@Get("json?m=getJobStatus")
	public Map<String, Object> getJobStatus() throws ResourceException;

	@Put("json?m=stopJob")
	public boolean stopJob(Map<String, Object> paramMap) throws ResourceException;

	@Put("json?m=finishAllJobs")
	public List<Error> finishAllJobs(Map<String, Object> paramMap) throws ResourceException;

	@Post("json?m=addDataSource")
	public boolean addDataSource(Map<String, Object> paramMap) throws ResourceException;

	@Get("json?m=exists")
	public boolean exists() throws ResourceException;

	@Get("json?m=getDataSource")
	public Map<String, Object> getDataSource() throws ResourceException;

	@Get("json?m=getDataSources")
	public List<Map<String, Object>> getDataSources() throws ResourceException;

	@Get("json?m=listDataSources")
	public List<String> listDataSources() throws ResourceException;

	@Delete("json?m=removeDataSource")
	public boolean removeDataSource() throws ResourceException;

	@Delete("json?m=removeDataSources")
	public List<String> removeDataSources() throws ResourceException;

	@Put("json?m=updateDataSource")
	public boolean updateDataSource(Map<String, Object> paramMap) throws ResourceException;

	@Put("json?m=reset")
	public void reset() throws ResourceException;

	@Delete("json?m=shutdown")
	public void shutdown() throws ResourceException;

	@Get("json?m=listBatches")
	public List<Map<String, Object>> listBatches() throws ResourceException;

	@Get("json?m=getBatchStatus")
	public Map<String, Object> getBatchStatus() throws ResourceException;

	@Delete("json?m=deleteBatches")
	public boolean deleteBatches() throws ResourceException;

	@Put("json?m=startBatchJob")
	public String startBatchJob(Map<String, Object> paramMap) throws ResourceException;

	@Get("json?m=getBatchJobStatuses")
	public List<Map<String, Object>> getBatchJobStatuses() throws ResourceException;

	@Get("json?m=getHistory")
	public List<Map<String, Object>> getHistory() throws ResourceException;

	@Get("json?m=getCumulativeHistory")
	public Map<String, Object> getCumulativeHistory() throws ResourceException;

	@Delete("json?m=removeHistory")
	public boolean removeHistory() throws ResourceException;

	@Get("json?m=listResources")
	public List<Map<String, Object>> listResources() throws ResourceException;

	@Put("?m=uploadResource")
	public Representation uploadResource(Representation paramRepresentation) throws ResourceException;

	@Delete("json?m=deleteResource")
	public boolean deleteResource() throws ResourceException;

	@Get("?m=openResource")
	public Representation openResource(Representation paramRepresentation) throws ResourceException;

	@Get("json?m=buildSecurityFilter")
	public Map<String, Object> buildSecurityFilter() throws ResourceException;

	@Delete("json?m=deleteOutputData")
	public void deleteOutputData() throws ResourceException;
}
