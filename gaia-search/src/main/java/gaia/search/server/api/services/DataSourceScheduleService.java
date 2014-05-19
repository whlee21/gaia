package gaia.search.server.api.services;

import gaia.admin.collection.AdminScheduler;
import gaia.admin.collection.CollectionManager;
import gaia.admin.collection.SchedParamUtil;
import gaia.admin.collection.Schedule;
import gaia.admin.collection.ScheduledSolrCommand;
import gaia.api.Error;
import gaia.api.ErrorUtils;
import gaia.api.ObjectSerializer;
import gaia.crawl.ConnectorManager;
import gaia.crawl.datasource.DataSource;
import gaia.crawl.datasource.DataSourceId;
import gaia.search.server.api.services.parsers.BodyParseException;
import gaia.search.server.api.services.parsers.RequestBodyParser;
import gaia.utils.StringUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.quartz.SchedulerException;
import org.xnap.commons.i18n.I18nFactory;

public class DataSourceScheduleService  extends BaseService  {

	private static org.xnap.commons.i18n.I18n i18n = I18nFactory.getI18n(DataSourceScheduleService.class);
	private String collection;
	private String dataSourceId;
	private volatile boolean existing = true;
	public static final String TYPE = "type";
	public static final String ID = "id";
	private DataSource ds = null;
	private AdminScheduler adminScheduler;
	private CollectionManager cm;
	private ConnectorManager ccm;
	public static final Set<String> VALID_FIELDS = new HashSet<String>();

	public DataSourceScheduleService(ObjectSerializer serializer, RequestBodyParser bodyParser, String collectionName, String dataSourceId,
			AdminScheduler scheduleHelper, ConnectorManager ccm, CollectionManager cm) {
		super(serializer, bodyParser);
		this.collection= collectionName;
		this.dataSourceId = dataSourceId;
		this.adminScheduler = scheduleHelper;
		this.cm = cm;
		this.ccm = ccm;
	}
	

	public void setExisting(boolean exists) {
		this.existing = exists;
	}
	

	public boolean isExisting() {
		return this.existing;
	}
	
	public void doInit() {
		String idStr = dataSourceId;
		if (idStr == null) {
			setExisting(false);
			return;
		}
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
	@GET
	@Produces("text/plain")
	public Response getDataSourceSchedule(@Context HttpHeaders headers, @Context UriInfo ui) {

		doInit();
		Map<String, Object> result = retrieve();
		return buildResponse(Response.Status.OK, result);
	}
	
	public Map<String, Object> retrieve() {
		if (!isExisting()) {
			throw ErrorUtils.statusExp(Response.Status.NOT_FOUND, i18n.tr("URI not found"));
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
				errors.add(new Error(key, Error.E_FORBIDDEN_KEY, i18n.tr("Unknown or dissallowed key found:" + key)));
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
	
	@PUT
	@Produces("text/plain")
	public Response updateCollectionActivities(String body, @Context HttpHeaders headers, @Context UriInfo ui) throws IOException, SchedulerException {
//		return handleRequest(headers, body, ui, Request.Type.PUT, createDataSourceScheduleResource(collectionName, dataSourceId));
		doInit();
		RequestBody requestBody = null;
		try {
			requestBody = getRequestBody(body);
//			return createDataSource(requestBody.getProperties());
		} catch (BodyParseException e) {
			throw ErrorUtils.statusExp(Response.Status.INTERNAL_SERVER_ERROR, new Error("body", Error.E_EXCEPTION,
					i18n.tr("cannot parse body " + body)));
		}
		Map<String, Object> map = requestBody.getProperties();
		Response resultVal = update(map);
		
		return resultVal;
	}
	
	public Response update(Map<String, Object> m) throws IOException, SchedulerException {
		if (!isExisting()) {
			throw ErrorUtils.statusExp(Response.Status.NOT_FOUND, i18n.tr("URI not found"));
		}
		if (m.size() == 0) {
			throw ErrorUtils.statusExp(422, i18n.tr("No input content found"));
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
			return buildResponse(Response.Status.NO_CONTENT);
		} else {
			throw ErrorUtils.statusExp(Response.Status.NOT_FOUND, i18n.tr("DataSource schedule cannot be found"));
		}
	}
}
