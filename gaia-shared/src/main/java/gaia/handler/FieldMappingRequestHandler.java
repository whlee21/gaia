package gaia.handler;

import java.io.Reader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.solr.cloud.ZkController;
import org.apache.solr.common.cloud.ClusterState;
import org.apache.solr.common.cloud.DocCollection;
import org.apache.solr.common.cloud.Replica;
import org.apache.solr.common.cloud.Slice;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.ContentStream;
import org.apache.solr.core.CoreContainer;
import org.apache.solr.core.SolrCore;
import org.apache.solr.handler.RequestHandlerBase;
import org.apache.solr.handler.component.ShardHandler;
import org.apache.solr.handler.component.ShardHandlerFactory;
import org.apache.solr.handler.component.ShardRequest;
import org.apache.solr.handler.component.ShardResponse;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.response.SolrQueryResponse;
import org.apache.solr.update.processor.UpdateRequestProcessorChain;
import org.apache.solr.update.processor.UpdateRequestProcessorFactory;
import org.apache.solr.util.plugin.SolrCoreAware;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gaia.common.params.FieldMappingParams;
import gaia.crawl.datasource.FieldMapping;
import gaia.crawl.datasource.FieldMappingUtil;
import gaia.update.DistributedUpdateProcessorFactory;
import gaia.update.FieldMappingUpdateProcessorFactory;

public class FieldMappingRequestHandler extends RequestHandlerBase implements SolrCoreAware {
	private static final Logger LOG = LoggerFactory.getLogger(FieldMappingRequestHandler.class);
	Map<String, Map<String, FieldMapping>> mappings = null;

	public void inform(SolrCore core) {
		if (mappings == null) {
			FieldMappingLoader loader = new FieldMappingLoader(core);
			try {
				mappings = loader.loadMappings();
			} catch (Exception e) {
				LOG.warn(new StringBuilder().append("Exception loading field mappings: ").append(e.toString()).toString());
				mappings = null;
			}
			if (mappings == null) {
				LOG.info("No saved field mappings");
				mappings = new HashMap<String, Map<String, FieldMapping>>();
			}
		}
		for (String chain : mappings.keySet())
			try {
				localUpdate(core, chain, mappings.get(chain).keySet());
			} catch (Exception e) {
				LOG.warn(new StringBuilder().append("Failed to configure local update chain '").append(chain).append("': ")
						.append(e.toString()).toString());
			}
	}

