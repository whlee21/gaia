package gaia.search.server.api.services;

import gaia.Constants;
import gaia.admin.collection.AdminScheduler;
import gaia.admin.collection.CollectionManager;
import gaia.admin.collection.SolrCmdHistory;
import gaia.api.APIUtils;
import gaia.api.AuditLogger;
import gaia.api.ClickLoggingContext;
import gaia.api.CollectionTemplatesServerResource;
import gaia.api.Error;
import gaia.api.ErrorUtils;
import gaia.api.ObjectSerializer;
import gaia.crawl.ConnectorManager;
import gaia.crawl.DataSourceManager;
import gaia.crawl.datasource.DataSourceId;
import gaia.jmx.JmxManager;
import gaia.search.server.api.services.parsers.BodyParseException;
import gaia.search.server.api.services.parsers.RequestBodyParser;
import gaia.search.server.configuration.Configuration;
import gaia.search.server.utils.StringUtils;
import gaia.utils.MasterConfUtil;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.io.FileUtils;
import org.apache.solr.client.solrj.SolrRequest;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.client.solrj.request.CoreAdminRequest;
import org.apache.solr.client.solrj.request.QueryRequest;
import org.apache.solr.cloud.ZkController;
import org.apache.solr.common.params.CollectionParams;
import org.apache.solr.common.params.CoreAdminParams;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.core.CoreContainer;
import org.apache.solr.core.SolrCore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xnap.commons.i18n.I18nFactory;

import com.google.inject.Inject;

@Path("/collections/")
public class CollectionService extends BaseService {
	private static org.xnap.commons.i18n.I18n i18n = I18nFactory.getI18n(CollectionService.class);
	private static final Logger LOG = LoggerFactory.getLogger(CollectionService.class);
	public static final String NAME = "name";
	public static final String INSTANCE_DIR = "instance_dir";
	public static final String TEMPLATE = "template";
	public static final String NUM_SHARDS = "num_shards";
	public static final String REPLICATION_FACTOR = "replication_factor";
	public static final String MAX_SHARDS_PER_NODE = "max_shards_per_node";
	public static final String DEFAULT_TEMPLATE = "default.zip";
	private CoreContainer cores;
	private CollectionManager cm;
	private AdminScheduler adminScheduler;
	private ConnectorManager crawlerManager;
	private JmxManager jmx;
	private Configuration configuration;
	private DataSourceManager dm;
	private SolrCmdHistory cmdHistory;
	private ClickLoggingContext clickLoggingContext;

	@Inject
	public CollectionService(ObjectSerializer serializer, RequestBodyParser bodyParser, CoreContainer cores,
			CollectionManager cm, AdminScheduler adminScheduler, ConnectorManager crawlerManager, JmxManager jmx,
			Configuration configuration, DataSourceManager dm, SolrCmdHistory cmdHistory, ClickLoggingContext clickLogCtx) {
		super(serializer, bodyParser);
		this.cores = cores;
		this.cm = cm;
		this.adminScheduler = adminScheduler;
		this.crawlerManager = crawlerManager;
		this.jmx = jmx;
		this.configuration = configuration;
		this.dm = dm;
		this.cmdHistory = cmdHistory;
		this.clickLoggingContext = clickLogCtx;
	}

	@GET
	@Produces("text/plain")
	public Response getCollections(@Context HttpHeaders headers, @Context UriInfo ui) throws WebApplicationException {
		List<gaia.admin.collection.Collection> collections = cm.getCollections();
		List<Map<String, Object>> list = new ArrayList<Map<String, Object>>(collections.size());
		for (gaia.admin.collection.Collection collection : collections) {
			Map<String, Object> attribs = new HashMap<String, Object>();
			attribs.put("name", collection.getName());
			attribs.put("instance_dir", collection.getInstanceDir());
			list.add(attribs);
		}

		return buildResponse(Response.Status.OK, list);
	}

