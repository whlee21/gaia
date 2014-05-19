package gaia.api;

import gaia.admin.collection.AdminScheduler;
import gaia.admin.collection.CollectionManager;
import gaia.admin.collection.SchedParamUtil;
import gaia.admin.collection.Schedule;
import gaia.admin.collection.ScheduledSolrCommand;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.Response;

import org.apache.solr.core.CoreContainer;
import org.restlet.data.Status;
import org.restlet.resource.Get;
import org.restlet.resource.Post;
import org.restlet.resource.ResourceException;
import org.restlet.resource.ServerResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;

public class ActivitiesServerResource extends ServerResource implements ActivitiesResource {
	private static transient Logger LOG = LoggerFactory.getLogger(ActivitiesServerResource.class);
	private AdminScheduler adminScheduler;
	private CollectionManager cm;
	private String collection;
	private CoreContainer cores;

	@Inject
	public ActivitiesServerResource(CollectionManager cm, CoreContainer cores, AdminScheduler adminScheduler) {
		this.adminScheduler = adminScheduler;
		this.cm = cm;
		this.cores = cores;
	}

	public void doInit() throws ResourceException {
		collection = ((String) getRequestAttributes().get("coll_name"));
		setExisting(APIUtils.coreExists(cores, collection));
	}

	@Post("json")
	public Map<String, Object> add(Map<String, Object> m) {
		if (!isExisting()) {
			throw ErrorUtils.statusExp(Response.Status.NOT_FOUND, "URI not found");
		}
		if (m.size() == 0) {
			 throw ErrorUtils.statusExp(422, "No input content found");
		}

		List<Error> errors = new ArrayList<Error>();
		errors.addAll(ActivityServerResource.validate(m, true));

		errors.addAll(Schedule.validate(m, true));

		if (errors.size() > 0) {
			throw ErrorUtils.statusExp(422, errors);
		}

		ScheduledSolrCommand cmd = new ScheduledSolrCommand();
		ActivityServerResource.fill(cmd, m);
		SchedParamUtil.addParams(cmd, collection, cm);
		cm.addScheduledSolrCommand(collection, cmd, cmd.getId().toString());

		adminScheduler.schedule(collection, cmd, true);

		getResponse().setLocationRef("activities/" + cmd.getId());
		setStatus(Status.SUCCESS_CREATED);
		AuditLogger.log("added scheduled command");
		return ActivityServerResource.toJSON(cmd);
	}

	@Get("json")
	public List<Map<String,Object>> retrieve() {
		List<Map<String,Object>> result = new ArrayList<Map<String,Object>>();
		for (ScheduledSolrCommand cmd : cm.getAllScheduledSolrCommands(collection)) {
			result.add(ActivityServerResource.toJSON(cmd));
		}

		return result;
	}
}
