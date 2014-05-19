package gaia.admin.collection;

import gaia.UUIDObject;

public class ScheduledSolrCommand extends UUIDObject {
	protected Schedule schedule = new Schedule();
	protected GaiaSolrParams params;
	protected String name;

	public ScheduledSolrCommand() {
	}

	public ScheduledSolrCommand(String id) {
		this.id = id;
	}

	ScheduledSolrCommand(String id, GaiaSolrParams params, Schedule schedule) {
		this.id = id;
		this.params = params;
		this.schedule = schedule;
	}

	public GaiaSolrParams getParams() {
		if (params == null) {
			return null;
		}
		return params.clone();
	}

	public void setParams(GaiaSolrParams params) {
		this.params = params;
	}

	public Schedule getSchedule() {
		if (schedule == null) {
			return null;
		}
		return schedule;
	}

	public void setSchedule(Schedule schedule) {
		this.schedule = schedule;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("Schedule:" + schedule + '\n');
		sb.append("SolrParams:" + params + '\n');
		return sb.toString();
	}

	public String getJobName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}
}