	@GET
	@Path("{collectionName}")
	@Produces("text/plain")
	public Response getCollection(@Context HttpHeaders headers, @Context UriInfo ui,
			@PathParam("collectionName") String collectionName) {
		gaia.admin.collection.Collection coll = cm.getCollection(collectionName);

		Map<String, Object> attribs = getCollectionMap(coll);

		return buildResponse(Response.Status.OK, attribs);
	}

	static Map<String, Object> getCollectionMap(gaia.admin.collection.Collection coll) {
		Map<String, Object> attribs = new HashMap<String, Object>();
		attribs.put("name", coll.getName());
		attribs.put("instance_dir", coll.getInstanceDir());
		return attribs;
	}

	@POST
	@Produces("text/plain")
	public Response createCollection(String body, @Context HttpHeaders headers, @Context UriInfo ui) throws Exception {
		try {
			RequestBody requestBody = getRequestBody(body);
			return createCollection(requestBody.getProperties());
		} catch (BodyParseException e) {
			throw ErrorUtils.statusExp(Response.Status.INTERNAL_SERVER_ERROR,
					new Error("body", Error.E_EXCEPTION, i18n.tr("cannot parse body {0}", body)));
		}
	}

	private Response createCollection(Map<String, Object> m) throws Exception {
		if (m.size() == 0) {
			throw ErrorUtils.statusExp(422, i18n.tr("No input content found"));
		}

		if (cores.getCoreNames().size() == 0) {
			throw ErrorUtils.statusExp(Response.Status.INTERNAL_SERVER_ERROR, new Error("numSolrCores",
					Error.E_INVALID_OPERATION, i18n.tr("You cannot create a SolrCore unless one already exists")));
		}

		String name = (String) m.get("name");
		String inputInstanceDir = (String) m.get("instance_dir");
		String templateName = (String) m.get("template");

		CollectionService.validate(cm, cores, m, name);

		File solrHome = new File(cores.getSolrHome());

		String instanceDir = inputInstanceDir;
		if (inputInstanceDir == null) {
			String safeCoreFileName = gaia.utils.FileUtils.safeFileName(name);
			instanceDir = gaia.utils.FileUtils.uniqueFileName(new File(solrHome, "cores"), safeCoreFileName);
		}

		File instanceDirFile = new File(instanceDir);
		String corePath = instanceDir;
		if (!instanceDirFile.isAbsolute()) {
			instanceDirFile = new File(new File(solrHome, "cores"), instanceDir);
			corePath = "cores" + File.separator + instanceDir;
		}

		if ((inputInstanceDir == null) || (!instanceDirFile.exists())) {
			if (!instanceDirFile.mkdirs()) {
				throw ErrorUtils.statusExp(422, i18n.tr("Cannot create instanceDir: {0}", instanceDirFile.getAbsolutePath()));
			}

			gaia.utils.FileUtils.extract(pickTemplate(templateName), instanceDirFile);
		} else if ((null != templateName) && (instanceDirFile.exists())) {
			throw ErrorUtils.statusExp(
					422,
					new Error("template", Error.E_INVALID_VALUE, i18n
							.tr("Can not specify both an existing instanceDir and a template")));
		}

		if (cores.isZooKeeperAware()) {
			ZkController zkController = cores.getZkController();
			zkController.uploadConfigDir(new File(instanceDirFile, "conf"), name);
		}

		String dataHome = Constants.GAIA_DATA_HOME;
		if (dataHome == null) {
			throw ErrorUtils.statusExp(Response.Status.INTERNAL_SERVER_ERROR, new Error("data.dir", Error.E_MISSING_VALUE,
					i18n.tr("data.dir system property could not be found")));
		}
		String dataDir = "${data.dir}" + File.separator + "solr" + File.separator + "cores" + File.separator
				+ instanceDirFile.getName() + File.separator + "data";

		if (cores.isZooKeeperAware()) {
			Integer numShards = null;
			Object numShardsParam = m.get("num_shards");
			if (numShardsParam != null) {
				numShards = Integer.valueOf(Integer.parseInt(numShardsParam.toString()));
			}
			Integer replicationFactor = null;
			Object replicationFactorParam = m.get("replication_factor");
			if (replicationFactorParam != null) {
				replicationFactor = Integer.valueOf(Integer.parseInt(replicationFactorParam.toString()));
			}
			Integer maxShardsPerNode = null;
			Object maxShardsPerNodeParam = m.get("max_shards_per_node");
			if (maxShardsPerNodeParam != null) {
				maxShardsPerNode = Integer.valueOf(Integer.parseInt(maxShardsPerNodeParam.toString()));
			}
			createCollection(name, corePath, dataDir, numShards, replicationFactor, maxShardsPerNode);

			SolrCore core = cores.getCore(name);
			try {
				if (core != null) {
					String defaultInstanceDir = instanceDirFile.getParentFile().getParent();
					File newInstanceDirFile = new File(defaultInstanceDir, core.getName());
					org.apache.commons.io.FileUtils.copyDirectory(instanceDirFile, newInstanceDirFile);
					org.apache.commons.io.FileUtils.deleteDirectory(instanceDirFile);
				}
			} catch (IOException e) {
			} finally {
				if (core != null)
					core.close();
			}
		} else {
			createCore(name, corePath, dataDir);
		}

		gaia.admin.collection.Collection coll = new gaia.admin.collection.Collection(name);
		coll.setInstanceDir(instanceDir);
		cm.addCollection(coll);

		Map<String, Object> attribs = CollectionService.getCollectionMap(coll);

		URI seeOther = configuration.getCollectionUri(URLEncoder.encode(name, "UTF-8"));
		Response response = buildResponse(Response.Status.CREATED, seeOther, attribs);

		AuditLogger.log(i18n.tr("created new collection: {0}", name));

		return response;
	}