	public void handleRequestBody(SolrQueryRequest req, SolrQueryResponse rsp) throws Exception {
		SolrParams params = req.getParams();
		String dsId = params.get("fm.ds");
		String action = params.get("fm.action");
		String chain = params.get("fm.chain");
		if (chain == null) {
			chain = "";
		}
		if (action == null) {
			LOG.error("No action specified (null)");
			rsp.add("error", "No action specified (null)");
			return;
		}
		if (action.equals(FieldMappingParams.Action.DEFINE.toString())) {
			Iterable<ContentStream> streams = req.getContentStreams();
			if (streams == null) {
				throw new RuntimeException(new StringBuilder().append("fm.action=").append(FieldMappingParams.Action.DEFINE)
						.append(" but missing content streams!").toString());
			}

			define(chain, streams, req, rsp);
		} else {
			if (action.equals(FieldMappingParams.Action.GET.toString())) {
				Map<String, Map<String, Object>> res = new HashMap<String, Map<String, Object>>();
				for (Map.Entry<String, Map<String, FieldMapping>> e : mappings.entrySet()) {
					Map<String, Object> map = new HashMap<String, Object>();
					for (Map.Entry<String, FieldMapping> em : e.getValue().entrySet()) {
						if ((dsId == null) || ((dsId != null) && (((String) em.getKey()).equals(dsId)))) {
							res.put(e.getKey(), map);
							map.put(em.getKey(), em.getValue().toMap());
						}
					}
				}
				rsp.add("field_mapping", res);
				return;
			}
			if (action.equals(FieldMappingParams.Action.SYNC.toString())) {
				LOG.info("SYNC requested, reloading mappings.");
				FieldMappingLoader loader = new FieldMappingLoader(req.getCore());
				Map<String, Map<String, FieldMapping>> newMappings = loader.loadMappings();
				mappings = newMappings;
				if (mappings != null) {
					for (String ch : mappings.keySet())
						try {
							localUpdate(req.getCore(), ch, mappings.get(ch).keySet());
						} catch (Exception e) {
							LOG.warn(new StringBuilder().append("Failed to configure local update chain '").append(ch).append("': ")
									.append(e.toString()).toString());
						}
				}
			} else {
				if (action.equals(FieldMappingParams.Action.DELETE.toString())) {
					if (dsId == null) {
						LOG.error("DELETE requires non-null fm.ds");
						rsp.add("error", "DELETE requires non-null fm.ds");
						return;
					}
					Map<String, FieldMapping> maps = mappings.get(chain);
					String msg;
					if (maps == null) {
						msg = new StringBuilder().append("No mappings for chain '").append(chain).append("'").toString();
					} else {
						if (maps.remove(dsId) != null)
							msg = new StringBuilder().append("ok: removed mapping for fm.ds=").append(dsId).toString();
						else {
							msg = new StringBuilder().append("ok: no mapping for fm.ds=").append(dsId).toString();
						}
					}
					saveAndPublish(req.getCore(), chain, Collections.singleton(dsId));
					LOG.info(msg);
					rsp.add(FieldMappingParams.Action.DELETE.toString(), msg);
					return;
				}
				if (action.equals(FieldMappingParams.Action.CLEAR.toString())) {
					mappings.clear();
					saveAndPublish(req.getCore(), chain, null);
					LOG.info("CLEAR requested, removed all mappings");
				} else {
					LOG.error(new StringBuilder().append("No valid action specified (").append(action).append(")").toString());
					rsp.add("error", new StringBuilder().append("No valid action specified (").append(action).append(")")
							.toString());
				}
			}
		}
	}

	private synchronized void define(String chain, Iterable<ContentStream> streams, SolrQueryRequest req,
			SolrQueryResponse rsp) {
		List<String> updated = new ArrayList<String>();
		List<String> failed = new ArrayList<String>();
		String dsId = req.getParams().get("fm.ds");
		String singleName = null;
//		for (ContentStream stream : streams) {
//			String name = stream.getName();
//			if (name == null) {
//				if (singleName == null)
//					singleName = dsId;
//				else
//					LOG.warn("Invalid stream with null name, skipping.");
//			} else {
//				// FIXME: by whlee21
//				if ((name == null) && (singleName != null)) {
//					name = singleName;
//				}
//				FieldMapping mapping = null;
//				try {
//					Reader r = stream.getReader();
//					mapping = FieldMappingUtil.fromJSON(r, null);
//				} catch (Exception e) {
//					LOG.warn(new StringBuilder().append("Invalid field mapping for ds=").append(name).append(", skipping: ")
//							.append(e.toString()).toString());
//
//					failed.add(name);
//					continue;
//				}
//
//				if (mapping.isVerifySchema()) {
//					try {
//						FieldMappingUtil.verifySchema(mapping, req.getCore());
//					} catch (Exception e) {
//						LOG.warn(new StringBuilder().append("Exception verifying schema for ds=").append(name)
//								.append(", skipping: ").append(e.toString()).toString());
//
//						failed.add(name);
//					}
//				} else {
//					Map<String, FieldMapping> perDs = mappings.get(chain);
//					if (perDs == null) {
//						perDs = new HashMap<String, FieldMapping>();
//						mappings.put(chain, perDs);
//					}
//					perDs.put(name, mapping);
//					updated.add(name);
//				}
//			}
//		}
		
		for (ContentStream stream : streams) {
			String name = stream.getName();
			if (name == null) {
				if (singleName == null)
					singleName = dsId;
				else
					LOG.warn("Invalid stream with null name, skipping.");
			}
			
			if ((name == null) && (singleName != null)) {
				name = singleName;
			}
			
			FieldMapping mapping = null;
			try {
				Reader r = stream.getReader();
				mapping = FieldMappingUtil.fromJSON(r, null);
			} catch (Exception e) {
				LOG.warn(new StringBuilder().append("Invalid field mapping for ds=").append(name).append(", skipping: ")
						.append(e.toString()).toString());

				failed.add(name);
				continue;
			}

			if (mapping.isVerifySchema()) {
				try {
					FieldMappingUtil.verifySchema(mapping, req.getCore());
				} catch (Exception e) {
					LOG.warn(new StringBuilder().append("Exception verifying schema for ds=").append(name)
							.append(", skipping: ").append(e.toString()).toString());

					failed.add(name);
				}
			} 
			
			Map<String, FieldMapping> perDs = mappings.get(chain);
			if (perDs == null) {
				perDs = new HashMap<String, FieldMapping>();
				mappings.put(chain, perDs);
			}
			perDs.put(name, mapping);
			updated.add(name);
		}
		if (updated.size() > 0) {
			FieldMappingLoader loader = new FieldMappingLoader(req.getCore());
			try {
				loader.saveMappings(mappings);
				LOG.info(new StringBuilder().append("Saved field mappings, updated for datasources ").append(updated)
						.toString());
			} catch (Exception e) {
				LOG.warn(new StringBuilder().append("Exception saving updated mappings: ").append(e.toString()).toString(), e);
			}
			saveAndPublish(req.getCore(), chain, updated);
			rsp.add("fm.action", FieldMappingParams.Action.DEFINE.toString());
			rsp.add(FieldMappingParams.Action.DEFINE.toString(), new StringBuilder().append("ok: chain '").append(chain)
					.append("', updated ").append(updated).append(", failed ").append(failed).toString());
		} else {
			rsp.add("fm.action", FieldMappingParams.Action.DEFINE.toString());
			rsp.add(FieldMappingParams.Action.DEFINE.toString(), new StringBuilder().append("error: all invalid streams ")
					.append(failed).toString());
		}
	}

