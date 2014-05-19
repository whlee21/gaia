package gaia.search.server.api.services;

import gaia.admin.collection.CollectionManager;
import gaia.admin.editor.ComponentInUseException;
import gaia.admin.editor.EditableSolrConfig;
import gaia.api.APIUtils;
import gaia.api.Error;
import gaia.api.ErrorUtils;
import gaia.api.ObjectSerializer;
import gaia.search.server.api.services.parsers.BodyParseException;
import gaia.search.server.api.services.parsers.RequestBodyParser;
import gaia.search.server.configuration.Configuration;
import gaia.security.ad.ADACLTagProvider;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;

import org.apache.solr.core.CoreContainer;
import org.apache.solr.core.SolrCore;
import org.quartz.SchedulerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.DOMException;
import org.xml.sax.SAXException;
import org.xnap.commons.i18n.I18nFactory;

public class FilteringService  extends BaseService {

	private static org.xnap.commons.i18n.I18n i18n = I18nFactory.getI18n(FilteringService.class);
	private static final Logger LOG = LoggerFactory.getLogger(FilteringService.class);
	
	private static final Object INSTANCE_NAME = "name";
	public static final String CONFIG_GET_LIST = "config_get_list";
	private final CollectionManager cm;
	private String collection;
	private final CoreContainer cores;
	private SolrCore core;
	private Configuration configuration;
	private volatile boolean existing = true;

