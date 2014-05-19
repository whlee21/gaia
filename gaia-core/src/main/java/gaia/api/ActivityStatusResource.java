package gaia.api;

import java.text.ParseException;
import java.util.Map;
import org.apache.solr.client.solrj.SolrServerException;
import org.quartz.SchedulerException;
import org.restlet.resource.Get;

public interface ActivityStatusResource {
	@Get("json")
	public Map<String, Object> retrieve() throws SchedulerException, ParseException, SolrServerException;
}