	private void saveAndPublish(SolrCore core, String chain, Collection<String> updated) {
		FieldMappingLoader loader = new FieldMappingLoader(core);
		try {
			loader.saveMappings(mappings);
			LOG.info(new StringBuilder().append("Saved field mappings, updated for datasources ").append(updated).toString());
		} catch (Exception e) {
			LOG.warn(new StringBuilder().append("Exception saving updated mappings: ").append(e.toString()).toString(), e);
		}
		try {
			localUpdate(core, chain, updated);
		} catch (Exception e) {
			LOG.warn(new StringBuilder().append("Failed to configure local update chain '").append(chain).append("': ")
					.append(e.toString()).toString());
		}
		try {
			broadcast(core, chain, updated);
		} catch (Exception e) {
			LOG.warn(new StringBuilder().append("Failed to broadcast update to chain '").append(chain).append("' ids ")
					.append(updated).append(": ").append(e.toString()).toString());
		}
	}

	private void localUpdate(SolrCore core, String chain, Collection<String> ids) throws Exception {
		UpdateRequestProcessorChain uc = core.getUpdateProcessingChain(chain);

		boolean updated = false;
		for (UpdateRequestProcessorFactory factory : uc.getFactories()) {
			if ((factory instanceof FieldMappingUpdateProcessorFactory)) {
				FieldMappingUpdateProcessorFactory fm = (FieldMappingUpdateProcessorFactory) factory;
				Map<String, FieldMapping> factoryMap = fm.getMappings();
				Map<String, FieldMapping> newMap = mappings.get(chain);
				if (ids != null) {
					for (String dsId : ids) {
						FieldMapping update = (FieldMapping) newMap.get(dsId);
						if (update == null)
							factoryMap.remove(dsId);
						else
							factoryMap.put(dsId, update);
					}
				} else {
					factoryMap.clear();
				}
				LOG.info(new StringBuilder()
						.append(ids == null ? "Deleted" : new StringBuilder().append("Updated ids ").append(ids).toString())
						.append(" in-core field mappings in chain '").append(chain).append("'").toString());
				updated = true;
				break;
			}
		}
		if (!updated)
			LOG.warn(new StringBuilder().append("Could not find in chain '").append(chain).append("' any instance of ")
					.append(FieldMappingUpdateProcessorFactory.class.getName()).append(", local field mapping for ids ")
					.append(ids).append(" could not be updated!").toString());
	}

