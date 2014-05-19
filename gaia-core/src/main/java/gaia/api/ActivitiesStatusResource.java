package gaia.api;

import java.text.ParseException;
import java.util.List;
import java.util.Map;
import org.apache.solr.client.solrj.SolrServerException;
import org.quartz.SchedulerException;
import org.restlet.resource.Get;

public interface ActivitiesStatusResource {
	@Get("json")
	public List<Map<String, Object>> retrieve() throws SolrServerException, SchedulerException, ParseException;
}
