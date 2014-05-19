package gaia.api;

import gaia.Constants;
import gaia.admin.collection.CollectionManager;
import gaia.utils.MasterConfUtil;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.core.Response;

import org.apache.hadoop.util.StringUtils;
import org.apache.solr.client.solrj.SolrRequest;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.client.solrj.request.CoreAdminRequest;
import org.apache.solr.client.solrj.request.QueryRequest;
import org.apache.solr.cloud.ZkController;
import org.apache.solr.common.params.CollectionParams;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.core.CoreContainer;
import org.apache.solr.core.SolrCore;
import org.restlet.data.Status;
import org.restlet.resource.Get;
import org.restlet.resource.Post;
import org.restlet.resource.ServerResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;

public class CollectionsServerResource extends ServerResource implements CollectionsResource {
	private static transient Logger LOG = LoggerFactory.getLogger(CollectionsServerResource.class);

	private static String DEFAULT_TEMPLATE = "default.zip";
	private CoreContainer cores;
	private CollectionManager cm;

	@Inject
	public CollectionsServerResource(CoreContainer cores, CollectionManager cm) {
		this.cores = cores;
		this.cm = cm;
	}

	@Get("json")
	public List<Map<String, Object>> retrieve() throws IOException, SolrServerException {
		List<gaia.admin.collection.Collection> collections = cm.getCollections();
		List<Map<String, Object>> list = new ArrayList<Map<String, Object>>(collections.size());
		for (gaia.admin.collection.Collection collection : collections) {
			Map<String, Object> attribs = new HashMap<String, Object>();
			attribs.put("name", collection.getName());
			attribs.put("instance_dir", collection.getInstanceDir());
			list.add(attribs);
		}

		return list;
	}

	@Post("json")
	public Map<String, Object> add(Map<String, Object> m) throws Exception {
		if (m.size() == 0) {
			throw ErrorUtils.statusExp(422, "No input content found");
		}

		if (cores.getCoreNames().size() == 0) {
			throw ErrorUtils.statusExp(Response.Status.INTERNAL_SERVER_ERROR, new Error("numSolrCores",
					Error.E_INVALID_OPERATION, "You cannot create a SolrCore unless one already exists"));
		}

		String name = (String) m.get("name");
		String inputInstanceDir = (String) m.get("instance_dir");
		String templateName = (String) m.get("template");

		CollectionServerResource.validate(cm, cores, m, name);

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
				throw ErrorUtils.statusExp(422, "Cannot create instanceDir: " + instanceDirFile.getAbsolutePath());
			}

			gaia.utils.FileUtils.extract(pickTemplate(templateName), instanceDirFile);
		} else if ((null != templateName) && (instanceDirFile.exists())) {
			throw ErrorUtils.statusExp(422, new Error("template", Error.E_INVALID_VALUE,
					"Can not specify both an existing instanceDir and a template"));
		}

		if (cores.isZooKeeperAware()) {
			ZkController zkController = cores.getZkController();
			zkController.uploadConfigDir(new File(instanceDirFile, "conf"), name);
		}

		String dataHome = Constants.GAIA_DATA_HOME;
		if (dataHome == null) {
			throw ErrorUtils.statusExp(Response.Status.INTERNAL_SERVER_ERROR, new Error("data.dir", Error.E_MISSING_VALUE,
					"data.dir system property could not be found"));
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

		getResponse().setLocationRef("collections/" + URLEncoder.encode(name, "UTF-8"));

		setStatus(Status.SUCCESS_CREATED);
		AuditLogger.log("created new collection: " + name);

		Map<String, Object> attribs = CollectionServerResource.getCollectionMap(coll);

		return attribs;
	}

	private void createCollection(String name, String instanceDir, String dataDir, Integer numShards,
			Integer replicationFactor, Integer maxShardsPerNode) throws IOException {
		URL url = MasterConfUtil.getSolrAddress(true, null);

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
			LOG.warn("Could not create SolrCloud collection", e);
			throw ErrorUtils.statusExp(Response.Status.INTERNAL_SERVER_ERROR,
					"Could not create SolrCloud collection " + e.getMessage());
		} finally {
			server.shutdown();
		}
	}

	private void createCore(String name, String instanceDir, String dataDir) throws IOException {
		URL url = MasterConfUtil.getSolrAddress(true, null);

		SolrServer server = new HttpSolrServer(url.toExternalForm());
		try {
			CoreAdminRequest.Create req = new CoreAdminRequest.Create();
			req.setCoreName(name);
			req.setInstanceDir(instanceDir);
			req.setDataDir(dataDir);
			req.process(server);
		} catch (SolrServerException e) {
			LOG.warn("Could not create Solr core", e);
			throw ErrorUtils.statusExp(Response.Status.INTERNAL_SERVER_ERROR, "Could not create Solr core " + e.getMessage());
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
			throw ErrorUtils.statusExp(Response.Status.INTERNAL_SERVER_ERROR, new Error("template", Error.E_NOT_FOUND,
					"default template could not be found: " + templateName));
		}

		if ((null == zip) || (!zip.exists())) {
			throw ErrorUtils.statusExp(422, new Error("template", Error.E_NOT_FOUND, "template could not be found: "
					+ templateName));
		}

		if (!zip.canRead()) {
			throw ErrorUtils.statusExp(Response.Status.INTERNAL_SERVER_ERROR, new Error("template", Error.E_FORBIDDEN_VALUE,
					"template could not be read: " + zip.getAbsolutePath()));
		}

		return zip;
	}
}
