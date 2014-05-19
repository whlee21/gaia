package gaia.search.server.api.services;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import gaia.admin.collection.CollectionManager;
import gaia.admin.editor.EditableSolrConfig;
import gaia.api.ErrorUtils;
import gaia.api.ObjectSerializer;
import gaia.search.server.api.services.parsers.RequestBodyParser;
import gaia.search.server.configuration.Configuration;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.solr.core.CoreContainer;
import org.apache.solr.core.SolrCore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xnap.commons.i18n.I18nFactory;

public class ComponentService  extends BaseService {
	
	private static org.xnap.commons.i18n.I18n i18n = I18nFactory.getI18n(ComponentService.class);
	private static final Logger LOG = LoggerFactory.getLogger(ComponentService.class);
	private static final Object INSTANCE_NAME = "name";
	private final CollectionManager cm;
	private String collection;
	private final CoreContainer cores;
	private SolrCore core;
	private volatile boolean existing = true;
	private Configuration configuration;
	
	private String listName;
	private String listKey;
	private String handlerName;
	private static final Map<String, String> INSTANCE_ALIASES;// =

	static {
		Map<String, String> tmp = new HashMap<String, String>();
		tmp.put("all", "components");
		tmp.put("first", "first-components");
		tmp.put("last", "last-components");
		INSTANCE_ALIASES = Collections.unmodifiableMap(tmp);
	}
	
	public ComponentService(ObjectSerializer serializer, RequestBodyParser bodyParser, String collectionName, 
			CollectionManager cm, CoreContainer cores, Configuration configuration) {
		super(serializer, bodyParser);
		this.collection = collectionName;
		this.cm = cm;
		this.cores = cores;
		core = cores.getCore(collection);
		setExisting(core != null);
	}

	public void setExisting(boolean exists) {
		this.existing = exists;
	}
	
	public boolean isExisting() {
		return this.existing;
	}
	
	
	@GET
	@Path("{listName}")
	@Produces("text/plain")
	public Response getComponent(@Context HttpHeaders headers, @Context UriInfo ui,
			@PathParam("listName") String listName) {
		LOG.debug("hhokyung getComponent (collectionName, listName) = (" + collection + ", " + listName + ")");
//		return handleRequest(headers, null, ui, Request.Type.GET, createComponentResource(collectionName, listName));
		
		if (!isExisting()) {
			throw ErrorUtils.statusExp(Response.Status.NOT_FOUND, i18n.tr("URI not found"));
		}
		EditableSolrConfig ecc = new EditableSolrConfig(core, cm.getUpdateChain(), cores.getZkController());

		if (ecc.getRequestHandlerNode(handlerName) == null) {
			throw ErrorUtils.statusExp(Response.Status.NOT_FOUND, i18n.tr("Search components configuration does not exist: "
					+ listName));
		}

		String[] list = ecc.getArrayFromRequestHandler(handlerName, listKey);

		return buildResponse(Response.Status.OK, list);
	}

	@PUT
	@Path("{listName}")
	@Produces("text/plain")
	public Response updateComponent(String body, @Context HttpHeaders headers, @Context UriInfo ui,
			@PathParam("listName") String listName) {
//		return handleRequest(headers, body, ui, Request.Type.PUT, createComponentResource(collectionName, listName));
		return null;
	}

	@DELETE
	@Path("{listName}")
	@Produces("text/plain")
	public Response deleteComponent(@Context HttpHeaders headers, @Context UriInfo ui,
			@PathParam("listName") String listName) {
//		return handleRequest(headers, null, ui, Request.Type.DELETE, createComponentResource(collectionName, listName));
		return null;
	}
}