package gaia.api;

import gaia.Constants;
import gaia.admin.collection.AdminScheduler;
import gaia.admin.collection.CollectionManager;
import gaia.crawl.ConnectorManager;
import gaia.crawl.datasource.DataSourceId;
import gaia.jmx.JmxManager;
import gaia.utils.MasterConfUtil;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;

import javax.ws.rs.core.Response;

import org.apache.commons.io.FileUtils;
import org.apache.solr.client.solrj.SolrRequest;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.client.solrj.request.QueryRequest;
import org.apache.solr.common.params.CollectionParams;
import org.apache.solr.common.params.CoreAdminParams;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.core.CoreContainer;
import org.restlet.data.Parameter;
import org.restlet.data.Status;
import org.restlet.resource.Delete;
import org.restlet.resource.Get;
import org.restlet.resource.ResourceException;
import org.restlet.resource.ServerResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;

public class CollectionServerResource extends ServerResource implements CollectionResource {
	private static final Logger LOG = LoggerFactory.getLogger(CollectionServerResource.class);
	public static final String NAME = "name";
	public static final String INSTANCE_DIR = "instance_dir";
	public static final String TEMPLATE = "template";
	public static final String NUM_SHARDS = "num_shards";
	public static final String REPLICATION_FACTOR = "replication_factor";
	public static final String MAX_SHARDS_PER_NODE = "max_shards_per_node";
	private CoreContainer cores;
	private String collection;
	private CollectionManager cm;
	private AdminScheduler adminScheduler;
	private ConnectorManager crawlerManager;
	private JmxManager jmx;

	@Inject
	public CollectionServerResource(CoreContainer cores, CollectionManager cm, AdminScheduler adminScheduler,
			ConnectorManager crawlerManager, JmxManager jmx) {
		this.cores = cores;
		this.cm = cm;
		this.adminScheduler = adminScheduler;
		this.crawlerManager = crawlerManager;
		this.jmx = jmx;
	}

	public void doInit() throws ResourceException {
		collection = ((String) getRequestAttributes().get("coll_name"));

		setExisting(APIUtils.coreExists(cores, collection));
	}

	@Get("json")
	public Map<String, Object> retrieve() throws IOException, SolrServerException {
		gaia.admin.collection.Collection coll = cm.getCollection(collection);

		Map<String, Object> attribs = getCollectionMap(coll);

		return attribs;
	}

	static Map<String, Object> getCollectionMap(gaia.admin.collection.Collection coll) {
		Map<String, Object> attribs = new HashMap<String, Object>();
		attribs.put("name", coll.getName());
		attribs.put("instance_dir", coll.getInstanceDir());
		return attribs;
	}