	static void validate(CollectionManager cm, CoreContainer cores, Map<String, Object> m, String name) {
		List<Error> errors = new ArrayList<Error>();
		if ((name == null) || (name.trim().length() == 0)) {
			errors.add(new Error("name", Error.E_MISSING_VALUE, i18n.tr("name is a required key")));
		}

		if (name != null) {
			Matcher matcher = APIUtils.ALPHANUM.matcher(name);
			if (!matcher.matches()) {
				errors.add(new Error("name", Error.E_INVALID_VALUE, i18n.tr("name must consist of only A-Z a-z 0-9 - _")));
			}
		}

		String template = (String) m.get("template");
		if ((template != null) && (template.trim().length() == 0)) {
			errors.add(new Error("template", Error.E_INVALID_VALUE, i18n.tr("template cannot be empty")));
		}

		String instanceDir = (String) m.get("instance_dir");
		if ((instanceDir != null) && (instanceDir.trim().length() == 0)) {
			errors.add(new Error("instance_dir", Error.E_INVALID_VALUE, i18n.tr("instance_dir cannot be empty")));
		}
		String path = getPath(instanceDir);
		if (path != null) {
			List<gaia.admin.collection.Collection> collections = cm.getCollections();
			for (gaia.admin.collection.Collection collection : collections) {
				if (getPath(collection.getInstanceDir()).equals(path)) {
					errors.add(new Error("instance_dir", Error.E_EXISTS, i18n.tr(
							"this instance_dir is already used by a collection - {0}", instanceDir)));
				}
			}
		}

		if (instanceDir != null) {
			File instanceDirFile = new File(instanceDir);
			if ((instanceDirFile.exists()) && (instanceDirFile.isFile())) {
				errors.add(new Error("instance_dir", Error.E_INVALID_VALUE, i18n
						.tr("instance_dir appears to point to an existing file rather than directory")));
			} else if (instanceDirFile.exists()) {
				File solrConfig = new File(instanceDir + File.separator + "conf", "solrconfig.xml");
				if (!solrConfig.exists()) {
					errors.add(new Error("instance_dir", Error.E_INVALID_VALUE, i18n
							.tr("instance_dir appears to point to an invalid collection")));
				}
			}
		}

		if (cores.isZooKeeperAware()) {
			Object numShardsParam = m.get("num_shards");
			if (numShardsParam == null)
				errors.add(new Error("num_shards", Error.E_MISSING_VALUE, i18n
						.tr("num_shards is a required parameter in SolrCloud mode")));
			else {
				try {
					int val = Integer.parseInt(numShardsParam.toString());
					if (val < 1)
						errors.add(new Error("num_shards", Error.E_INVALID_VALUE, i18n.tr("num_shards is invalid")));
				} catch (NumberFormatException e) {
					errors
							.add(new Error("num_shards", Error.E_INVALID_VALUE, i18n.tr("num_shards is invalid: {0}", e.toString())));
				}
			}

			Object replicationFactorParam = m.get("replication_factor");
			if (replicationFactorParam != null) {
				try {
					int val = Integer.parseInt(replicationFactorParam.toString());
					if (val < 1)
						errors
								.add(new Error("replication_factor", Error.E_INVALID_VALUE, i18n.tr("replication_factor is invalid")));
				} catch (NumberFormatException e) {
					errors.add(new Error("replication_factor", Error.E_INVALID_VALUE, i18n.tr(
							"replication_factor is invalid: {0}", e.toString())));
				}
			}

			Object maxShardsPerNodeParam = m.get("max_shards_per_node");
			if (maxShardsPerNodeParam != null) {
				try {
					int val = Integer.parseInt(maxShardsPerNodeParam.toString());
					if (val < 1)
						errors.add(new Error("max_shards_per_node", Error.E_INVALID_VALUE, i18n
								.tr("max_shards_per_node is invalid")));
				} catch (NumberFormatException e) {
					errors.add(new Error("max_shards_per_node", Error.E_INVALID_VALUE, i18n
							.tr("max_shards_per_node is invalid: {0}" + e.toString())));
				}
			}
		}

		Map<String, Object> paramCopy = new HashMap<String, Object>(m.size());
		paramCopy.putAll(m);
		paramCopy.remove("name");
		paramCopy.remove("instance_dir");
		paramCopy.remove("template");
		paramCopy.remove("num_shards");
		paramCopy.remove("replication_factor");
		paramCopy.remove("max_shards_per_node");
		if (paramCopy.size() > 0) {
			errors.add(new Error(m.keySet().toString(), Error.E_FORBIDDEN_KEY, i18n.tr(
					"Unknown or dissallowed key found: {0}", m.keySet())));
		}

		if (errors.size() > 0) {
			throw ErrorUtils.statusExp(422, errors);
		}

		if (APIUtils.coreExists(cores, name))
			throw ErrorUtils.statusExp(Response.Status.CONFLICT,
					new Error("name", Error.E_FORBIDDEN_VALUE, i18n.tr("collection already exists")));
	}

