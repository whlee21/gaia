package gaia.api;

import java.util.List;
import java.util.Map;
import org.apache.solr.client.solrj.SolrServerException;
import org.restlet.resource.Get;

public interface ActivityHistoryResource {
	@Get("json")
	public List<Map<String, Object>> retrieve() throws SolrServerException;
}