	public FilteringService(ObjectSerializer serializer, RequestBodyParser bodyParser, String collectionName, 
				CollectionManager cm, CoreContainer cores, Configuration configuration) {
		super(serializer, bodyParser);
		this.collection = collectionName;
		this.cm = cm;
		this.cores = cores;
		this.configuration = configuration;
		
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
	@Produces("text/plain")
	public Response getFilterings(@Context HttpHeaders headers, @Context UriInfo ui) {
		LOG.debug("hhokyung getFilterings (collectionName) = (" + collection+ ")");
		if (!isExisting()) {
			throw ErrorUtils.statusExp(Response.Status.NOT_FOUND, i18n.tr("URI not found"));
		}
		EditableSolrConfig ecc = new EditableSolrConfig(core, cm.getUpdateChain(), cores.getZkController());

		LOG.debug("hhokyung getFilterings2 (collectionName) = (" + collection+ ")");
		List<String> aclNames = ecc.getACLComponentNames();
		Map<String, Object> configs = new HashMap<String, Object>();
		for (String name : aclNames) {
			configs.put(name, ecc.getACLComponentConfig(name));
		}
		LOG.debug("hhokyung getFilterings3 (collectionName) = (" + collection+ ")");
		return buildResponse(Response.Status.OK, configs); 
	}

	@GET
	@Path("{instanceName}")
	@Produces("text/plain")
	public Response getFiltering(@Context HttpHeaders headers, @Context UriInfo ui,
			@PathParam("instanceName") String instanceName) {
		LOG.debug("hhokyung getFiltering (collectionName, instanceName) = (" + collection+ ", " + instanceName + ")");

		if (!isExisting()) {
			throw ErrorUtils.statusExp(Response.Status.NOT_FOUND, i18n.tr("URI not found"));
		}
		EditableSolrConfig ecc = new EditableSolrConfig(core, cm.getUpdateChain(), cores.getZkController());

		Map<String, Object> config = ecc.getACLComponentConfig(instanceName);
		if (config == null) {
			throw ErrorUtils.statusExp(Response.Status.NOT_FOUND, i18n.tr("Configuration does not exist: " + instanceName));
		}

		return buildResponse(Response.Status.OK, config); 
	}

	
	public Response createFiltering(String instanceName, Map<String, Object> m) throws DOMException, XPathExpressionException, IOException,
	ParserConfigurationException, SAXException, SchedulerException, URISyntaxException {
		EditableSolrConfig ecc = new EditableSolrConfig(core, cm.getUpdateChain(), cores.getZkController());
		
		Map<String, Object> config = ecc.getACLComponentConfig(instanceName);
		if (config != null) {
			throw ErrorUtils.statusExp(Response.Status.CONFLICT, i18n.tr("configuration already exists: " + instanceName));
		}
		
		validate(instanceName, m);
		ecc.setACLComponentConfig(instanceName, m);
		ecc.save();
		APIUtils.reloadCore(collection, cores);
		
	
		URI seeOther = configuration.getCollectionUri("filtering/" + URLEncoder.encode(instanceName, "UTF-8"));
		Response response = buildResponse(Response.Status.CREATED, seeOther, config);
		
		return response;
	}
			
	@POST
	@Path("{instanceName}")
	@Produces("text/plain")
	public Response createFiltering(String body, @Context HttpHeaders headers, @Context UriInfo ui,
			@PathParam("instanceName") String instanceName) throws DOMException, XPathExpressionException, IOException, 
			ParserConfigurationException, SAXException, SchedulerException, URISyntaxException {
		LOG.debug("hhokyung createFiltering (collectionName, instanceName) = (" + collection+ ", "+instanceName+")");
//		return handleRequest(headers, body, ui, Request.Type.POST, createFilteringResource(collectionName, null));
		try {
			RequestBody requestBody = getRequestBody(body);
			return createFiltering(instanceName, requestBody.getProperties());
		} catch (BodyParseException e) {
			throw ErrorUtils.statusExp(Response.Status.INTERNAL_SERVER_ERROR, new Error("body", Error.E_EXCEPTION,
					i18n.tr("cannot parse body " + body)));
		}
		
		
	}

	@POST
	@Produces("text/plain")
	public Response createFiltering(String body, @Context HttpHeaders headers, @Context UriInfo ui) throws DOMException, XPathExpressionException, IOException, 
			ParserConfigurationException, SAXException, SchedulerException, URISyntaxException {
		LOG.debug("hhokyung createFiltering2 (collectionName, instanceName) = (" + collection+")");
//		return handleRequest(headers, body, ui, Request.Type.POST, createFilteringResource(collectionName, null));
		try {
			RequestBody requestBody = getRequestBody(body);
			return null;
		} catch (BodyParseException e) {
			throw ErrorUtils.statusExp(Response.Status.INTERNAL_SERVER_ERROR, new Error("body", Error.E_EXCEPTION,
					i18n.tr("cannot parse body " + body)));
		}
		
		
	}
	
	public Response updateFiltering(String instanceName, Map<String, Object> m) throws DOMException, XPathExpressionException, IOException,
	ParserConfigurationException, SAXException, SchedulerException {
		validate(instanceName, m);
		EditableSolrConfig ecc = new EditableSolrConfig(core, cm.getUpdateChain(), cores.getZkController());
		
		Map<String, Object> config = ecc.getACLComponentConfig(instanceName);
		if (config == null) {
			throw ErrorUtils.statusExp(Response.Status.NOT_FOUND, i18n.tr("Configuration not found: " + instanceName));
		}
		
		ecc.setACLComponentConfig(instanceName, m);
		ecc.save();
		APIUtils.reloadCore(collection, cores);
		return buildResponse(Response.Status.NO_CONTENT);
	}
			
	@PUT
	@Path("{instanceName}")
	@Produces("text/plain")
	public Response updateFiltering(String body, @Context HttpHeaders headers, @Context UriInfo ui,
			@PathParam("instanceName") String instanceName) throws DOMException, XPathExpressionException, IOException, 
			ParserConfigurationException, SAXException, SchedulerException {
		LOG.debug("hhokyung updateFiltering (collectionName, instanceName) = (" + collection+", "+instanceName+")");
		try {
			RequestBody requestBody = getRequestBody(body);
			return updateFiltering(instanceName, requestBody.getProperties());
		} catch (BodyParseException e) {
			throw ErrorUtils.statusExp(Response.Status.INTERNAL_SERVER_ERROR, new Error("body", Error.E_EXCEPTION,
					i18n.tr("cannot parse body " + body)));
		}
	}

	@DELETE
	@Path("{instanceName}")
	@Produces("text/plain")
	public Response deleteFiltering(@Context HttpHeaders headers, @Context UriInfo ui,
			@PathParam("instanceName") String instanceName) throws IOException, ParserConfigurationException, SAXException {

		LOG.debug("hhokyung deleteFiltering (collectionName, instanceName) = (" + collection+", "+instanceName+")");
		if (!isExisting()) {
			throw ErrorUtils.statusExp(Response.Status.NOT_FOUND, i18n.tr("URI not found"));
		}
		EditableSolrConfig ecc = new EditableSolrConfig(core, cm.getUpdateChain(), cores.getZkController());

		Map<String, Object> config = ecc.getACLComponentConfig(instanceName);
		if (config == null) {
			throw ErrorUtils.statusExp(Response.Status.NOT_FOUND, i18n.tr("Configuration does not exist: " + instanceName));
		}

		try {
			ecc.deleteACLComponentConfig(instanceName);
			ecc.save();
			APIUtils.reloadCore(collection, cores);
		} catch (ComponentInUseException e) {
			// setStatus(Response.Status.CONFLICT,
			// "Cannot delete a config that is currently in use.");
		}
		return buildResponse(Response.Status.NO_CONTENT);
	}

	private void validate(String instanceName, Map<String, Object> m) {
		if (!isExisting()) {
			throw ErrorUtils.statusExp(Response.Status.NOT_FOUND, i18n.tr("URI not found"));
		}

		if (instanceName == null) {
			throw ErrorUtils.statusExp(417, i18n.tr("Name not specified"));
		}

		if (m.size() == 0) {
			throw ErrorUtils.statusExp(422, i18n.tr("No input content found"));
		}

		Map<String, Object> config = (Map) m.get("provider.config");

		String providerClass = m.get("provider.class").toString();

		if (config != null) {
			Object url = config.get("java.naming.provider.url");

			if (ADACLTagProvider.class.getName().equals(providerClass)) {
				if ((url == null) || (url.toString().trim().length() == 0)) {
					throw ErrorUtils.statusExp(Response.Status.BAD_REQUEST, new Error("java.naming.provider.url",
							i18n.tr("java.naming.provider.url.no_url", "URL must be specified")));
				}

				try {
					URI u = new URI(url.toString());
					if ((!"ldap".equals(u.getScheme())) && (!"ldaps".equals(u.getScheme()))) {
						throw ErrorUtils.statusExp(Response.Status.BAD_REQUEST, new Error("java.naming.provider.url",
								i18n.tr("java.naming.provider.url.non_ldap_ldaps_url", "protocol must be ldap or ldaps: '" + url + "'")));
					}

				} catch (URISyntaxException e) {
					throw ErrorUtils.statusExp(Response.Status.BAD_REQUEST, new Error("java.naming.provider.url",
							i18n.tr("java.naming.provider.url.illegal:url", "Value is not legal url: '" + url + "' " + e.getMessage())));
				}

				Object principal = config.get("java.naming.security.principal");
				if (principal == null) {
					throw ErrorUtils.statusExp(Response.Status.BAD_REQUEST, new Error("java.naming.security.principal",
							i18n.tr("java.naming.security.principal.no_value", "Principal must be specified")));
				}

				if (!principal.toString().contains("@")) {
					throw ErrorUtils.statusExp(Response.Status.BAD_REQUEST,
							new Error("java.naming.security.principal", "java.naming.security.principal.invalid_value",
									i18n.tr("Illegal username, no '@' found in: " + principal.toString())));
				}

				Object credentials = config.get("java.naming.security.credentials");
				if (credentials == null)
					throw ErrorUtils.statusExp(Response.Status.BAD_REQUEST, new Error("java.naming.security.credentials",
							i18n.tr("java.naming.security.credentials.no_value", "Credentials must be specified")));
			}
		}
	}
}
