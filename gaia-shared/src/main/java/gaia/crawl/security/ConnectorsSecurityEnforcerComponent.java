package gaia.crawl.security;

import java.io.IOException;
import java.net.URL;
import java.util.Map;

import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.core.CloseHook;
import org.apache.solr.core.CoreContainer;
import org.apache.solr.core.SolrCore;
import org.apache.solr.handler.component.ResponseBuilder;
import org.apache.solr.handler.component.SearchComponent;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.util.plugin.SolrCoreAware;
import org.restlet.Client;
import org.restlet.data.Protocol;
import org.restlet.resource.ClientResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gaia.utils.MasterConfUtil;

public class ConnectorsSecurityEnforcerComponent extends SearchComponent implements SolrCoreAware {
	private static Logger LOG = LoggerFactory.getLogger(ConnectorsSecurityEnforcerComponent.class);
	private String collection;
	private Client client = null;

	public void init(NamedList config) {
	}

	public String getDescription() {
		return "Enforces security for Gaia data sources";
	}

	public String getSource() {
		return "$URL: $";
	}

	public String getVersion() {
		return "$Revision: $";
	}

	public void prepare(ResponseBuilder builder) {
		boolean isShard = builder.req.getParams().getBool("isShard", false);
		if (isShard) {
			return;
		}
		SolrQueryRequest req = builder.req;

		ModifiableSolrParams params = new ModifiableSolrParams(req.getParams());
		req.setParams(params);
		SecurityFilter filter;
		try {
			String user = req.getParams().get("user");

			URL lweCoreUrl = MasterConfUtil.getGaiaSearchAddress();
			URL url = new URL(new StringBuilder().append(lweCoreUrl.toExternalForm()).append("/api/collections/")
					.append(collection).append("/security_trimming?user=").append(user == null ? "" : user).toString());
			LOG.debug(new StringBuilder().append("security trimming web service call = ").append(url.toExternalForm())
					.toString());

			ClientResource res = new ClientResource(url.toExternalForm());
			res.setNext(client);

			Map<String, Object> map = (Map) res.get(Map.class);
			res.release();

			if (map == null) {
				return;
			}

			filter = SecurityFilter.fromMap(map);
		} catch (Throwable e) {
			LOG.error("Exception when running security trimming API call, fallback to restricting all results", e);
			filter = new SecurityFilter("-*:*");
		}

		if ((filter == null) || (filter.getFilter() == null)) {
			return;
		}

		LOG.info(new StringBuilder().append("Connectors security filter: ").append(filter.toString()).toString());

		params.add("fq", new String[] { filter.getFilter() });
		for (Map.Entry<String, String> entry : filter.getNestedClauses().entrySet())
			params.set((String) entry.getKey(), new String[] { (String) entry.getValue() });
	}

	public void process(ResponseBuilder builder) throws IOException {
	}

	public void inform(SolrCore core) {
		CoreContainer cores = core.getCoreDescriptor().getCoreContainer();
		if (cores.isZooKeeperAware())
			collection = core.getCoreDescriptor().getCloudDescriptor().getCollectionName();
		else {
			collection = core.getName();
		}
		client = new Client(Protocol.HTTP);
		core.addCloseHook(new CloseHook() {
			public void preClose(SolrCore arg0) {
				if (client != null)
					try {
						client.stop();
					} catch (Exception e) {
					}
			}

			public void postClose(SolrCore arg0) {
			}
		});
	}
}
