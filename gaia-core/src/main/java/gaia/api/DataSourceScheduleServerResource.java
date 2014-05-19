package gaia.api;

import gaia.admin.collection.AdminScheduler;
import gaia.admin.collection.CollectionManager;
import gaia.admin.collection.SchedParamUtil;
import gaia.admin.collection.Schedule;
import gaia.admin.collection.ScheduledSolrCommand;
import gaia.crawl.ConnectorManager;
import gaia.crawl.datasource.DataSource;
import gaia.crawl.datasource.DataSourceId;
import gaia.utils.StringUtils;

import java.io.IOException;
import java.util.ArrayList;
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

import com.google.inject.Inject;

public class DataSourceScheduleServerResource extends ServerResource implements DataSourceScheduleResource {
	public static final String TYPE = "type";
	public static final String ID = "id";
	private DataSource ds = null;
	private AdminScheduler adminScheduler;
	private CollectionManager cm;
	private ConnectorManager ccm;
	private String collection;
	public static final Set<String> VALID_FIELDS = new HashSet<String>();

	@Inject
	public DataSourceScheduleServerResource(CollectionManager cm, AdminScheduler scheduleHelper, ConnectorManager ccm) {
		this.cm = cm;
		adminScheduler = scheduleHelper;
		this.ccm = ccm;
	}

	public void doInit() throws ResourceException {
		String idStr = (String) getRequest().getAttributes().get("id");
		if (idStr == null) {
			setExisting(false);
			return;
		}
		collection = ((String) getRequestAttributes().get("coll_name"));
		DataSourceId dsId = new DataSourceId(idStr);
		try {
			ds = ccm.getDataSource(dsId);
		} catch (Exception e) {
		} finally {
			setExisting((ds != null) && (collection.equals(ds.getCollection())));
		}
		String col = collection;

		setExisting((cm.getCollection(col) != null) && (isExisting()));
	}

	@Put("json")
	public void update(Map<String, Object> m) throws IOException, SchedulerException {
		if (!isExisting()) {
			throw ErrorUtils.statusExp(Response.Status.NOT_FOUND, "URI not found");
		}
		if (m.size() == 0) {
			throw ErrorUtils.statusExp(422, "No input content found");
		}
		if (isExisting()) {
			List<Error> errors = new ArrayList<Error>();
			ScheduledSolrCommand cmd = cm.getScheduledSolrCommand(collection, ds.getDataSourceId().toString());
			Schedule schedule = cmd != null ? cmd.getSchedule() : null;
			if ((schedule == null) || (schedule.getStartTime() == null)) {
				schedule = new Schedule(ds.getDataSourceId().toString(), "crawl");
				errors.addAll(Schedule.validate(m, true));
			} else {
				errors.addAll(Schedule.validate(m, false));
			}
			errors.addAll(validate(m, false));

			if (errors.size() > 0) {
				throw ErrorUtils.statusExp(422, errors);
			}

			boolean active = true;
			if (m.get("active") != null) {
				active = StringUtils.getBoolean(m.get("active")).booleanValue();
			}

			adminScheduler.stop(collection, ds.getDataSourceId(), null);
			Schedule.fill(schedule, m);
			if (active) {
				if (m.containsKey("start_time"))
					adminScheduler.scheduleDataSource(collection, ds.getDataSourceId(), schedule, true);
				else {
					adminScheduler.scheduleDataSource(collection, ds.getDataSourceId(), schedule, false);
				}
			}
			if (cmd == null) {
				cmd = new ScheduledSolrCommand();
				cmd.setSchedule(schedule);
			} else {
				cmd.setSchedule(schedule);
			}
			SchedParamUtil.fillCrawlCommand(cmd, collection, ds.getDataSourceId().toString());

			cm.updateScheduledSolrCommand(collection, cmd, ds.getDataSourceId().toString());
			cm.save();
			setStatus(Status.SUCCESS_NO_CONTENT);
			AuditLogger.log("updated Datasource Schedule");
		} else {
			throw ErrorUtils.statusExp(Response.Status.NOT_FOUND, "DataSource schedule cannot be found");
		}
	}

	@Delete("json")
	public void remove() throws SchedulerException {
		if (!isExisting()) {
			throw ErrorUtils.statusExp(Response.Status.NOT_FOUND, "URI not found");
		}
		ScheduledSolrCommand cmd = cm.getScheduledSolrCommand(collection, ds.getDataSourceId().toString());
		if (cmd == null)
			;
		cm.removeScheduledSolrCommand(collection, ds.getDataSourceId().toString());
		adminScheduler.stop(collection, ds.getDataSourceId(), null);
		adminScheduler.deleteJob(AdminScheduler.getJobNameKey(ds.getDataSourceId(), null));
		setStatus(Status.SUCCESS_NO_CONTENT);
		AuditLogger.log("removed Datasource Schedule");
	}

	@Get("json")
	public Map<String, Object> retrieve() {
		if (!isExisting()) {
			throw ErrorUtils.statusExp(Response.Status.NOT_FOUND, "URI not found");
		}

		return toJSON(getSchedule());
	}

	protected Schedule getSchedule() {
		if (!isExisting()) {
			return null;
		}
		ScheduledSolrCommand cmd = cm.getScheduledSolrCommand(collection, ds.getDataSourceId().toString());
		return cmd != null ? cmd.getSchedule() : null;
	}

	protected Map<String, Object> toJSON(Schedule sched) {
		Map<String, Object> m = new HashMap<String, Object>();

		if (sched != null) {
			m.put("start_time", StringUtils.formatDate(sched.getStartTime()));
			m.put("period", Integer.valueOf(sched.getInterval()));
			m.put("type", sched.getActivity());
			m.put("active", Boolean.valueOf(sched.isActive()));
		} else {
			m.put("start_time", null);
			m.put("period", Integer.valueOf(0));
			m.put("type", "crawl");
			m.put("active", Boolean.valueOf(false));
		}

		return m;
	}

	public static List<Error> validate(Map<String, Object> m, boolean newResource) {
		List<Error> errors = new ArrayList<Error>();
		Set<String> keys = m.keySet();
		for (String key : keys) {
			if (!VALID_FIELDS.contains(key)) {
				errors.add(new Error(key, Error.E_FORBIDDEN_KEY, "Unknown or dissallowed key found:" + key));
			}
		}

		return errors;
	}

	static {
		VALID_FIELDS.add("start_time");
		VALID_FIELDS.add("period");
		VALID_FIELDS.add("active");
		VALID_FIELDS.add("type");
	}
}
