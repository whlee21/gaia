package gaia.api;

import gaia.admin.collection.AdminScheduler;
import gaia.admin.collection.CollectionManager;
import gaia.admin.collection.ScheduledSolrCommand;

import java.text.ParseException;
import java.util.Map;

import javax.ws.rs.core.Response;

import org.apache.solr.client.solrj.SolrServerException;
import org.quartz.SchedulerException;
import org.restlet.resource.Get;
import org.restlet.resource.ResourceException;
import org.restlet.resource.ServerResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;

public class ActivityStatusServerResource extends ServerResource implements ActivityStatusResource {
	private static transient Logger LOG = LoggerFactory.getLogger(ActivityStatusServerResource.class);
	static final String ID = "id";
	private ScheduledSolrCommand cmd;
	private String id;
	private CollectionManager cm;
	private String collection;
	private AdminScheduler adminScheduler;

	@Inject
	public ActivityStatusServerResource(CollectionManager cm, AdminScheduler adminScheduler) {
		this.cm = cm;
		this.adminScheduler = adminScheduler;
	}

	public void doInit() throws ResourceException {
		collection = ((String) getRequestAttributes().get("coll_name"));

		setExisting(cm.getCollectionNames().contains(collection));
		try {
			id = ((String) getRequest().getAttributes().get("id"));
			cmd = cm.getScheduledSolrCommand(collection, id);
		} catch (NumberFormatException e) {
		} finally {
			setExisting((cmd != null) && (isExisting()));
		}
	}

	@Get("json")
	public Map<String, Object> retrieve() throws SchedulerException, ParseException, SolrServerException {
		if (!isExisting()) {
			throw ErrorUtils.statusExp(Response.Status.NOT_FOUND, "URI not found");
		}
		return adminScheduler.getStatus(cmd);
	}
}