	static void validate(CollectionManager cm, CoreContainer cores, Map<String, Object> m, String name) {
		List<Error> errors = new ArrayList<Error>();
		if ((name == null) || (name.trim().length() == 0)) {
			errors.add(new Error("name", Error.E_MISSING_VALUE, "name is a required key"));
		}

		if (name != null) {
			Matcher matcher = APIUtils.ALPHANUM.matcher(name);
			if (!matcher.matches()) {
				errors.add(new Error("name", Error.E_INVALID_VALUE, "name must consist of only A-Z a-z 0-9 - _"));
			}
		}

		String template = (String) m.get("template");
		if ((template != null) && (template.trim().length() == 0)) {
			errors.add(new Error("template", Error.E_INVALID_VALUE, "template cannot be empty"));
		}

		String instanceDir = (String) m.get("instance_dir");
		if ((instanceDir != null) && (instanceDir.trim().length() == 0)) {
			errors.add(new Error("instance_dir", Error.E_INVALID_VALUE, "instance_dir cannot be empty"));
		}
		String path = getPath(instanceDir);
		if (path != null) {
			List<gaia.admin.collection.Collection> collections = cm.getCollections();
			for (gaia.admin.collection.Collection collection : collections) {
				if (getPath(collection.getInstanceDir()).equals(path)) {
					errors.add(new Error("instance_dir", Error.E_EXISTS, "this instance_dir is already used by a collection - "
							+ instanceDir));
				}

			}

		}

		if (instanceDir != null) {
			File instanceDirFile = new File(instanceDir);
			if ((instanceDirFile.exists()) && (instanceDirFile.isFile())) {
				errors.add(new Error("instance_dir", Error.E_INVALID_VALUE,
						"instance_dir appears to point to an existing file rather than directory"));
			} else if (instanceDirFile.exists()) {
				File solrConfig = new File(instanceDir + File.separator + "conf", "solrconfig.xml");
				if (!solrConfig.exists()) {
					errors.add(new Error("instance_dir", Error.E_INVALID_VALUE,
							"instance_dir appears to point to an invalid collection"));
				}
			}

		}

		if (cores.isZooKeeperAware()) {
			Object numShardsParam = m.get("num_shards");
			if (numShardsParam == null)
				errors.add(new Error("num_shards", Error.E_MISSING_VALUE,
						"num_shards is a required parameter in SolrCloud mode"));
			else {
				try {
					int val = Integer.parseInt(numShardsParam.toString());
					if (val < 1)
						errors.add(new Error("num_shards", Error.E_INVALID_VALUE, "num_shards is invalid"));
				} catch (NumberFormatException e) {
					errors.add(new Error("num_shards", Error.E_INVALID_VALUE, "num_shards is invalid: " + e.toString()));
				}
			}

			Object replicationFactorParam = m.get("replication_factor");
			if (replicationFactorParam != null) {
				try {
					int val = Integer.parseInt(replicationFactorParam.toString());
					if (val < 1)
						errors.add(new Error("replication_factor", Error.E_INVALID_VALUE, "replication_factor is invalid"));
				} catch (NumberFormatException e) {
					errors.add(new Error("replication_factor", Error.E_INVALID_VALUE, "replication_factor is invalid: "
							+ e.toString()));
				}
			}

			Object maxShardsPerNodeParam = m.get("max_shards_per_node");
			if (maxShardsPerNodeParam != null) {
				try {
					int val = Integer.parseInt(maxShardsPerNodeParam.toString());
					if (val < 1)
						errors.add(new Error("max_shards_per_node", Error.E_INVALID_VALUE, "max_shards_per_node is invalid"));
				} catch (NumberFormatException e) {
					errors.add(new Error("max_shards_per_node", Error.E_INVALID_VALUE, "max_shards_per_node is invalid: "
							+ e.toString()));
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
			errors.add(new Error(m.keySet().toString(), Error.E_FORBIDDEN_KEY, "Unknown or dissallowed key found:: "
					+ m.keySet()));
		}

		if (errors.size() > 0) {
			throw ErrorUtils.statusExp(422, errors);
		}

		if (APIUtils.coreExists(cores, name))
			throw ErrorUtils.statusExp(Response.Status.CONFLICT, new Error("name", Error.E_FORBIDDEN_VALUE,
					"collection already exists"));
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

	@Delete("json")
	public void remove(Map<String, Object> m) throws Exception {
		if (!isExisting()) {
			return;
		}

		if (cores.getCoreNames().size() == 1) {
			throw ErrorUtils.statusExp(422, "At least one SolrCore must always exist, so you cannot remove: "
					+ collection);
		}

		List<Error> errors = new ArrayList<Error>();

		LOG.info("Stopping all scheduled jobs for collection");
		try {
			adminScheduler.stopAndRemoveAllSchedules(collection);
		} catch (Exception e) {
			errors.add(new Error(collection, Error.E_EXCEPTION, "Unable to Clear All Schedules"));
		}

		LOG.info("Done stopping all scheduled jobs for collection");
		Parameter p = (Parameter) getQuery().getFirst("force");
		boolean force = p != null ? p.getValue().toLowerCase().equals("true") : false;
		try {
			crawlerManager.setClosing(null, collection, true);
			if (force) {
				crawlerManager.finishAllJobs(null, collection, true, 60000L);
				crawlerManager.close(null, collection, false);
				crawlerManager.resetAll(null, collection);
			} else {
				crawlerManager.close(null, collection, true);
				crawlerManager.resetAll(null, collection);
				crawlerManager.close(null, collection, false);
			}

			List<DataSourceId> ids = crawlerManager.listDataSources(collection);
			for (DataSourceId dsId : ids) {
				jmx.unregisterDataSourceMBean(dsId);
			}

			crawlerManager.removeDataSources(collection, true);
			crawlerManager.setClosing(null, collection, false);
		} catch (Throwable e) {
			crawlerManager.setClosing(null, collection, false);
			LOG.warn("Exception removing collection " + collection + ": " + e.toString());
			errors.add(new Error(collection, Error.E_EXCEPTION, e.toString()));
		}

		if (errors.size() > 0) {
			throw ErrorUtils.statusExp(422, errors);
		}
		LOG.info("No jobs are running for collection " + collection);
		try {
			gaia.admin.collection.Collection coll = cm.getCollection(collection);
			cm.removeCollection(coll);
			try {
				FileUtils.deleteDirectory(new File(Constants.GAIA_DATA_HOME + File.separator + "solr" + File.separator
						+ "cores" + File.separator + coll.getInstanceDir()));
			} catch (IOException e) {
				if (e.getMessage().contains("Unable to delete file: "))
					LOG.warn("Some files in collection '" + collection + "' data dir could not be removed: " + e.toString());
				else {
					errors.add(new Error(collection, Error.E_EXCEPTION, "Unable to Delete Collection 'data' dir: "
							+ e.toString()));
				}
			}

			if (cores.isZooKeeperAware())
				removeCollection(collection);
			else
				removeCore(collection);
		} catch (Throwable e) {
			LOG.error("Unable to Delete Collection Data: ", e);
			errors.add(new Error(collection, Error.E_EXCEPTION, "Unable to Delete Collection Data: " + e.toString()));
		}

		if (!errors.isEmpty()) {
			throw ErrorUtils.statusExp(Response.Status.INTERNAL_SERVER_ERROR, errors);
		}

		setStatus(Status.SUCCESS_NO_CONTENT);
		AuditLogger.log("removed collection:" + collection);
	}

	private void removeCollection(String name) throws IOException, SolrServerException {
		URL url = MasterConfUtil.getSolrAddress(true, null);

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
		URL url = MasterConfUtil.getSolrAddress(true, null);

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
}
