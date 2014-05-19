package gaia.security;

import gaia.crawl.security.SecurityFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.core.SolrCore;
import org.apache.solr.handler.component.ResponseBuilder;
import org.apache.solr.handler.component.SearchComponent;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.util.plugin.NamedListInitializedPlugin;
import org.apache.solr.util.plugin.SolrCoreAware;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AclBasedFilterComponent extends SearchComponent implements SolrCoreAware {
	private Logger LOG = LoggerFactory.getLogger(AclBasedFilterComponent.class);
	public static final String PROVIDER_CLASS_PARAMETER_NAME = "provider.class";
	public static final String PROVIDER_CONFIG_LIST_NAME = "provider.config";
	public static final String FILTERER_CLASS_PARAMETER_NAME = "filterer.class";
	public static final String FILTERER_CONFIG_LIST_NAME = "filterer.config";
	private ACLTagProvider provider = null;
	private ACLQueryFilterer filterer = null;
	private NamedList config;
	private boolean initialized;

	public void init(NamedList config) {
		super.init(config);
		this.config = config;
	}

	private <T> T instantiateAndConfigure(String classNameParameterName, String classConfigParameterName,
			ClassLoader loader) {
		SolrParams params = SolrParams.toSolrParams(config);
		String className = params.get(classNameParameterName);
		LOG.info("Instantiating class: '{}'", className);
		try {
			Class<?> theClass = loader.loadClass(className);
			Object instance = theClass.newInstance();
			if ((config != null) && ((instance instanceof NamedListInitializedPlugin))) {
				LOG.info("Configuring class: '{}'", className);
				NamedList classConfig = (NamedList) config.get(classConfigParameterName);
				((NamedListInitializedPlugin) instance).init(classConfig);
			}
			return (T) instance;
		} catch (Exception e) {
			LOG.error("Could not initialize class '{" + className + "}'", e);

			LOG.error("Returning null instance.");
		}
		return null;
	}

	public String getDescription() {
		return "Adds filter queries to SolrQueryRequest based on acl tags";
	}

	public String getSource() {
		return "$URL: $";
	}

	public String getVersion() {
		return "$Revision: $";
	}

	public void prepare(ResponseBuilder builder) throws IOException {
		boolean isShard = builder.req.getParams().getBool("isShard", false);
		if (isShard) {
			return;
		}

		SolrQueryRequest req = builder.req;
		ModifiableSolrParams params = new ModifiableSolrParams(req.getParams());
		req.setParams(params);

		String[] users = req.getParams().getParams("user");
		Set<String> tags = null;
		if (users != null) {
			String user = users[0];

			tags = getTags(user);
		} else {
			tags = Collections.emptySet();
		}

		SecurityFilter filter = filterer.buildFilter(tags, "f");

		LOG.info("ACL security filter: " + filter.toString());

		if (filter.getFilter() == null) {
			return;
		}

		params.add("fq", new String[] { filter.getFilter() });
		for (Map.Entry<String, String> entry : filter.getNestedClauses().entrySet())
			params.set((String) entry.getKey(), new String[] { (String) entry.getValue() });
	}

	public void process(ResponseBuilder builder) throws IOException {
	}

	private Set<String> getTags(String user) {
		Set<String> tags;
		if (provider != null) {
			tags = provider.getACLTagsForUser(user);
		} else {
			LOG.warn("No provider configured. Using empty set.");
			tags = Collections.emptySet();
		}
		return tags;
	}

	public void inform(SolrCore arg0) {
		if (!initialized) {
			ClassLoader loader = arg0.getResourceLoader().getClassLoader();
			provider = ((ACLTagProvider) instantiateAndConfigure(PROVIDER_CLASS_PARAMETER_NAME, PROVIDER_CONFIG_LIST_NAME,
					loader));

			filterer = ((ACLQueryFilterer) instantiateAndConfigure(FILTERER_CLASS_PARAMETER_NAME, FILTERER_CONFIG_LIST_NAME,
					loader));

			initialized = true;
		}
	}
}