	private static String getPath(String instanceDir) {
		if (instanceDir == null) {
			return null;
		}
		File instanceDirFile = new File(instanceDir);
		String path;
		if (!instanceDirFile.isAbsolute())
			path = Constants.GAIA_CONF_HOME + File.separator + "solr" + File.separator + "cores" + File.separator
					+ instanceDir;
		else {
			path = instanceDir;
		}
		return new File(path).getAbsolutePath();
	}

	private void createCollection(String name, String instanceDir, String dataDir, Integer numShards,
			Integer replicationFactor, Integer maxShardsPerNode) throws IOException {
		URL url = configuration.getSolrAddress(true, name);

		SolrServer server = new HttpSolrServer(url.toExternalForm());
		try {
			ModifiableSolrParams params = new ModifiableSolrParams();

			params.set("action", new String[] { CollectionParams.CollectionAction.CREATE.toString() });
			params.set("name", new String[] { name });
			params.set("numShards", numShards.intValue());
			int repFactor;
			if (replicationFactor != null) {
				repFactor = replicationFactor.intValue();
				params.set("replicationFactor", replicationFactor.intValue());
			} else {
				Set<String> nodes = cores.getZkController().getClusterState().getLiveNodes();

				repFactor = nodes.size() / numShards.intValue();
				if (repFactor == 0) {
					repFactor = 1;
				}
				params.set("replicationFactor", repFactor);
			}
			if (maxShardsPerNode != null) {
				params.set("maxShardsPerNode", maxShardsPerNode.intValue());
			}

			int numReplicas = numShards.intValue() * repFactor;
			String currentNode = cores.getZkController().getNodeName();
			Set<String> nodes = cores.getZkController().getClusterState().getLiveNodes();
			if (nodes.size() > numReplicas) {
				HashSet<String> selectedNodes = new HashSet<String>();
				selectedNodes.addAll(nodes);
				selectedNodes.remove(currentNode);
				List<String> nodeList = new ArrayList<String>(selectedNodes.size());
				nodeList.addAll(selectedNodes);
				Collections.shuffle(nodeList);
				List<String> selectedNodesList = nodeList.subList(0, numReplicas - 1);
				selectedNodesList.add(0, currentNode);
				params.set("createNodeSet", new String[] { StringUtils.join(",", selectedNodesList) });
			}

			SolrRequest request = new QueryRequest(params);
			request.setPath("/admin/collections");
			server.request(request);
		} catch (SolrServerException e) {
			LOG.warn(i18n.tr("Could not create SolrCloud collection : {0}", e));
			throw ErrorUtils.statusExp(Response.Status.INTERNAL_SERVER_ERROR,
					i18n.tr("Could not create SolrCloud collection : {0}", e.getMessage()));
		} finally {
			server.shutdown();
		}
	}

