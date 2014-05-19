package gaia.search.server.api.services;

import gaia.admin.collection.AdminScheduler;
import gaia.api.LweSolrServer;
import gaia.api.ObjectSerializer;
import gaia.crawl.ConnectorManager;
import gaia.search.server.api.services.parsers.RequestBodyParser;

import javax.ws.rs.DELETE;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.solr.core.CoreContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xnap.commons.i18n.I18nFactory;

public class CollectionIndexService extends BaseService {

	private static final Logger LOG = LoggerFactory.getLogger(CollectionIndexService.class);

	private static org.xnap.commons.i18n.I18n i18n = I18nFactory.getI18n(CollectionIndexService.class);

	public static final String KEY_PARAM = "key";
	private String collection;
	private AdminScheduler adminScheduler;
	private CoreContainer cores;
	private ConnectorManager crawlerManager;

	public CollectionIndexService(ObjectSerializer serializer, RequestBodyParser bodyParser, String collectionName,
			ConnectorManager crawlerManager, CoreContainer cores, AdminScheduler adminScheduler) {
		super(serializer, bodyParser);
		this.collection = collectionName;
		this.cores = cores;
		this.adminScheduler = adminScheduler;
		this.crawlerManager = crawlerManager;
	}

	@DELETE
	@Produces("text/plain")
	public Response deleteCollectionIndex(@Context HttpHeaders headers, @Context UriInfo ui,
			@QueryParam(KEY_PARAM) String keyParam) throws Exception {

		LOG.debug("key param: " + keyParam);

		if (!"iaccepttherisk".equals(keyParam)) {
			// setStatus(Response.Status.FORBIDDEN,
			// "you have not entered the correct query paramater: key=iaccepttherisk");
			return buildResponse(Response.Status.FORBIDDEN);
		}

		this.adminScheduler.stopAndRemoveAllSchedules(this.collection);

		LweSolrServer solrServer = new LweSolrServer(this.cores, this.collection);

		solrServer.deleteByQuery("*:*");
		solrServer.commit(false, true);

		this.crawlerManager.resetAll(null, this.collection);
		return buildResponse(Response.Status.NO_CONTENT);
	}
}
