package gaia.search.server.api.services;

import gaia.admin.collection.AdminScheduler;
import gaia.admin.collection.CollectionManager;
import gaia.admin.collection.SchedParamUtil;
import gaia.admin.collection.Schedule;
import gaia.admin.collection.ScheduledSolrCommand;
import gaia.admin.collection.SolrCmdHistory;
import gaia.api.APIUtils;
import gaia.api.AuditLogger;
import gaia.api.Error;
import gaia.api.ErrorUtils;
import gaia.api.ObjectSerializer;
import gaia.common.params.SchedulingParams;
import gaia.search.server.api.services.parsers.BodyParseException;
import gaia.search.server.api.services.parsers.RequestBodyParser;
import gaia.search.server.configuration.Configuration;
import gaia.utils.StringUtils;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.solr.core.CoreContainer;
import org.apache.solr.core.SolrCore;
import org.quartz.SchedulerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xnap.commons.i18n.I18nFactory;

public class ActivityService extends BaseService {
	
	private static org.xnap.commons.i18n.I18n i18n = I18nFactory.getI18n(ActivityService.class);
	private static final Logger LOG = LoggerFactory.getLogger(ActivityService.class);

	private CollectionManager cm;
	private String collection;
	private SolrCore core;
	private SolrCmdHistory cmdHistory;
	private CoreContainer cores;
	private AdminScheduler adminScheduler;
	private Configuration configuration;
	private volatile boolean existing = true;
	private ScheduledSolrCommand cmd;
	public static final Set<String> VALID_FIELDS = new HashSet<String>();
	
	public ActivityService(ObjectSerializer serializer, RequestBodyParser bodyParser, String collectionName, CollectionManager cm, CoreContainer cores,
			SolrCmdHistory cmdHistory, AdminScheduler adminScheduler, Configuration configuration) {
		super(serializer, bodyParser);
		this.collection= collectionName;
		this.cm = cm;
		this.cores = cores;
		this.cmdHistory = cmdHistory;
		this.adminScheduler = adminScheduler;
		this.configuration = configuration;
		setExisting(APIUtils.coreExists(cores, collection));
	}

	public void setExisting(boolean exists) {
		this.existing = exists;
	}
	

	public boolean isExisting() {
		return this.existing;
	}
	
	@GET
	@Produces("text/plain")
	public Response getActivities(@Context HttpHeaders headers, @Context UriInfo ui) {
		List<Map<String,Object>> result = new ArrayList<Map<String,Object>>();
		for (ScheduledSolrCommand cmd : cm.getAllScheduledSolrCommands(collection)) {
			Map<String, Object> m = toJSON(cmd.getSchedule());
			m.put("id", cmd.getId());
			m.put("type", cmd.getName());
			result.add(m);
		}

		return buildResponse(Response.Status.OK, result);
	}

	protected static Map<String, Object> toJSON(ScheduledSolrCommand cmd) {
		Map<String, Object> m = toJSON(cmd.getSchedule());
		m.put("id", cmd.getId());
		m.put("type", cmd.getName());
		return m;
	}

	protected static Map<String, Object> toJSON(Schedule sched) {
		Map<String, Object> m = new HashMap<String, Object>();

		m.put("start_time", StringUtils.formatDate(sched.getStartTime()));
		m.put("period", Integer.valueOf(sched.getInterval()));
		m.put("active", Boolean.valueOf(sched.isActive()));

		return m;
	}

	@GET
	@Path("{activityId}")
	@Produces("text/plain")
	public Response getActivity(@Context HttpHeaders headers, @Context UriInfo ui,
			@PathParam("activityId") String activityId) {

		
		setExisting(cm.getCollectionNames().contains(collection));
		try {
			cmd = cm.getScheduledSolrCommand(collection, activityId);
		} catch (NumberFormatException e) {
		} finally {
			setExisting((cmd != null) && (isExisting()));
		}
		
		if (!isExisting()) {
			throw ErrorUtils.statusExp(Response.Status.NOT_FOUND, i18n.tr("URI not found"));
		}
		Map<String, Object> cmdMap = toJSON(cmd);

		return buildResponse(Response.Status.OK, cmdMap);
	}

