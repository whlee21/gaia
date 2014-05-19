package gaia.crawl.security;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import gaia.crawl.ConnectorManager;
import gaia.crawl.DataSourceManager;
import gaia.crawl.datasource.DataSource;
import gaia.crawl.datasource.DataSourceId;

@Singleton
public class ConnectorsSecurityEnforcer {
	private static final Logger LOG = LoggerFactory.getLogger(ConnectorsSecurityEnforcer.class);
	private DataSourceManager dm;
	private ConnectorManager cm;

	@Inject
	public ConnectorsSecurityEnforcer(DataSourceManager dm, ConnectorManager cm) {
		this.dm = dm;
		this.cm = cm;
	}

	public SecurityFilter buildSecurityFilter(String collection, String user) {
		List<DataSource> dataSources = dm.getDataSources();
		Map<DataSourceId, SecurityFilter> filters = new HashMap<DataSourceId, SecurityFilter>();
		for (DataSource ds : dataSources) {
			if ((collection.equals(ds.getCollection())) && (ds.isSecurityTrimmingEnabled())) {
				try {
					SecurityFilter dsFilter = cm.buildSecurityFilter(ds.getDataSourceId(), user);
					if (dsFilter != null)
						filters.put(ds.getDataSourceId(), dsFilter);
				} catch (Exception e) {
					LOG.error(
							new StringBuilder().append("Got exception when building security filter for data source ")
									.append(ds.getDataSourceId().getId()).toString(), e);
				}
			}
		}

		if (filters.isEmpty()) {
			return null;
		}
		Map<String, String> nestedClauses = new HashMap<String, String>();
		StringBuilder sb = new StringBuilder("{!lucene q.op=AND}");
		for (Map.Entry<DataSourceId, SecurityFilter> entry : filters.entrySet()) {
			String filterClause = new StringBuilder().append("ds_").append(((DataSourceId) entry.getKey()).getId())
					.toString();
			nestedClauses.put(filterClause, entry.getValue().getFilter());
			nestedClauses.putAll(((SecurityFilter) entry.getValue()).getNestedClauses());
			String wrappedFilter = new StringBuilder().append("{!lucene q.op=OR} (*:* -data_source:")
					.append(((DataSourceId) entry.getKey()).getId()).append(") (+data_source:")
					.append(((DataSourceId) entry.getKey()).getId()).append(" +_query_:\"{!query v=$").append(filterClause)
					.append("}\")").toString();

			String wrappedClause = new StringBuilder().append("wrapped_ds_").append(((DataSourceId) entry.getKey()).getId())
					.toString();
			sb.append(new StringBuilder().append(" _query_:\"{!query v=$").append(wrappedClause).append("}\"").toString());
			nestedClauses.put(wrappedClause, wrappedFilter);
		}

		return new SecurityFilter(sb.toString(), nestedClauses);
	}
}
