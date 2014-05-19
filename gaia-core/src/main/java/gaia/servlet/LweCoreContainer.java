package gaia.servlet;

import java.util.Map;

import org.apache.solr.common.cloud.ClusterState;
import org.apache.solr.common.cloud.Replica;
import org.apache.solr.common.cloud.Slice;
import org.apache.solr.common.cloud.ZkNodeProps;
import org.apache.solr.common.cloud.ZkStateReader;
import org.apache.solr.core.ConfigSolr;
import org.apache.solr.core.CoreContainer;
import org.apache.solr.core.SolrCore;
import org.apache.solr.core.SolrResourceLoader;

public class LweCoreContainer extends CoreContainer {
	public LweCoreContainer(SolrResourceLoader loader, ConfigSolr config) {
		super(loader, config);
	}

	public SolrCore getCore(String name) {
		SolrCore core = super.getCore(name);
		if (!isZooKeeperAware()) {
			return core;
		}
		if (core != null) {
			return core;
		}
		return getCoreByCollection(name);
	}

	private SolrCore getCoreByCollection(String corename) {
		String collection = corename;
		ZkStateReader zkStateReader = getZkController().getZkStateReader();

		ClusterState clusterState = zkStateReader.getClusterState();
		Map<String, Slice> slices = clusterState.getSlicesMap(collection);
		if (slices == null) {
			return null;
		}

		SolrCore core = null;

		for (Map.Entry<String, Slice> entry : slices.entrySet()) {
			ZkNodeProps leaderProps = clusterState.getLeader(collection, (String) entry.getKey());
			if (leaderProps != null) {
				core = checkProps(leaderProps);
			}
			if (core != null) {
				break;
			}

			Map<String, Replica> shards = entry.getValue().getReplicasMap();
			for (Map.Entry<String, Replica> shardEntry : shards.entrySet()) {
				Replica zkProps = (Replica) shardEntry.getValue();
				core = checkProps(zkProps);
				if (core != null) {
					return core;
				}
			}
		}
		return core;
	}

	private SolrCore checkProps(ZkNodeProps zkProps) {
		SolrCore core = null;
		if (getZkController().getNodeName().equals(zkProps.getStr("node_name"))) {
			String corename = zkProps.getStr("core");
			core = super.getCore(corename);
		}
		return core;
	}
}
