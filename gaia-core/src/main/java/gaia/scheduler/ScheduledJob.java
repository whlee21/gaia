package gaia.scheduler;

public class ScheduledJob {
	private String jobKey;
	private String scheduleDate;
	private String triggerName;
	private String triggerKey;

	public ScheduledJob(String jobKey, String scheduleDate, String triggerName, String triggerKey) {
		this.jobKey = jobKey;
		this.scheduleDate = scheduleDate;
		this.triggerName = triggerName;
		this.triggerKey = triggerKey;
	}

	public String getJobKey() {
		return jobKey;
	}

	public String getScheduleDate() {
		return scheduleDate;
	}

	public String getTriggerName() {
		return triggerName;
	}

	public String getTriggerKey() {
		return triggerKey;
	}
}