	private void broadcast(SolrCore core, String chain, Collection<String> ids) throws Exception {
		if (core.getCoreDescriptor().getCoreContainer().isZooKeeperAware())
			zkBroadcast(core, chain, ids);
		else
			shardBroadcast(core, chain, ids);
	}

	private void zkBroadcast(SolrCore core, String chain, Collection<String> ids) throws Exception {
		CoreContainer container = core.getCoreDescriptor().getCoreContainer();
		ZkController zkController = container.getZkController();
		ShardHandlerFactory shardHandlerFactory = container.getShardHandlerFactory();
		ShardHandler shardHandler = shardHandlerFactory.getShardHandler();
		ModifiableSolrParams params = new ModifiableSolrParams();
		params.set("fm.action", new String[] { FieldMappingParams.Action.SYNC.toString() });

		ClusterState cloudState = zkController.getClusterState();
		DocCollection docCol = (DocCollection) cloudState.getCollectionStates().get(core.getName());
		Map<String, Slice> slices = null != docCol ? docCol.getSlicesMap() : Collections.<String, Slice> emptyMap();

		for (Map.Entry<String, Slice> entry : slices.entrySet()) {
			Slice slice = (Slice) entry.getValue();

			ArrayList<Replica> replicas = new ArrayList<Replica>();
			if (slice.getReplicas() != null)
				replicas.addAll(slice.getReplicas());
			if (slice.getLeader() != null) {
				replicas.add(slice.getLeader());
			}
			LOG.info(new StringBuilder().append("ZK broadcast to ").append(replicas.size()).append(" shards in slice ")
					.append(slice.getName()).append(".").toString());
			for (Replica replica : replicas) {
				if (cloudState.liveNodesContain(replica.getStr("node_name"))) {
					params.set("core", new String[] { replica.getStr("core") });
					String replicaUrl = replica.getStr("base_url");

					if (!replicaUrl.endsWith("/")) {
						replicaUrl = new StringBuilder().append(replicaUrl).append("/").toString();
					}
					replicaUrl = new StringBuilder().append(replicaUrl).append(core.getName()).toString();
					ShardRequest sreq = new ShardRequest();

					params.set("qt", new String[] { "/fmap" });
					sreq.purpose = 1;

					if (replicaUrl.startsWith("http://"))
						replicaUrl = replicaUrl.substring(7);
					sreq.shards = new String[] { replicaUrl };
					sreq.actualShards = sreq.shards;
					sreq.params = params;

					shardHandler.submit(sreq, replicaUrl, sreq.params);
				}
			}
		}
		ShardResponse srsp;
		do {
			srsp = shardHandler.takeCompletedOrError();
			if (srsp != null) {
				Throwable e = srsp.getException();
				if (e != null)
					LOG.error(new StringBuilder().append("Error talking to shard: ").append(srsp.getShard()).toString(), e);
			}
		} while (srsp != null);
	}

	private void shardBroadcast(SolrCore core, String chain, Collection<String> ids) throws Exception {
		DistributedUpdateProcessorFactory distrib = null;
		UpdateRequestProcessorChain uc = core.getUpdateProcessingChain(chain);
		if (uc != null) {
			for (UpdateRequestProcessorFactory f : uc.getFactories()) {
				if ((f instanceof DistributedUpdateProcessorFactory)) {
					distrib = (DistributedUpdateProcessorFactory) f;
					break;
				}
			}
		}
		if (distrib == null) {
			return;
		}
		List<String> shards = distrib.getShards();
		if (shards == null) {
			LOG.info("No shards - no broadcast needed.");
		} else {
			LOG.info(new StringBuilder().append("Shard broadcast to: ").append(shards).toString());
			throw new UnsupportedOperationException("this broadcast method is not supported yet");
		}
	}

	public String getVersion() {
		return "$Revision$";
	}

	public String getDescription() {
		return "Receives and synchronizes field mapping definitions";
	}

	public String getSource() {
		return "$URL$";
	}
}
