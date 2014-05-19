package gaia.security;

import gaia.crawl.security.SecurityFilter;
import java.util.Set;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.NamedList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WindowsACLQueryFilterer extends ACLQueryFilterer {
	public static String ALLOWPREFIX = "WINA";
	public static String DENYPREFIX = "WIND";

	private Logger LOG = LoggerFactory.getLogger(WindowsACLQueryFilterer.class);

	private String fallbackQ = "-*:*";

	private String shouldClause = null;

	public SecurityFilter buildFilter(Set<String> tags, String localParamsPrefix) {
		if (((tags == null) || (tags.size() == 0)) && (fallbackQ != null) && (fallbackQ.trim().length() != 0)) {
			String filter = fallbackQ;
			return new SecurityFilter(filter);
		}

		StringBuffer sb = new StringBuffer();
		for (String tag : tags) {
			sb.append(" acl:" + ClientUtils.escapeQueryChars(new StringBuilder().append(ALLOWPREFIX).append(tag).toString()));
			sb.append(" -acl:" + ClientUtils.escapeQueryChars(new StringBuilder().append(DENYPREFIX).append(tag).toString()));
		}
		String filter;
		if (shouldClause == null) {
			if (sb.length() > 0) {
				sb.insert(0, "{!lucene q.op=OR}");
			}
			filter = sb.toString();
		} else {
			StringBuffer fq = new StringBuffer("{!lucene q.op=OR}");
			fq.append("(").append(shouldClause).append(")");
			if (sb.length() > 0) {
				fq.append(" (").append(sb).append(")");
			}
			filter = fq.toString();
		}

		return new SecurityFilter(filter);
	}

	public void init(NamedList arg0) {
		SolrParams params = SolrParams.toSolrParams(arg0);
		fallbackQ = params.get("fallback_query", "-*:*");
		LOG.info("fallbackQ set to {}.", fallbackQ);
		shouldClause = params.get("should_clause", null);
		LOG.info("shouldClause set to {}.", shouldClause);
	}
}
