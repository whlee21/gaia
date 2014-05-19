package gaia.api;

import gaia.admin.collection.AdminScheduler;
import gaia.admin.collection.CollectionManager;
import gaia.admin.collection.SchedParamUtil;
import gaia.admin.collection.Schedule;
import gaia.admin.collection.ScheduledSolrCommand;
import gaia.common.params.SchedulingParams;
import gaia.utils.StringUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.core.Response;

import org.quartz.SchedulerException;
import org.restlet.data.Status;
import org.restlet.resource.Delete;
import org.restlet.resource.Get;
import org.restlet.resource.Put;
import org.restlet.resource.ResourceException;
import org.restlet.resource.ServerResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;

public class ActivityServerResource extends ServerResource implements ActivityResource {
	static final String ID = "id";
	public static final String TYPE = "type";
	private static transient Logger LOG = LoggerFactory.getLogger(ActivityServerResource.class);

	private ScheduledSolrCommand cmd = null;
	private String id;
	private AdminScheduler adminScheduler;
	private CollectionManager cm;
	private String collection;
	public static final Set<String> VALID_FIELDS = new HashSet<String>();

	@Inject
	public ActivityServerResource(CollectionManager cm, AdminScheduler adminScheduler) {
		this.adminScheduler = adminScheduler;
		this.cm = cm;
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

	@Delete
	public void remove() throws SchedulerException {
		if (!isExisting()) {
			throw ErrorUtils.statusExp(Response.Status.NOT_FOUND, "URI not found");
		}
		if (cmd != null) {
			adminScheduler.stop(collection, cmd);
			cm.removeScheduledSolrCommand(collection, id);
			AuditLogger.log("removed scheduled command");
		}
		cm.save();
		setStatus(Status.SUCCESS_NO_CONTENT);
	}

	@Put("json")
	public void update(Map<String, Object> m) throws IOException, SchedulerException {
		if (!isExisting()) {
			throw ErrorUtils.statusExp(Response.Status.NOT_FOUND, "URI not found");
		}
		if (m.size() == 0) {
			throw ErrorUtils.statusExp(422, "No input content found");
		}

		List<Error> errors = new ArrayList<Error>();
		errors.addAll(validate(m, false));

		errors.addAll(Schedule.validate(m, false));

		if (errors.size() > 0) {
			throw ErrorUtils.statusExp(422, errors);
		}

		if (isExisting()) {
			fill(cmd, m);
			SchedParamUtil.addParams(cmd, collection, cm);

			adminScheduler.stop(collection, cmd);

			cm.updateScheduledSolrCommand(collection, cmd, id);

			if (m.containsKey("start_time"))
				adminScheduler.schedule(collection, cmd, true);
			else {
				adminScheduler.schedule(collection, cmd, false);
			}

			cm.save();

			AuditLogger.log("updated scheduled command");
			setStatus(Status.SUCCESS_NO_CONTENT);
		} else {
			throw ErrorUtils.statusExp(Response.Status.NOT_FOUND, "Could not find activity with the given id:" + id);
		}
	}

	@Get("json")
	public Map<String, Object> retrieve() {
		if (!isExisting()) {
			throw ErrorUtils.statusExp(Response.Status.NOT_FOUND, "URI not found");
		}
		return toJSON(cmd);
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
				errors.add(new Error("id", Error.E_FORBIDDEN_VALUE, "You cannot update the id"));
			}

			if (type != null) {
				errors.add(new Error("type", Error.E_FORBIDDEN_VALUE, "You cannot change an Activities type"));
			}
		}

		if ((type != null) && (!SchedParamUtil.ACT_TYPES.contains(type))) {
			errors.add(new Error("type", Error.E_INVALID_VALUE, "Unknown type:" + type));
		}

		Set<String> keys = m.keySet();
		for (String key : keys) {
			if (!VALID_FIELDS.contains(key)) {
				errors.add(new Error(key, Error.E_FORBIDDEN_KEY, "Unknown or dissallowed key found:" + key));
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
