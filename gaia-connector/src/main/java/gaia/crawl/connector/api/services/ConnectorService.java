package gaia.crawl.connector.api.services;

import gaia.api.ObjectSerializer;
import gaia.crawl.ConnectorManager;
import gaia.crawl.connector.api.services.parsers.RequestBodyParser;

import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;

@Path("/mgr/")
@Produces("application/json")
public class ConnectorService {
	private static final Logger LOG = LoggerFactory.getLogger(ConnectorService.class);

	private Map<String, GetMethodHandler> getMethodHandlers = new HashMap<String, GetMethodHandler>();
	private Map<String, PostMethodHandler> postMethodHandlers = new HashMap<String, PostMethodHandler>();
	private Map<String, PutMethodHandler> putMethodHandlers = new HashMap<String, PutMethodHandler>();
	private Map<String, DeleteMethodHandler> deleteMethodHandlers = new HashMap<String, DeleteMethodHandler>();

	@Inject
	public ConnectorService(ConnectorManager connManager, ObjectSerializer serializer, RequestBodyParser bodyParser) {
		getMethodHandlers.put("getVersion", new GetVersionHandler(connManager, serializer, bodyParser));
		getMethodHandlers.put("getCrawlerTypes", new GetCrawlerTypesHandler(connManager, serializer, bodyParser));
		getMethodHandlers.put("isAvailable", new IsAvailableHandler(connManager, serializer, bodyParser));
		getMethodHandlers.put("getCrawlerSpecs", new GetCrawlerSpecsHandler(connManager, serializer, bodyParser));
		getMethodHandlers.put("getCrawlerSpec", new GetCrawlerSpecHandler(connManager, serializer, bodyParser));
		getMethodHandlers.put("listJobs", new ListJobsHandler(connManager, serializer, bodyParser));
		getMethodHandlers.put("getJobStatus", new GetJobStatusHandler(connManager, serializer, bodyParser));
		getMethodHandlers.put("exists", new ExistsHandler(connManager, serializer, bodyParser));
		getMethodHandlers.put("getDataSource", new GetDataSourceHandler(connManager, serializer, bodyParser));
		getMethodHandlers.put("getDataSources", new GetDataSourcesHandler(connManager, serializer, bodyParser));
		getMethodHandlers.put("listDataSources", new ListDataSourcesHandler(connManager, serializer, bodyParser));
		getMethodHandlers.put("listBatches", new ListBatchesHandler(connManager, serializer, bodyParser));
		getMethodHandlers.put("getBatchStatus", new GetBatchStatusHandler(connManager, serializer, bodyParser));
		getMethodHandlers.put("getBatchJobStatuses", new GetBatchJobStatusesHandler(connManager, serializer, bodyParser));
		getMethodHandlers.put("getHistory", new GetHistoryHandler(connManager, serializer, bodyParser));
		getMethodHandlers.put("getCumulativeHistory", new GetCumulativeHistoryHandler(connManager, serializer, bodyParser));
		getMethodHandlers.put("listResources", new ListResourcesHandler(connManager, serializer, bodyParser));
		getMethodHandlers.put("openResource", new OpenResourceHandler(connManager, serializer, bodyParser));
		getMethodHandlers.put("buildSecurityFilter", new BuildSecurityFilterHandler(connManager, serializer, bodyParser));

		postMethodHandlers.put("initCrawlersFromJar", new InitCrawlersFromJarHandler(connManager, serializer, bodyParser));
		postMethodHandlers.put("initCrawler", new InitCrawlerHandler(connManager, serializer, bodyParser));
		postMethodHandlers.put("createDataSource", new CreateDataSourceHandler(connManager, serializer, bodyParser));
		postMethodHandlers.put("addDataSource", new AddDataSourceHandler(connManager, serializer, bodyParser));

		putMethodHandlers.put("validateDataSource", new ValidateDataSourceHandler(connManager, serializer, bodyParser));
		putMethodHandlers.put("setClosing", new SetClosingHandler(connManager, serializer, bodyParser));
		putMethodHandlers.put("crawlerReset", new CrawlerResetHandler(connManager, serializer, bodyParser));
		putMethodHandlers.put("crawlerResetAll", new CrawlerResetAllHandler(connManager, serializer, bodyParser));
		putMethodHandlers.put("close", new CloseHandler(connManager, serializer, bodyParser));
		putMethodHandlers.put("crawl", new CrawlHandler(connManager, serializer, bodyParser));
		putMethodHandlers.put("stopJob", new StopJobHandler(connManager, serializer, bodyParser));
		putMethodHandlers.put("finishAllJobs", new FinishAllJobsHandler(connManager, serializer, bodyParser));
		putMethodHandlers.put("updateDataSource", new UpdateDataSourceHandler(connManager, serializer, bodyParser));
		putMethodHandlers.put("reset", new ResetHandler(connManager, serializer, bodyParser));
		putMethodHandlers.put("startBatchJob", new StartBatchJobHandler(connManager, serializer, bodyParser));
		putMethodHandlers.put("uploadResource", new UploadResourceHandler(connManager, serializer, bodyParser));

		deleteMethodHandlers.put("removeDataSource", new RemoveDataSourceHandler(connManager, serializer, bodyParser));
		deleteMethodHandlers.put("removeDataSources", new RemoveDataSourcesHandler(connManager, serializer, bodyParser));
		deleteMethodHandlers.put("shutdown", new ShutdownHandler(connManager, serializer, bodyParser));
		deleteMethodHandlers.put("deleteBatches", new DeleteBatchesHandler(connManager, serializer, bodyParser));
		deleteMethodHandlers.put("removeHistory", new RemoveHistoryHandler(connManager, serializer, bodyParser));
		deleteMethodHandlers.put("deleteResource", new DeleteResourceHandler(connManager, serializer, bodyParser));
		deleteMethodHandlers.put("deleteOutputData", new DeleteOutputDataHandler(connManager, serializer, bodyParser));
	}

	@GET
	public Response handlerGet(@Context UriInfo ui) {
		MultivaluedMap<String, String> queryParams = ui.getQueryParameters();
		String method = queryParams.getFirst("m");
		GetMethodHandler handler = getMethodHandlers.get(method);
		if (handler != null) {
			return handler.handles(queryParams, null);
		} else { // FIXME: by whlee21
			return null;
		}
	}

	@POST
	public Response handlePost(String body, @Context HttpHeaders headers, @Context UriInfo ui) {
		MultivaluedMap<String, String> queryParams = ui.getQueryParameters();
		String method = queryParams.getFirst("m");
		PostMethodHandler handler = postMethodHandlers.get(method);
		if (handler != null) {
			return handler.handles(queryParams, headers, body);
		} else { // FIXME: by whlee21
			return null;
		}
	}

	@PUT
	public Response handlePut(String body, @Context HttpHeaders headers, @Context UriInfo ui) {
		MultivaluedMap<String, String> queryParams = ui.getQueryParameters();
		String method = queryParams.getFirst("m");
		PutMethodHandler handler = putMethodHandlers.get(method);
		if (handler != null) {
			return handler.handles(queryParams, headers, body);
		} else { // FIXME: by whlee21
			return null;
		}
	}

	@DELETE
	public Response handleDelete(String body, @Context HttpHeaders headers, @Context UriInfo ui) {
		MultivaluedMap<String, String> queryParams = ui.getQueryParameters();
		String method = queryParams.getFirst("m");
		DeleteMethodHandler handler = deleteMethodHandlers.get(method);
		if (handler != null) {
			return handler.handles(queryParams, headers);
		} else { // FIXME: by whlee21
			return null;
		}
	}
}
