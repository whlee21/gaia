package gaia.handler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.solr.common.SolrException;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.handler.component.ResponseBuilder;
import org.apache.solr.handler.component.SearchComponent;
import org.apache.solr.handler.component.ShardRequest;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.search.QParser;
import org.apache.solr.search.QueryUtils;
import org.apache.solr.search.SyntaxError;
import org.apache.solr.util.SolrPluginUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RoleBasedFilterComponent extends SearchComponent {
	private static final String PARAMETER_PREFIX = "r";
	private static final String ROLE = "role";
	private static final Logger LOG = LoggerFactory.getLogger(RoleBasedFilterComponent.class);
	public static final String NO_ROLE_FILTER = "default.filter";
	public static final String FILTER_LIST = "filters";
	private Map<String, String[]> filters = new HashMap<String, String[]>();
	private String[] defaultFilters;

	public void init(NamedList config) {
		super.init(config);
		List<Object> defaultFilterList = config.getAll("default.filter");
		defaultFilters = ((String[]) defaultFilterList.toArray(new String[defaultFilterList.size()]));

		LOG.debug("Using filters " + defaultFilterList + " for non existing roles.");

		HashMap<String, LinkedHashSet<String>> buildFilters = new HashMap<String, LinkedHashSet<String>>();

		NamedList filterConfig = (NamedList) config.getAll("filters").get(0);

		for (int i = 0; i < filterConfig.size(); i++) {
			String role = filterConfig.getName(i);
			String filterQuery = ((String) filterConfig.getVal(i)).trim();

			LinkedHashSet<String> filterList = buildFilters.get(role);
			if (filterList == null) {
				filterList = new LinkedHashSet<String>();
				buildFilters.put(role, filterList);
			}

			if (0 < filterQuery.length()) {
				LOG.debug("Adding filter '" + filterQuery + "' for role" + role);
				filterList.add(filterQuery);
			}

		}

		for (Map.Entry<String, LinkedHashSet<String>> entry : buildFilters.entrySet())
			filters.put(entry.getKey(), entry.getValue().toArray(new String[entry.getValue().size()]));
	}

	public Map<String, String[]> getFilters() {
		return filters;
	}

	public String getDescription() {
		return "Adds filter queries to SolrQueryRequest based on user role";
	}

	public String getSource() {
		return "$URL: $";
	}

	public String getVersion() {
		return "$Revision: $";
	}

	public void prepare(ResponseBuilder builder) throws IOException {
		if (isDistrib(builder)) {
			return;
		}
		SolrQueryRequest req = builder.req;
		try {
			List<Query> allRoleQueries = getFilters(req);

			if (0 < allRoleQueries.size()) {
				List<Query> solrFilters = builder.getFilters();
				if (solrFilters == null) {
					solrFilters = new ArrayList<Query>();
					builder.setFilters(solrFilters);
				}

				if (1 == allRoleQueries.size()) {
					solrFilters.add(allRoleQueries.get(0));
				} else {
					BooleanQuery bq = new BooleanQuery(true);
					for (Query fq : allRoleQueries) {
						bq.add(fq, BooleanClause.Occur.SHOULD);
					}
					solrFilters.add(bq);
				}
			}
		} catch (SyntaxError e) {
			throw new SolrException(SolrException.ErrorCode.BAD_REQUEST, e);
		}
	}

	private String generateFilters(Map<String, String> outComponents, SolrQueryRequest req) {
		String[] roles = req.getParams().getParams(ROLE);

		StringBuffer masterQuery = new StringBuffer();
		if (roles != null) {
			LOG.debug("roles=" + Arrays.asList(roles));
			for (int i = 0; i < roles.length; i++) {
				String role = roles[i];

				String[] roleQueries = (String[]) filters.get(role);

				if (roleQueries != null) {
					String template = intersect(req, outComponents, PARAMETER_PREFIX + i + "_", roleQueries);
					if (template.length() > 0) {
						masterQuery.append(template).append(" ");
					}
				}
			}
		}

		if (masterQuery.length() == 0) {
			masterQuery.append(intersect(req, outComponents, PARAMETER_PREFIX, defaultFilters));
		}

		if (masterQuery.length() > 0) {
			masterQuery.insert(0, "{!q.op=OR}");
		}

		return masterQuery.toString();
	}

	private String intersect(SolrQueryRequest req, Map<String, String> outComponents, String paramPrefix, String[] queries) {
		StringBuffer fq = new StringBuffer("(");
		int paramIndex = 0;
		List<Query> parsedQueries = null;
		for (int i = 0; i < queries.length; i++) {
			try {
				parsedQueries = SolrPluginUtils.parseQueryStrings(req, queries);
			} catch (SyntaxError e) {
				LOG.error("Parse exception: " + e, e);
				return "";
			}

			LOG.debug("intersecting filter query:" + fq);
			String paramName = paramPrefix + paramIndex++;
			fq.append("+_query_:\"{!query v=$" + paramName + "}\" ");
			if (QueryUtils.isNegative((Query) parsedQueries.get(i)))
				outComponents.put(paramName, "(" + queries[i] + " *:*)");
			else {
				outComponents.put(paramName, queries[i]);
			}
		}

		fq.append(")");
		return fq.toString();
	}

	private List<Query> getFilters(SolrQueryRequest req) throws SyntaxError {
		List<Query> allRoleQueries = new ArrayList<Query>(5);

		String[] roles = req.getParams().getParams(ROLE);
		if (roles != null) {
			LOG.debug("roles=" + Arrays.asList(roles));

			for (String role : roles) {
				List<Query> roleQueries = SolrPluginUtils.parseQueryStrings(req, (String[]) filters.get(role));

				if (null != roleQueries) {
					if (1 == roleQueries.size()) {
						Query fq = (Query) roleQueries.get(0);
						LOG.debug("unioning role filter query:" + fq);
						allRoleQueries.add(fq);
					} else {
						BooleanQuery bq = new BooleanQuery(true);
						for (Query fq : roleQueries) {
							LOG.debug("intersecting role filter query:" + fq);
							bq.add(QueryUtils.makeQueryable(fq), BooleanClause.Occur.MUST);
						}
						allRoleQueries.add(bq);
					}
				}
			}

		}

		if (0 == allRoleQueries.size()) {
			for (String fq : defaultFilters) {
				LOG.debug("unioning default role filter:" + fq);
				allRoleQueries.add(QParser.getParser(fq, null, req).getQuery());
			}
		}
		return allRoleQueries;
	}

	public void process(ResponseBuilder builder) throws IOException {
	}

	public void modifyRequest(ResponseBuilder rb, SearchComponent who, ShardRequest sreq) {
		SolrQueryRequest req = rb.req;

		HashMap<String, String> components = new HashMap<String, String>();

		String template = generateFilters(components, req);
		if (template.length() > 0) {
			sreq.params.add("fq", new String[] { template });
		}
		for (Map.Entry<String, String> nameQueryEntry : components.entrySet()) {
			sreq.params.add((String) nameQueryEntry.getKey(), new String[] { (String) nameQueryEntry.getValue() });
		}

		sreq.params.remove(ROLE);
	}

	private boolean isDistrib(ResponseBuilder rb) {
		boolean distribFlag = rb.req.getParams().getBool("distrib", false);
		String shards = rb.req.getParams().get("shards");
		boolean hasShardURL = (shards != null) && (shards.indexOf(47) > 0);
		return hasShardURL | distribFlag;
	}
}
