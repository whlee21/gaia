package gaia.common.params;

public interface SchedulingParams {
	public static final String JOB_KEY = "jobKey";
	public static final String JOB_START_DATE = "jobStartDate";
	public static final String JOB_END_DATE = "jobEndDate";
	public static final String JOB_REPEAT = "jobRepeat";
	public static final String JOB_REPEAT_INTERVAL = "jobRepeatInterval";
	public static final String JOB_REPEAT_COUNT = "jobRepeatCount";
	public static final String JOB_REPEAT_UNITS = "jobRepeatUnits";
	public static final String JOB_PRIORITY = "jobPriority";
	public static final String JOB_HANDLER = "jobHandler";
	public static final String JOB_PARAMS = "jobParams";
	public static final String JOB_SCHEDULE_DATE = "jobScheduleDate";
	public static final String CONTENT_STREAMS = "streams";
	public static final String JOB_DETAIL = "jobDetail";
	public static final String TRIGGER_KEY = "triggerKey";
	public static final String TRIGGER_NAME = "triggerName";
	public static final String JOB_TRIGGERS = "jobTriggers";
	public static final String TRIGGER_DETAIL = "triggerDetail";
	public static final String JOB_DELETED = "jobDeleted";
	public static final String JOB_UNSCHEDULED = "jobUnscheduled";
	public static final String JOB_INTERRUPTED = "jobInterrupted";
	public static final String DELETE_ALL_TIMEOUT = "deleteAllTimeout";
	public static final String TRACK_HISTORY = "trackHistory";
	public static final String JOB_ID = "jobId";
	public static final String CORE_NAME = "coreName";
	public static final String CRAWL_PSEUDO_HANDLER = "--crawl--";

	public static enum JobRepeatUnits {
		MILLISECOND, SECOND, MINUTE, HOUR, DAY, WEEK, MONTH;

		public static JobRepeatUnits get(String p) {
			if (p != null) {
				if ((p.equalsIgnoreCase("MILLISECOND")) || (p.equalsIgnoreCase("ms")))
					return MILLISECOND;
				if ((p.equalsIgnoreCase("SECOND")) || (p.equalsIgnoreCase("SEC")))
					return SECOND;
				if ((p.equalsIgnoreCase("MINUTE")) || (p.equalsIgnoreCase("MIN")))
					return MINUTE;
				if ((p.equalsIgnoreCase("HOUR")) || (p.equalsIgnoreCase("HR")))
					return HOUR;
				if ((p.equalsIgnoreCase("DAY")) || (p.equalsIgnoreCase("DAY"))) {
					return DAY;
				}
				if (p.equalsIgnoreCase("WEEK"))
					return WEEK;
				if (p.equalsIgnoreCase("MONTH")) {
					return MONTH;
				}
			}
			return null;
		}
	}

	public static enum JobType {
		DELETE, DELETE_ALL;
	}
}
