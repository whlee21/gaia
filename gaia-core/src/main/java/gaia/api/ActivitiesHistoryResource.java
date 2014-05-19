package gaia.api;

import java.util.List;
import org.apache.solr.client.solrj.SolrServerException;
import org.restlet.resource.Get;

public interface ActivitiesHistoryResource {
	@Get("json")
	public List retrieve() throws SolrServerException;
}
