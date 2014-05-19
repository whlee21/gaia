package gaia.commons.services;

import com.google.common.collect.Iterables;
import com.google.inject.Inject;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.TreeSet;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SolrAwareServiceLocator extends BaseServiceLocator implements ServiceLocator {
	private static Logger LOG = LoggerFactory.getLogger(SolrAwareServiceLocator.class);

	protected final HashSet<Watcher> watchers = new HashSet<Watcher>();
	protected final ObjectMapper mapper = new ObjectMapper();
	protected final String solrZkPath;
	protected final String LIVE_NODES;
	protected final String CLUSTERSTATE;
	protected ZkWatcher zkWatcher = new ZkWatcher();
	protected ZooKeeper zk;
	protected Map<String, Collection<URIPayload>> serviceURIs;

	@Inject
	public SolrAwareServiceLocator(PropertyServiceLocator psl) throws IOException {
		Properties props = psl.getProperties();
		String zkhosts = props.getProperty("zkhost");
		solrZkPath = props.getProperty("solrZkPath");
		LIVE_NODES = (solrZkPath + "/live_nodes");
		CLUSTERSTATE = (solrZkPath + "/clusterstate.json");
		zk = new ZooKeeper(zkhosts, 3000, zkWatcher);
		refreshServiceURIs();
	}

	public void registerService(String name, URI endpoint, Map<String, String> payload) {
		throw new UnsupportedOperationException();
	}

	public void unregisterService(String name, URI endpoint) {
		throw new UnsupportedOperationException();
	}

	public URIPayload getServiceURI(String serviceType) {
		URIPayload result = null;
		Collection<URIPayload> uris = getServiceURIs(serviceType);
		if ((uris != null) && (!uris.isEmpty())) {
			result = (URIPayload) Iterables.getFirst(uris, null);
		}
		return result;
	}

	public Map<String, Collection<URIPayload>> getServiceURIs() {
		return serviceURIs;
	}

	public Collection<URIPayload> getServiceURIs(String serviceType) {
		Collection<URIPayload> result = null;
		if (serviceURIs != null) {
			result = serviceURIs.get(serviceType);
		}
		return result;
	}

	public void close() throws IOException {
		try {
			zk.close();
		} catch (InterruptedException e) {
			LOG.warn("Ignoring InterruptedException during ZooKeeper.close()");
		}
	}

	protected void refreshServiceURIs() {
		try {
			if ((zk.exists(LIVE_NODES, new ZkWatcher()) == null) || (zk.exists(CLUSTERSTATE, new ZkWatcher()) == null)) {
				return;
			}
			TreeSet<String> liveNodes = new TreeSet<String>();
			liveNodes.addAll(zk.getChildren(LIVE_NODES, new ZkWatcher()));
			byte[] rawJson = zk.getData(CLUSTERSTATE, new ZkWatcher(), new Stat());
			JsonNode clusterData = mapper.readTree(rawJson);
			Map<String, Collection<URIPayload>> result = new HashMap<String, Collection<URIPayload>>();
			Iterator<String> i = clusterData.getFieldNames();
			String collectionName;
			Collection<URIPayload> uriPayloads;
			while (i.hasNext()) {
				collectionName = (String) i.next();
				uriPayloads = new ArrayList<URIPayload>();
				for (JsonNode collection : clusterData.get(collectionName)) {
					for (JsonNode node : collection) {
						if ((node.get("base_url") != null) && (liveNodes.contains(node.get("node_name").asText()))) {
							uriPayloads.add(new URIPayload(new URI(node.get("base_url").asText())));
						}
					}

					if (!uriPayloads.isEmpty()) {
						result.put(collectionName, uriPayloads);
					}
				}
			}
			serviceURIs = result;
		} catch (KeeperException e) {
			LOG.error("refreshServiceURIs", e);
		} catch (InterruptedException e) {
			LOG.error("refreshServiceURIs", e);
		} catch (Exception e) {
			LOG.error("refreshServiceURIs", e);
		}
	}

	public void addWatcher(Watcher watcher) {
		watchers.add(watcher);
	}

	public class ZkWatcher implements Watcher {
		public ZkWatcher() {
		}

		public void process(WatchedEvent event) {
			// None (-1),
			// NodeCreated (1),
			// NodeDeleted (2),
			// NodeDataChanged (3),
			// NodeChildrenChanged (4);
			switch (event.getType()) { // FIXME: by whlee21
			case NodeCreated:
			case NodeDeleted:
			case NodeDataChanged:
			case NodeChildrenChanged:
				if (event.getPath().equals(CLUSTERSTATE)) {
					refreshServiceURIs();
					for (Watcher watcher : watchers)
						watcher.process(event);
				} else if (event.getPath().startsWith(LIVE_NODES)) {
					refreshServiceURIs();
					for (Watcher watcher : watchers)
						watcher.process(event);
				}
				break;
			case None:
			}
		}
	}
}