	private void createCore(String name, String instanceDir, String dataDir) throws IOException {
		URL url = configuration.getSolrAddress(true, null);

		SolrServer server = new HttpSolrServer(url.toExternalForm());
		try {
			CoreAdminRequest.Create req = new CoreAdminRequest.Create();
			req.setCoreName(name);
			req.setInstanceDir(instanceDir);
			req.setDataDir(dataDir);
			req.process(server);
		} catch (SolrServerException e) {
			LOG.warn(i18n.tr("Could not create Solr core"), e);
			throw ErrorUtils.statusExp(Response.Status.INTERNAL_SERVER_ERROR,
					i18n.tr("Could not create Solr core : {0}", e.getMessage()));
		} finally {
			server.shutdown();
		}
	}

	private File pickTemplate(Object template) {
		String templateName = null != template ? template.toString() : DEFAULT_TEMPLATE;

		File zip = null;

		if (null != CollectionTemplatesServerResource.CONF_TEMPLATES_DIR) {
			zip = new File(CollectionTemplatesServerResource.CONF_TEMPLATES_DIR, templateName);
		}

		if (((null == zip) || (!zip.exists())) && (null != CollectionTemplatesServerResource.APP_TEMPLATES_DIR)) {
			zip = new File(CollectionTemplatesServerResource.APP_TEMPLATES_DIR, templateName);
		}

		if ((null == zip) && (null == template)) {
			throw ErrorUtils.statusExp(Response.Status.INTERNAL_SERVER_ERROR,
					new Error("template", Error.E_NOT_FOUND, i18n.tr("default template could not be found: {0}", templateName)));
		}

		if ((null == zip) || (!zip.exists())) {
			throw ErrorUtils.statusExp(422,
					new Error("template", Error.E_NOT_FOUND, i18n.tr("template could not be found: {0}", templateName)));
		}

		if (!zip.canRead()) {
			throw ErrorUtils.statusExp(Response.Status.INTERNAL_SERVER_ERROR, new Error("template", Error.E_FORBIDDEN_VALUE,
					i18n.tr("template could not be read: {0}", zip.getAbsolutePath())));
		}

		return zip;
	}