	@POST
	@Produces("text/plain")
	public Response createCollectionActivities(String body, @Context HttpHeaders headers, @Context UriInfo ui) throws UnsupportedEncodingException, IOException, URISyntaxException {
		try {
			RequestBody requestBody = getRequestBody(body);
			return (Response) add(requestBody.getProperties());
		} catch (BodyParseException e) {
			throw ErrorUtils.statusExp(Response.Status.INTERNAL_SERVER_ERROR, new Error("body", Error.E_EXCEPTION,
					i18n.tr("cannot parse body " + body)));
		}
		
	}
	
	public Response add(Map<String, Object> m) throws UnsupportedEncodingException, IOException, URISyntaxException {
		if (!isExisting()) {
			throw ErrorUtils.statusExp(Response.Status.NOT_FOUND, i18n.tr("URI not found"));
		}
		if (m.size() == 0) {
			 throw ErrorUtils.statusExp(422, i18n.tr("No input content found"));
		}

		List<Error> errors = new ArrayList<Error>();
		errors.addAll(validate(m, true));

		errors.addAll(Schedule.validate(m, true));

		if (errors.size() > 0) {
			throw ErrorUtils.statusExp(422, errors);
		}

		ScheduledSolrCommand cmd = new ScheduledSolrCommand();
		fill(cmd, m);
		SchedParamUtil.addParams(cmd, collection, cm);
		cm.addScheduledSolrCommand(collection, cmd, cmd.getId().toString());

		adminScheduler.schedule(collection, cmd, true);

		//getResponse().setLocationRef("activities/" + cmd.getId());
		//AuditLogger.log("added scheduled command");

LOG.debug("configuration: "+configuration);
LOG.debug("cmd: "+cmd);
		URI seeOther = configuration.getCollectionUri(collection + "/activities/" + URLEncoder.encode(cmd.getId(), "UTF-8"));
		Response response = buildResponse(Response.Status.CREATED, seeOther, toJSON(cmd));
		return response;
	}

	@PUT
	@Path("{activityId}")
	@Produces("text/plain")
	public Response updateCollectionActivities(String body, @Context HttpHeaders headers, @Context UriInfo ui,
			@PathParam("activityId") String activityId) throws SchedulerException, BodyParseException {
		if (!isExisting()) {
			throw ErrorUtils.statusExp(Response.Status.NOT_FOUND, i18n.tr("URI not found"));
		}
		

		RequestBody requestBody = getRequestBody(body);
		Map<String, Object> m = (requestBody.getProperties());
		if (m.size() == 0) {
			throw ErrorUtils.statusExp(422, i18n.tr("No input content found"));
		}

		List<Error> errors = new ArrayList<Error>();
		errors.addAll(validate(m, false));

		errors.addAll(Schedule.validate(m, false));

		if (errors.size() > 0) {
			throw ErrorUtils.statusExp(422, errors);
		}

		if (isExisting()) {
			cmd = cm.getScheduledSolrCommand(collection, activityId);
			fill(cmd, m);
			SchedParamUtil.addParams(cmd, collection, cm);

			adminScheduler.stop(collection, cmd);

			cm.updateScheduledSolrCommand(collection, cmd, activityId);

			if (m.containsKey("start_time"))
				adminScheduler.schedule(collection, cmd, true);
			else {
				adminScheduler.schedule(collection, cmd, false);
			}

			cm.save();

			AuditLogger.log("updated scheduled command");
		} else {
			throw ErrorUtils.statusExp(Response.Status.NOT_FOUND, i18n.tr("Could not find activity with the given id:" + activityId));
		}
		return buildResponse(Response.Status.NO_CONTENT);
	}

