package gaia.commons.management;

import com.google.inject.Inject;
import gaia.commons.api.API;
import gaia.commons.api.Configuration;
import gaia.commons.services.BaseServerResource;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class EndpointsSR extends BaseServerResource implements EndpointsResource {
	protected Set<API> apis;

	@Inject
	public EndpointsSR(Configuration configuration, Set<API> apis) {
		super(configuration);
		this.apis = apis;
	}

	public Map<String, Collection<String>> endpoints() {
		Map<String, Collection<String>> result = new HashMap<String, Collection<String>>();
		for (API api : apis) {
			Collection<String> uris = result.get(api.getAPIName());
			if (uris == null) {
				uris = new HashSet<String>();
				result.put(api.getAPIName(), uris);
			}
			uris.addAll(api.getEndpoints());
		}
		return result;
	}
}
