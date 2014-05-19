package gaia.admin.collection;

import gaia.UUIDObject;
import gaia.common.params.SchedulingParams;
import gaia.api.Error;
import gaia.utils.StringUtils;

import java.io.Serializable;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class Schedule extends UUIDObject implements Serializable {
	private static final Pattern INT_STRING = Pattern.compile("\\d+");
	private static final long FIVE_MIN = 300000L;
	public static final String START_TIME = "start_time";
	public static final String ACTIVE = "active";
	public static final String PERIOD = "period";
	protected Date startTime;
	protected Date endTime;
	protected SchedulingParams.JobRepeatUnits repeatUnit = SchedulingParams.JobRepeatUnits.DAY;
	protected int interval = 0;
	protected boolean active = false;
	protected String triggerName;
	protected String activity;

	public Schedule() {
	}

	public Schedule(String id, String activity) {
		this.id = id;
		this.activity = activity;
	}

	public Schedule(String activity) {
		this.activity = activity;
	}

	public Schedule(Date startTime, int interval) {
		this.startTime = startTime;
		this.interval = interval;
	}

	public Schedule(Schedule other) {
		id = other.id;
		active = other.active;
		activity = other.activity;
		createDate = other.createDate;
		endTime = other.endTime;
		interval = other.interval;
		lastModified = other.lastModified;
		repeatUnit = other.repeatUnit;
		startTime = other.startTime;
		triggerName = other.triggerName;
	}

	public Date getEndTime() {
		return endTime;
	}

	public void setEndTime(Date endTime) {
		this.endTime = endTime;
	}

	public SchedulingParams.JobRepeatUnits getRepeatUnit() {
		return repeatUnit;
	}

	public void setRepeatUnit(SchedulingParams.JobRepeatUnits repeatUnit) {
		this.repeatUnit = repeatUnit;
	}

	public Date getStartTime() {
		return startTime;
	}

	public void setStartTime(Date startTime) {
		this.startTime = startTime;
	}

	public String getTriggerName() {
		return triggerName;
	}

	public void setTriggerName(String triggerName) {
		this.triggerName = triggerName;
	}

	public boolean isActive() {
		return active;
	}

	public void setActive(boolean active) {
		this.active = active;
	}

	public int getInterval() {
		return interval;
	}

	public void setInterval(int interval) {
		this.interval = interval;
	}

	public boolean equals(Object o) {
		if (this == o)
			return true;
		if ((o == null) || (getClass() != o.getClass()))
			return false;
		if (!super.equals(o))
			return false;

		Schedule schedule = (Schedule) o;

		if (startTime != null ? !startTime.equals(schedule.startTime) : schedule.startTime != null)
			return false;
		if (endTime != null ? !endTime.equals(schedule.startTime) : schedule.endTime != null)
			return false;
		if (interval != schedule.interval)
			return false;
		if (repeatUnit != schedule.repeatUnit)
			return false;
		if (activity != null ? !activity.equals(schedule.activity) : schedule.activity != null)
			return false;
		if (triggerName != null ? !triggerName.equals(schedule.triggerName) : schedule.triggerName != null)
			return false;

		return true;
	}

	public int hashCode() {
		int result = super.hashCode();
		result = 31 * result + (startTime != null ? startTime.hashCode() : 0);
		result = 31 * result + (endTime != null ? endTime.hashCode() : 0);
		result = 31 * result + interval;
		result = 31 * result + repeatUnit.hashCode();
		result = 31 * result + (activity != null ? activity.hashCode() : 0);
		result = 31 * result + (triggerName != null ? triggerName.hashCode() : 0);
		return result;
	}

	public String toString() {
		return "Schedule{startTime=" + startTime + ", endTime=" + endTime + ", repeatUnit=" + repeatUnit + ", interval="
				+ interval + ", activity='" + activity + '\'' + ", active='" + active + '\'' + ", triggerName='" + triggerName
				+ '\'' + '}';
	}

	public String getActivity() {
		return activity;
	}

	public void setActivity(String activity) {
		this.activity = activity;
	}

	public boolean activityIsCrawl() {
		return (activity != null) && (activity.equalsIgnoreCase("crawl"));
	}

	public boolean activityIsIndex() {
		return (activity != null) && (activity.equalsIgnoreCase("index"));
	}

	public static void fill(Schedule sched, Map<String, Object> m) {
		if (m.containsKey("start_time")) {
			sched.setStartTime((Date) m.get("start_time"));
		}
		if (m.containsKey("period")) {
			sched.setInterval(((Integer) m.get("period")).intValue());
			sched.setRepeatUnit(SchedulingParams.JobRepeatUnits.SECOND);
		}
		if (m.containsKey("type")) {
			sched.setActivity((String) m.get("type"));
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

	public static List<Error> validate(Map<String, Object> m, boolean newResource) {
		List<Error> errors = new ArrayList<Error>();

		if ((newResource) && (!m.containsKey("start_time"))) {
			errors.add(new Error("start_time", Error.E_MISSING_VALUE));
		}

		try {
			if (m.containsKey("start_time")) {
				Object startTimeObject = m.get("start_time");
				if (startTimeObject.equals("now")) {
					m.put("start_time", new Date());
				} else if (INT_STRING.matcher(startTimeObject.toString()).matches()) {
					int offset = Integer.parseInt(startTimeObject.toString());
					Date offsetDate = new Date();
					offsetDate.setSeconds(offsetDate.getSeconds() + offset);
					m.put("start_time", offsetDate);
				} else {
					Date startTime = StringUtils.parseDate(startTimeObject.toString());
					Date now = new Date();
					now.setTime(now.getTime() - 300000L);
					if (!startTime.after(now)) {
						errors.add(new Error("start_time", Error.E_INVALID_VALUE, "start_time is in the past"));
					}
					m.put("start_time", startTime);
				}
			}
		} catch (ParseException e) {
			errors.add(new Error("start_time", Error.E_INVALID_VALUE, "start_time could not be parsed as a date: "
					+ m.get("start_time")));

			return errors;
		}

		Object period = m.get("period");
		if (period != null) {
			try {
				int p = Integer.parseInt(period.toString());
				if (p < 0)
					errors.add(new Error("period", Error.E_INVALID_VALUE, "Period must be a positive integer: "
							+ period.toString()));
				else
					m.put("period", Integer.valueOf(p));
			} catch (NumberFormatException e) {
				errors
						.add(new Error("period", Error.E_INVALID_VALUE, "Could not parse period as integer:" + period.toString()));
			}
		}

		return errors;
	}
}