	@DELETE
	@Path("{collectionName}")
	@Produces("text/plain")
	public Response deleteCollection(@Context HttpHeaders headers, @Context UriInfo ui,
			@PathParam("collectionName") String collectionName, @QueryParam("force") String forceParam) throws Exception {
		// if (!isExisting()) {
		// return;
		// }

		if (cores.getCoreNames().size() == 1) {
			throw ErrorUtils.statusExp(422,
					i18n.tr("At least one SolrCore must always exist, so you cannot remove: {0}", collectionName));
		}

		List<Error> errors = new ArrayList<Error>();

		LOG.info(i18n.tr("Stopping all scheduled jobs for collection"));
		try {
			adminScheduler.stopAndRemoveAllSchedules(collectionName);
		} catch (Exception e) {
			errors.add(new Error(collectionName, Error.E_EXCEPTION, i18n.tr("Unable to Clear All Schedules")));
		}

		LOG.info(i18n.tr("Done stopping all scheduled jobs for collection"));
		// Parameter p = (Parameter) getQuery().getFirst("force");
		boolean force = forceParam != null ? forceParam.toLowerCase().equals("true") : false;
		try {
			crawlerManager.setClosing(null, collectionName, true);
			if (force) {
				crawlerManager.finishAllJobs(null, collectionName, true, 60000L);
				crawlerManager.close(null, collectionName, false);
				crawlerManager.resetAll(null, collectionName);
			} else {
				crawlerManager.close(null, collectionName, true);
				crawlerManager.resetAll(null, collectionName);
				crawlerManager.close(null, collectionName, false);
			}

			List<DataSourceId> ids = crawlerManager.listDataSources(collectionName);
			for (DataSourceId dsId : ids) {
				jmx.unregisterDataSourceMBean(dsId);
			}

			crawlerManager.removeDataSources(collectionName, true);
			crawlerManager.setClosing(null, collectionName, false);
		} catch (Throwable e) {
			crawlerManager.setClosing(null, collectionName, false);
			LOG.warn(i18n.tr("Exception removing collection {0}: {1}", collectionName, e.toString()));
			errors.add(new Error(collectionName, Error.E_EXCEPTION, e.toString()));
		}

		if (errors.size() > 0) {
			throw ErrorUtils.statusExp(422, errors);
		}
		LOG.info(i18n.tr("No jobs are running for collection {0}", collectionName));
		try {
			gaia.admin.collection.Collection coll = cm.getCollection(collectionName);
			cm.removeCollection(coll);
			try {
				FileUtils.deleteDirectory(new File(Constants.GAIA_DATA_HOME + File.separator + "solr" + File.separator
						+ "cores" + File.separator + coll.getInstanceDir()));
			} catch (IOException e) {
				if (e.getMessage().contains("Unable to delete file: "))
					LOG.warn(i18n.tr("Some files in collection '{0}' data dir could not be removed: {1}", collectionName,
							e.toString()));
				else {
					errors.add(new Error(collectionName, Error.E_EXCEPTION, i18n.tr(
							"Unable to Delete Collection 'data' dir: {0}", e.toString())));
				}
			}

			if (cores.isZooKeeperAware())
				removeCollection(collectionName);
			else
				removeCore(collectionName);
		} catch (Throwable e) {
			LOG.error(i18n.tr("Unable to Delete Collection Data: {0}", e));
			errors.add(new Error(collectionName, Error.E_EXCEPTION, i18n.tr("Unable to Delete Collection Data: {0}"
					+ e.toString())));
		}

		if (!errors.isEmpty()) {
			throw ErrorUtils.statusExp(Response.Status.INTERNAL_SERVER_ERROR, errors);
		}

		AuditLogger.log(i18n.tr("removed collection: {0}", collectionName));
		return buildResponse(Response.Status.NO_CONTENT);
	}

	private void removeCollection(String name) throws IOException, SolrServerException {
		URL url = MasterConfUtil.getSolrAddress(true, null);

		LOG.debug("whlee21 removeCollection solrUrl = " + url);

		SolrServer server = new HttpSolrServer(url.toExternalForm());
		try {
			ModifiableSolrParams params = new ModifiableSolrParams();
			params.set("action", new String[] { CollectionParams.CollectionAction.DELETE.toString() });
			params.set("name", new String[] { name });
			SolrRequest request = new QueryRequest(params);
			request.setPath("/admin/collections");
			server.request(request);
		} finally {
			server.shutdown();
		}
	}

