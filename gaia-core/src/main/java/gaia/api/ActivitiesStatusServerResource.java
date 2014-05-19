package gaia.api;

import gaia.admin.collection.AdminScheduler;
import gaia.admin.collection.CollectionManager;
import gaia.admin.collection.ScheduledSolrCommand;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.Response;

import org.apache.solr.client.solrj.SolrServerException;
import org.quartz.SchedulerException;
import org.restlet.resource.Get;
import org.restlet.resource.ResourceException;
import org.restlet.resource.ServerResource;

import com.google.inject.Inject;

public class ActivitiesStatusServerResource extends ServerResource implements ActivitiesStatusResource {
	private CollectionManager cm;
	private AdminScheduler adminScheduler;
	private String collection;

	@Inject
	public ActivitiesStatusServerResource(CollectionManager cm, AdminScheduler adminScheduler) {
		this.cm = cm;
		this.adminScheduler = adminScheduler;
	}

	public void doInit() throws ResourceException {
		collection = ((String) getRequestAttributes().get("coll_name"));

		setExisting(cm.getCollectionNames().contains(collection));
	}

	@Get("json")
	public List<Map<String, Object>> retrieve() throws SolrServerException, SchedulerException, ParseException {
		if (!isExisting()) {
			throw ErrorUtils.statusExp(Response.Status.NOT_FOUND, "URI not found");
		}
		List<Map<String, Object>> activitiesStatus = new ArrayList<Map<String, Object>>();

		for (ScheduledSolrCommand cmd : cm.getAllScheduledSolrCommands(collection)) {
			activitiesStatus.add(adminScheduler.getStatus(cmd));
		}

		return activitiesStatus;
	}
}