	@DELETE
	@Path("{activityId}")
	@Produces("text/plain")
	public Response deleteCollectionActivities(@Context HttpHeaders headers, @Context UriInfo ui,
			@PathParam("activityId") String activityId) throws SchedulerException {

		if (!isExisting()) {
			throw ErrorUtils.statusExp(Response.Status.NOT_FOUND, i18n.tr("URI not found"));
		}
		cmd = cm.getScheduledSolrCommand(collection, activityId);
		if (cmd != null) {
			adminScheduler.stop(collection, cmd);
			cm.removeScheduledSolrCommand(collection, activityId);
			AuditLogger.log(i18n.tr("removed scheduled command"));
		}
		cm.save();
		return buildResponse(Response.Status.NO_CONTENT);
	}

	@Path("{activityId}/status")
	public ActivityStatusService getActivityStatusHandler(@PathParam("activityId") String activityId) {
		 return new ActivityStatusService(serializer, bodyParser, collection,
					activityId,	cm, adminScheduler);
	}

	@Path("all/history")
	public ActivityHistoryService getAllActivityHistoryHandler() {
		LOG.debug("$$$#####  activities/all/history called....");
		 return new ActivityHistoryService(serializer, bodyParser, collection,
					"all", cm, cores, cmdHistory);
	}

	@Path("{activityId}/history")
	public ActivityHistoryService getActivityHistoryHandler(@PathParam("activityId") String activityId) {
		 return new ActivityHistoryService(serializer, bodyParser, collection,
					activityId, 	cm, cores, cmdHistory);
	}

	// protected ResourceInstance createActivityResource(String collectionName,
	// String activityId) {
	// Map<Resource.Type, String> mapIds = new HashMap<Resource.Type, String>();
	// mapIds.put(Resource.Type.Collection, collectionName);
	// mapIds.put(Resource.Type.Activity, activityId);
	// return createResource(Resource.Type.Activity, mapIds);
	// }

	protected static void fill(ScheduledSolrCommand cmd, Map<String, Object> m) {
		if (cmd.getSchedule() == null)
			cmd.setSchedule(new Schedule());
		Schedule schedule = cmd.getSchedule();
		if (m.containsKey("type")) {
			String type = (String) m.get("type");
			cmd.setName(type);
			schedule.setActivity(type);
		}
		fill(schedule, m);
		cmd.setSchedule(schedule);
	}

	protected static void fill(Schedule sched, Map<String, Object> m) {
		if (m.containsKey("start_time")) {
			sched.setStartTime((Date) m.get("start_time"));
		}
		if (m.containsKey("period")) {
			Integer period = Integer.valueOf(Integer.parseInt(m.get("period").toString()));
			sched.setInterval(period.intValue());
			sched.setRepeatUnit(SchedulingParams.JobRepeatUnits.SECOND);
		}
		if (m.containsKey("active")) {
			boolean active = StringUtils.getBoolean(m.get("active")).booleanValue();
			if (!active)
				sched.setEndTime(new Date());
			else {
				sched.setEndTime(null);
			}
			sched.setActive(active);
		}
	}

	static List<Error> validate(Map<String, Object> m, boolean newActivity) {
		List<Error> errors = new ArrayList<Error>();
		String type = (String) m.get("type");

		if (newActivity) {
			if (type == null)
				errors.add(new Error("type", Error.E_MISSING_VALUE));
		} else {
			if (m.get("id") != null) {
				errors.add(new Error("id", Error.E_FORBIDDEN_VALUE, i18n.tr("You cannot update the id")));
			}

			if (type != null) {
				errors.add(new Error("type", Error.E_FORBIDDEN_VALUE, i18n.tr("You cannot change an Activities type")));
			}
		}

		if ((type != null) && (!SchedParamUtil.ACT_TYPES.contains(type))) {
			errors.add(new Error("type", Error.E_INVALID_VALUE, i18n.tr("Unknown type:" + type)));
		}

		Set<String> keys = m.keySet();
		for (String key : keys) {
			if (!VALID_FIELDS.contains(key)) {
				errors.add(new Error(key, Error.E_FORBIDDEN_KEY, i18n.tr("Unknown or dissallowed key found:" + key)));
			}
		}

		return errors;
	}

	static {
		VALID_FIELDS.add("active");
		VALID_FIELDS.add("type");
		VALID_FIELDS.add("period");
		VALID_FIELDS.add("start_time");
	}
}