package gaia.api;

import gaia.crawl.security.ConnectorsSecurityEnforcer;
import gaia.crawl.security.SecurityFilter;

import java.util.Map;

import javax.ws.rs.core.Response;

import org.apache.solr.core.CoreContainer;
import org.apache.solr.core.SolrCore;
import org.restlet.data.Parameter;
import org.restlet.resource.ResourceException;
import org.restlet.resource.ServerResource;

import com.google.inject.Inject;

public class SecurityTrimmingServerResource extends ServerResource implements SecurityTrimmingResource {
	private String collection;
	private ConnectorsSecurityEnforcer enforcer;
	private CoreContainer cores;

	@Inject
	public SecurityTrimmingServerResource(ConnectorsSecurityEnforcer enforcer, CoreContainer cores) {
		this.enforcer = enforcer;
		this.cores = cores;
	}

	public void doInit() throws ResourceException {
		collection = ((String) getRequestAttributes().get("coll_name"));
		SolrCore core = cores.getCore(collection);
		try {
			setExisting(core != null);
		} finally {
			if (core != null)
				core.close();
		}
	}

	public Map<String, ?> retrieve() {
		if (!isExisting()) {
			throw ErrorUtils.statusExp(Response.Status.NOT_FOUND, "URI not found");
		}

		Parameter p = (Parameter) getQuery().getFirst("user");
		String user = p == null ? null : p.getValue();

		SecurityFilter res = enforcer.buildSecurityFilter(collection, user);
		if (res == null) {
			return null;
		}

		return res.toMap();
	}
}