	private void removeCore(String name) throws SolrServerException, IOException {
		URL url = configuration.getSolrAddress(true, null);

		LOG.debug("whlee21 removeCollection solrUrl = " + url);

		SolrServer server = new HttpSolrServer(url.toExternalForm());
		try {
			ModifiableSolrParams params = new ModifiableSolrParams();
			params.set("action", new String[] { CoreAdminParams.CoreAdminAction.UNLOAD.toString() });
			params.set("core", new String[] { name });
			params.set("deleteDataDir", true);
			params.set("deleteIndex", true);
			params.set("deleteInstanceDir", true);
			SolrRequest request = new QueryRequest(params);
			request.setPath("/admin/cores");
			server.request(request);
		} finally {
			server.shutdown();
		}
	}

	@Path("{collectionName}/info")
	public CollectionInfoService getCollectionInfoHandler(@PathParam("collectionName") String collectionName) {
		return new CollectionInfoService(serializer, bodyParser, collectionName, cm, cores);
	}

	@Path("{collectionName}/index")
	public CollectionIndexService getCollectionIndexHandler(@PathParam("collectionName") String collectionName) {
		return new CollectionIndexService(serializer, bodyParser, collectionName, crawlerManager, cores, adminScheduler);
	}

	@Path("{collectionName}/activities")
	public ActivityService getActivityHandler(@PathParam("collectionName") String collectionName) {
		return new ActivityService(serializer, bodyParser, collectionName, cm, cores, cmdHistory, adminScheduler,
				this.configuration);
	}

	@Path("{collectionName}/datasources")
	public DataSourceService getDataSourceHandler(@PathParam("collectionName") String collectionName) {
		return new DataSourceService(serializer, bodyParser, collectionName, crawlerManager, jmx, cm, cores, dm,
				configuration, adminScheduler);
	}

	@Path("{collectionName}/batches")
	public BatchService getBatchHandler(@PathParam("collectionName") String collectionName) {
		return new BatchService(serializer, bodyParser, collectionName, crawlerManager);
	}

	@Path("{collectionName}/jdbcdrivers")
	public JdbcDriverService getJdbcDriverHandler(@PathParam("collectionName") String collectionName) {
		return new JdbcDriverService(serializer, bodyParser, collectionName, crawlerManager);
	}

	@Path("{collectionName}/fieldtypes")
	public FieldTypeService getFieldTypeHandler(@PathParam("collectionName") String collectionName) {
		return new FieldTypeService(serializer, bodyParser, collectionName, this.cm, this.cores, this.configuration);
	}

	@Path("{collectionName}/fields")
	public FieldService getFieldHandler(@PathParam("collectionName") String collectionName) {
		return new FieldService(serializer, bodyParser, collectionName, this.cm, this.cores, this.configuration);
	}

	@Path("{collectionName}/dynamicfields")
	public DynamicFieldService getDynamicFieldHandler(@PathParam("collectionName") String collectionName) {
		return new DynamicFieldService(serializer, bodyParser, collectionName, this.cm, this.cores, this.configuration);
	}

	@Path("{collectionName}/filtering")
	public FilteringService getFilteringHandler(@PathParam("collectionName") String collectionName) {
		return new FilteringService(serializer, bodyParser, collectionName, this.cm, this.cores, this.configuration);
	}

	@Path("{collectionName}/components")
	public ComponentService getComponentHandler(@PathParam("collectionName") String collectionName) {
		return new ComponentService(serializer, bodyParser, collectionName, this.cm, this.cores, this.configuration);
	}

	@Path("{collectionName}/settings")
	public SettingService getSettingHandler(@PathParam("collectionName") String collectionName) {
		return new SettingService(serializer, bodyParser, collectionName, this.cm, this.cores, this.configuration);
	}

	@Path("{collectionName}/caches")
	public CacheService getCacheHandler(@PathParam("collectionName") String collectionName) {
		return new CacheService(serializer, bodyParser, collectionName, this.cm, this.cores, this.configuration);
	}

	@Path("{collectionName}/click")
	public ClickService getClickHandler(@PathParam("collectionName") String collectionName) {
		return new ClickService(serializer, bodyParser, collectionName, clickLoggingContext);
	}

	@Path("{collectionName}/roles")
	public RoleService getRoleHandler(@PathParam("collectionName") String collectionName) {
		return new RoleService(serializer, bodyParser, collectionName);
	}
}
